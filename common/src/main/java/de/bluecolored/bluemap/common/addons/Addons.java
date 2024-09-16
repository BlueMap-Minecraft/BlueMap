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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.bluecolored.bluemap.common.config.ConfigurationException;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static de.bluecolored.bluemap.common.addons.AddonInfo.ADDON_INFO_FILE;

public final class Addons {

    private static final String PLUGIN_YML = "plugin.yml";
    private static final String MODS_TOML = "META-INF/mods.toml";
    private static final String FABRIC_MOD_JSON = "fabric.mod.json";

    private static final Gson GSON = new GsonBuilder().create();
    private static final Map<String, LoadedAddon> LOADED_ADDONS = new ConcurrentHashMap<>();

    private Addons() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void tryLoadAddons(Path root) {
        tryLoadAddons(root, false);
    }

    public static void tryLoadAddons(Path root, boolean expectOnlyAddons) {
        if (!Files.exists(root)) return;
        try (Stream<Path> files = Files.list(root)) {
                files
                        .filter(Files::isRegularFile)
                        .filter(f -> f.getFileName().toString().endsWith(".jar"))
                        .forEach(expectOnlyAddons ? Addons::tryLoadAddon : Addons::tryLoadJar);
        } catch (IOException e) {
            Logger.global.logError("Failed to load addons from '%s'".formatted(root), e);
        }
    }

    public static void tryLoadAddon(Path addonJarFile) {
        try {
            AddonInfo addonInfo = loadAddonInfo(addonJarFile);
            if (addonInfo == null) throw createRichExceptionForFile(addonJarFile);

            if (LOADED_ADDONS.containsKey(addonInfo.getId())) return;

            loadAddon(addonJarFile, addonInfo);
        } catch (ConfigurationException e) {
            ConfigurationException e2 = new ConfigurationException("BlueMap failed to load the addon '%s'!".formatted(addonJarFile), e);
            Logger.global.logWarning(e2.getFormattedExplanation());
            Logger.global.logError(e2);
        }
    }

    public static void tryLoadJar(Path addonJarFile) {
        try {
            AddonInfo addonInfo = loadAddonInfo(addonJarFile);
            if (addonInfo == null) {
                Logger.global.logDebug("No %s found in '%s', skipping...".formatted(ADDON_INFO_FILE, addonJarFile));
                return;
            }

            if (LOADED_ADDONS.containsKey(addonInfo.getId())) return;

            loadAddon(addonJarFile, addonInfo);
        } catch (ConfigurationException e) {
            ConfigurationException e2 = new ConfigurationException("BlueMap failed to load the addon '%s'!".formatted(addonJarFile), e);
            Logger.global.logWarning(e2.getFormattedExplanation());
            Logger.global.logError(e2);
        }
    }

    public synchronized static void loadAddon(Path jarFile, AddonInfo addonInfo) throws ConfigurationException {
        Logger.global.logInfo("Loading BlueMap Addon: %s (%s)".formatted(addonInfo.getId(), jarFile));

        if (LOADED_ADDONS.containsKey(addonInfo.getId()))
            throw new ConfigurationException("There is already an addon with same id ('%s') loaded!"
                    .formatted(addonInfo.getId()));

        try {
            ClassLoader addonClassLoader = BlueMap.class.getClassLoader();
            Class<?> entrypointClass;

            // try to find entrypoint class and load jar with new classloader if needed
            try {
                entrypointClass = addonClassLoader.loadClass(addonInfo.getEntrypoint());
            } catch (ClassNotFoundException e) {
                addonClassLoader = new URLClassLoader(
                        new URL[]{ jarFile.toUri().toURL() },
                        BlueMap.class.getClassLoader()
                );
                entrypointClass = addonClassLoader.loadClass(addonInfo.getEntrypoint());
            }

            // create addon instance
            Object instance = entrypointClass.getConstructor().newInstance();
            LoadedAddon addon = new LoadedAddon(
                    addonInfo,
                    addonClassLoader,
                    instance
            );
            LOADED_ADDONS.put(addonInfo.getId(), addon);

            // run addon
            if (instance instanceof Runnable runnable)
                runnable.run();

        } catch (Exception e) {
            throw new ConfigurationException("There was an exception trying to initialize the addon!", e);
        }
    }

    public static @Nullable AddonInfo loadAddonInfo(Path addonJarFile) throws ConfigurationException {
        try (FileSystem fileSystem = FileSystems.newFileSystem(addonJarFile, (ClassLoader) null)) {
            for (Path root : fileSystem.getRootDirectories()) {
                Path addonInfoFile = root.resolve(ADDON_INFO_FILE);
                if (!Files.exists(addonInfoFile)) continue;

                try (Reader reader = Files.newBufferedReader(addonInfoFile, StandardCharsets.UTF_8)) {
                    AddonInfo addonInfo = GSON.fromJson(reader, AddonInfo.class);

                    if (addonInfo.getId() == null)
                        throw new ConfigurationException("'id' is missing");

                    if (addonInfo.getEntrypoint() == null)
                        throw new ConfigurationException("'entrypoint' is missing");

                    return addonInfo;
                }
            }
        } catch (IOException e) {
            throw new ConfigurationException("There was an exception trying to access the file.", e);
        }

        return null;
    }

    private static ConfigurationException createRichExceptionForFile(Path jarFile) {
        boolean isPlugin = false;
        boolean isMod = false;

        try (FileSystem fileSystem = FileSystems.newFileSystem(jarFile, (ClassLoader) null)) {
            for (Path root : fileSystem.getRootDirectories()) {
                if (Files.exists(root.resolve(PLUGIN_YML))) isPlugin = true;
                if (Files.exists(root.resolve(MODS_TOML))) isMod = true;
                if (Files.exists(root.resolve(FABRIC_MOD_JSON))) isMod = true;
            }
        } catch (IOException e) {
            Logger.global.logError("Failed to log file-info for '%s'".formatted(jarFile), e);
        }

        if (!(isPlugin || isMod)) return new ConfigurationException("""
        File '%s' does not seem to be a valid native bluemap addon.
        """.strip().formatted(jarFile));

        String type = isPlugin ? "plugin" : "mod";
        String targetFolder = isPlugin ? "./plugins" : "./mods";

        return new ConfigurationException("""
        File '%s' seems to be a %s and not a native bluemap addon.
        Try adding it to the '%s' folder of your server instead!
        """.strip().formatted(jarFile, type, targetFolder));
    }

}
