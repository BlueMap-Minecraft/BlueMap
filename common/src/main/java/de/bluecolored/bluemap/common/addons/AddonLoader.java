/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.common.addons;

import de.bluecolored.bluemap.common.config.ConfigurationException;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class AddonLoader {
    public static final AddonLoader INSTANCE = new AddonLoader();

    private final Map<String, LoadedAddon> loadedAddons = new ConcurrentHashMap<>();

    public void tryLoadAddons(Path root) {
        if (!Files.exists(root))
            return;
        try (Stream<Path> files = Files.list(root)) {

            // find all addons and load addon-info
            Map<String, Addon> availableAddons = files
                    .filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().endsWith(".jar"))
                    .map(this::tryLoadAddonInfo)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(addon -> addon.getAddonInfo().getId(), addon -> addon));

            // remove addons that have missing required dependencies
            while (!availableAddons.isEmpty()) {
                Addon addonToRemove = availableAddons.values().stream()
                        .filter(a -> !availableAddons.keySet().containsAll(a.getAddonInfo().getDependencies()))
                        .findAny()
                        .orElse(null);
                if (addonToRemove == null)
                    break;
                String id = addonToRemove.getAddonInfo().getId();
                availableAddons.remove(id);
                new ConfigurationException("Missing required dependencies %s to load addon '%s' (%s)".formatted(
                        Arrays.toString(addonToRemove.getAddonInfo().getDependencies().toArray(String[]::new)),
                        id,
                        addonToRemove.getJarFile())).printLog(Logger.global);
            }

            // topography sort and load addons based on their dependencies
            Map<String, Long> dependenciesToLoad = new HashMap<>();
            Queue<String> loadNext = new ArrayDeque<>();
            for (Addon addon : availableAddons.values()) {
                long dependencyCount = addon.getAddonInfo().getDependencies().size() +
                        addon.getAddonInfo().getSoftDependencies().stream()
                                .filter(availableAddons::containsKey)
                                .count();
                String id = addon.getAddonInfo().getId();
                if (dependencyCount == 0)
                    loadNext.add(id);
                else
                    dependenciesToLoad.put(id, dependencyCount);
            }

            while (!loadNext.isEmpty()) {
                String id = loadNext.poll();
                Addon addon = availableAddons.get(id);

                try {
                    loadAddon(addon);
                    for (Addon dependant : availableAddons.values()) {
                        AddonInfo info = dependant.getAddonInfo();
                        if (info.getDependencies().contains(id) || info.getSoftDependencies().contains(id)) {
                            Long count = dependenciesToLoad.get(info.getId());
                            if (count == null)
                                continue;
                            if (--count <= 0) {
                                dependenciesToLoad.remove(info.getId());
                                loadNext.add(info.getId());
                            } else {
                                dependenciesToLoad.put(info.getId(), count);
                            }
                        }
                    }
                } catch (ConfigurationException ex) {
                    new ConfigurationException("Failed to load addon '%s' (%s)".formatted(id, addon.getJarFile()), ex)
                            .printLog(Logger.global);
                }
            }

            // failed to resolve dependencies, possibly a cyclic reference
            // try to load anyway in case a soft dependency is involved
            for (String id : dependenciesToLoad.keySet()) {
                Addon addon = availableAddons.remove(id);
                try {
                    if (addon != null)
                        loadAddon(addon);
                } catch (ConfigurationException ex) {
                    new ConfigurationException("Failed to load addon '%s' (%s)".formatted(id, addon.getJarFile()), ex)
                            .printLog(Logger.global);
                }
            }

        } catch (IOException e) {
            Logger.global.logError("Failed to load addons from '%s'".formatted(root), e);
        }
    }

    private @Nullable Addon tryLoadAddonInfo(Path jarFile) {
        try {
            AddonInfo addonInfo = AddonInfo.load(jarFile);
            if (addonInfo == null)
                return null;
            return new Addon(addonInfo, jarFile);
        } catch (ConfigurationException e) {
            new ConfigurationException("Failed to load addon info from '%s'.".formatted(jarFile), e)
                    .printLog(Logger.global);
            return null;
        }
    }

    private synchronized void loadAddon(Addon addon) throws ConfigurationException {
        AddonInfo addonInfo = addon.getAddonInfo();
        Path jarFile = addon.getJarFile();

        if (loadedAddons.containsKey(addonInfo.getId()))
            return;
        Logger.global.logInfo("Loading BlueMap Addon: %s (%s)".formatted(addonInfo.getId(), jarFile));

        try {
            Set<ClassLoader> dependencyClassLoaders = new LinkedHashSet<>();
            for (String dependencyId : addon.getAddonInfo().getDependencies()) {
                LoadedAddon loadedAddon = loadedAddons.get(dependencyId);
                if (loadedAddon == null)
                    throw new IllegalStateException("Required dependency '%s' is not loaded."
                            .formatted(addon.getAddonInfo().getId()));
                dependencyClassLoaders.add(loadedAddon.getClassLoader());
            }
            for (String dependencyId : addon.getAddonInfo().getSoftDependencies()) {
                LoadedAddon loadedAddon = loadedAddons.get(dependencyId);
                if (loadedAddon == null)
                    continue;
                dependencyClassLoaders.add(loadedAddon.getClassLoader());
            }

            ClassLoader parent = BlueMap.class.getClassLoader();
            if (!dependencyClassLoaders.isEmpty())
                parent = new CombinedClassLoader(parent, dependencyClassLoaders.toArray(ClassLoader[]::new));

            ClassLoader addonClassLoader = new URLClassLoader(
                    new URL[] { jarFile.toUri().toURL() },
                    parent);
            Class<?> entrypointClass = addonClassLoader.loadClass(addonInfo.getEntrypoint());

            // create addon instance
            Object instance = entrypointClass.getConstructor().newInstance();
            LoadedAddon loadedAddon = new LoadedAddon(
                    addonInfo,
                    jarFile,
                    addonClassLoader,
                    instance);

            loadedAddons.put(addonInfo.getId(), loadedAddon);

            // run addon
            if (instance instanceof Runnable runnable)
                runnable.run();

        } catch (Exception e) {
            throw new ConfigurationException("There was an exception trying to initialize the addon!", e);
        }
    }

}
