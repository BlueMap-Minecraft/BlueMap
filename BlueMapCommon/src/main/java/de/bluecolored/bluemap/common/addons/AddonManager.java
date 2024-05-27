package de.bluecolored.bluemap.common.addons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

public final class AddonManager {

    private static final Gson GSON = new GsonBuilder().create();
    private static final Map<String, LoadedAddon> LOADED_ADDONS = new ConcurrentHashMap<>();

    private AddonManager() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void tryLoadAddons(Path root) {
        tryLoadAddons(root, false);
    }

    public static void tryLoadAddons(Path root, boolean expectOnlyAddons) {
        try (Stream<Path> files = Files.list(root)) {
                files
                        .filter(Files::isRegularFile)
                        .filter(f -> f.getFileName().toString().endsWith(".jar"))
                        .forEach(expectOnlyAddons ? AddonManager::tryLoadAddon : AddonManager::tryLoadJar);
        } catch (IOException e) {
            Logger.global.logError("Failed to load addons from '%s'".formatted(root), e);
        }
    }

    public static void tryLoadAddon(Path addonJarFile) {
        try {
            AddonInfo addonInfo = loadAddonInfo(addonJarFile);
            if (addonInfo == null) throw new AddonException("No %s found in '%s'".formatted(ADDON_INFO_FILE, addonJarFile));

            if (LOADED_ADDONS.containsKey(addonInfo.getId())) return;

            loadAddon(addonJarFile, addonInfo);
        } catch (IOException | AddonException e) {
            Logger.global.logError("Failed to load addon '%s'".formatted(addonJarFile), e);
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
        } catch (IOException | AddonException e) {
            Logger.global.logError("Failed to load addon '%s'".formatted(addonJarFile), e);
        }
    }

    public synchronized static void loadAddon(Path jarFile, AddonInfo addonInfo) throws AddonException {
        Logger.global.logInfo("Loading BlueMap Addon: %s (%s)".formatted(addonInfo.getId(), jarFile));

        if (LOADED_ADDONS.containsKey(addonInfo.getId()))
            throw new AddonException("Addon with id '%s' is already loaded".formatted(addonInfo.getId()));

        try {
            ClassLoader addonClassLoader = BlueMap.class.getClassLoader();
            Class<?> entrypointClass;

            // try to find entrypoint class and load jar with new classloader if needed
            try {
                entrypointClass = BlueMap.class.getClassLoader().loadClass(addonInfo.getEntrypoint());
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
            throw new AddonException("Failed to load addon '%s'".formatted(jarFile), e);
        }
    }

    public static @Nullable AddonInfo loadAddonInfo(Path addonJarFile) throws IOException, AddonException {
        try (FileSystem fileSystem = FileSystems.newFileSystem(addonJarFile, (ClassLoader) null)) {
            for (Path root : fileSystem.getRootDirectories()) {
                Path addonInfoFile = root.resolve(ADDON_INFO_FILE);
                if (!Files.exists(addonInfoFile)) continue;

                try (Reader reader = Files.newBufferedReader(addonInfoFile, StandardCharsets.UTF_8)) {
                    AddonInfo addonInfo = GSON.fromJson(reader, AddonInfo.class);

                    if (addonInfo.getId() == null)
                        throw new AddonException("'id' is missing");

                    if (addonInfo.getEntrypoint() == null)
                        throw new AddonException("'entrypoint' is missing");

                    return addonInfo;
                }
            }
        }

        return null;
    }

}
