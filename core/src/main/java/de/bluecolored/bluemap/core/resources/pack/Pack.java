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
package de.bluecolored.bluemap.core.resources.pack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resources.adapter.ResourcesGson;
import de.bluecolored.bluemap.core.util.FileHelper;
import de.bluecolored.bluemap.core.util.Key;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Getter
public abstract class Pack {

    private final PackVersion packVersion;
    private final @Nullable Set<Key> enabledFeatures;

    public Pack(PackVersion packVersion) {
        this(packVersion, null);
    }

    public abstract void loadResources(Iterable<Path> roots) throws IOException, InterruptedException;

    public void loadResourcePath(Path root, Loader resourceLoader) throws IOException, InterruptedException {
        if (Thread.interrupted()) throw new InterruptedException();
        if (!Files.isDirectory(root)) {
            try (FileSystem fileSystem = FileSystems.newFileSystem(root, (ClassLoader) null)) {
                for (Path fsRoot : fileSystem.getRootDirectories()) {
                    if (!Files.isDirectory(fsRoot)) continue;
                    loadResourcePath(fsRoot, resourceLoader);
                }
            } catch (Exception ex) {
                Logger.global.logDebug("Failed to read '" + root + "': " + ex);
            }
            return;
        }

        // load nested jars from fabric.mod.json if present
        Path fabricModJson = root.resolve("fabric.mod.json");
        if (Files.isRegularFile(fabricModJson)) {
            try (BufferedReader reader = Files.newBufferedReader(fabricModJson)) {
                JsonObject rootElement = ResourcesGson.INSTANCE.fromJson(reader, JsonObject.class);
                if (rootElement.has("jars")) {
                    for (JsonElement element : rootElement.getAsJsonArray("jars")) {
                        Path file = root.resolve(element.getAsJsonObject().get("file").getAsString());
                        if (Files.exists(file)) {
                            try {
                                loadResourcePath(file, resourceLoader);
                            } catch (Exception ex) {
                                Logger.global.logDebug("Failed to read '" + root + "': " + ex);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                Logger.global.logDebug("Failed to read fabric.mod.json: " + ex);
            }
        }

        // load pack-meta
        PackMeta packMeta;
        Path packMetaFile = root.resolve("pack.mcmeta");
        if (Files.isRegularFile(packMetaFile)) {
            try (BufferedReader reader = Files.newBufferedReader(packMetaFile)) {
                packMeta = ResourcesGson.INSTANCE.fromJson(reader, PackMeta.class);
            } catch (Exception ex) {
                Logger.global.logDebug("Failed to read pack.mcmeta: " + ex);
                packMeta = new PackMeta();
            }
        } else {
            packMeta = new PackMeta();
        }

        // stop loading pack if feature is not enabled
        if (enabledFeatures != null && !enabledFeatures.containsAll(packMeta.getFeatures().getEnabled())) {
            Logger.global.logDebug("Skipping resources from '%s' because not all required features (%s) are enabled (%s)"
                    .formatted(
                            root,
                            Arrays.toString(packMeta.getFeatures().getEnabled().toArray()),
                            Arrays.toString(enabledFeatures.toArray())
                    ));
            return;
        }

        // load nested datapacks
        list(root.resolve("data"))
                .map(namespaceRoot -> namespaceRoot.resolve("datapacks"))
                .filter(Files::isDirectory)
                .flatMap(Pack::list)
                .forEach(nestedPack -> {
                    try {
                        loadResourcePath(nestedPack, resourceLoader);
                    } catch (Exception ex) {
                        Logger.global.logDebug("Failed to load nested datapack '" + nestedPack + "': " + ex);
                    }
                });

        // load overlays
        PackMeta.Overlay[] overlays = packMeta.getOverlays().getEntries();
        for (int i = overlays.length - 1; i >= 0; i--) {
            PackMeta.Overlay overlay = overlays[i];
            String dir = overlay.getDirectory();
            if (dir != null && overlay.includes(this.packVersion)) {
                Path overlayRoot = root.resolve(dir);
                Logger.global.logInfo("Loading overlay '" + overlayRoot + "'...");
                if (Files.exists(overlayRoot)) {
                    try {
                        loadResourcePath(overlayRoot, resourceLoader);
                    } catch (Exception ex) {
                        Logger.global.logDebug("Failed to load overlay '" + overlayRoot + "': " + ex);
                    }
                }
            }
        }

        resourceLoader.load(root);
    }

    @SneakyThrows(IOException.class)
    public static Stream<Path> list(Path root) {
        if (!Files.isDirectory(root)) return Stream.empty();
        return Files.list(root);
    }

    @SneakyThrows(IOException.class)
    public static Stream<Path> walk(Path root) {
        if (!Files.exists(root)) return Stream.empty();
        if (Files.isRegularFile(root)) return Stream.of(root);
        return FileHelper.walk(root);
    }

    public interface Loader {
        void load(Path root) throws IOException;
    }

}
