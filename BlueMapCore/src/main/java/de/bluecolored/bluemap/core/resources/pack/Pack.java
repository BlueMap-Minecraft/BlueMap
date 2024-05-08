package de.bluecolored.bluemap.core.resources.pack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.adapter.ResourcesGson;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Getter
public abstract class Pack {

    private final int packVersion;

    public abstract void loadResources(Iterable<Path> roots) throws IOException, InterruptedException;

    protected void loadResourcePath(Path root, ResourcePack.PathLoader resourceLoader) throws IOException, InterruptedException {
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
                        if (Files.exists(file)) loadResourcePath(file, resourceLoader);
                    }
                }
            } catch (Exception ex) {
                Logger.global.logDebug("Failed to read fabric.mod.json: " + ex);
            }
        }

        // load overlays
        Path packMetaFile = root.resolve("pack.mcmeta");
        if (Files.isRegularFile(packMetaFile)) {
            try (BufferedReader reader = Files.newBufferedReader(packMetaFile)) {
                PackMeta packMeta = ResourcesGson.INSTANCE.fromJson(reader, PackMeta.class);
                PackMeta.Overlay[] overlays = packMeta.getOverlays().getEntries();
                for (int i = overlays.length - 1; i >= 0; i--) {
                    PackMeta.Overlay overlay = overlays[i];
                    String dir = overlay.getDirectory();
                    if (dir != null && overlay.getFormats().includes(this.packVersion)) {
                        Path overlayRoot = root.resolve(dir);
                        if (Files.exists(overlayRoot)) loadResourcePath(overlayRoot, resourceLoader);
                    }
                }
            } catch (Exception ex) {
                Logger.global.logDebug("Failed to read pack.mcmeta: " + ex);
            }
        }

        resourceLoader.load(root);
    }

    protected <T> void loadResource(Path root, Path file, int namespacePos, int valuePos, Loader<T> loader, Map<? super ResourcePath<T>, T> resultMap) {
        try {
            ResourcePath<T> resourcePath = new ResourcePath<>(root.relativize(file), namespacePos, valuePos);
            if (resultMap.containsKey(resourcePath)) return; // don't load already present resources

            T resource = loader.load(resourcePath);
            if (resource == null) return; // don't load missing resources

            resourcePath.setResource(resource);
            resultMap.put(resourcePath, resource);
        } catch (Exception ex) {
            Logger.global.logDebug("Failed to parse resource-file '" + file + "': " + ex);
        }
    }

    protected static Stream<Path> list(Path root) {
        if (!Files.isDirectory(root)) return Stream.empty();
        try {
            return Files.list(root);
        } catch (IOException ex) {
            throw new CompletionException(ex);
        }
    }

    protected static Stream<Path> walk(Path root) {
        if (!Files.exists(root)) return Stream.empty();
        if (Files.isRegularFile(root)) return Stream.of(root);
        try {
            return Files.walk(root);
        } catch (IOException ex) {
            throw new CompletionException(ex);
        }
    }

    protected interface Loader<T> {
        T load(ResourcePath<T> resourcePath) throws IOException;
    }

    protected interface PathLoader {
        void load(Path root) throws IOException;
    }

}
