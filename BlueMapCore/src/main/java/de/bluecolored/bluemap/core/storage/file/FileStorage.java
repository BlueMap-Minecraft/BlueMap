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
package de.bluecolored.bluemap.core.storage.file;

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.core.storage.*;
import de.bluecolored.bluemap.core.util.DeletingPathVisitor;
import de.bluecolored.bluemap.core.util.FileHelper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@DebugDump
public class FileStorage extends Storage {

    private final Path root;
    private final Compression hiresCompression;

    public FileStorage(FileStorageSettings config) {
        this.root = config.getRoot();
        this.hiresCompression = config.getCompression();
    }

    public FileStorage(Path root, Compression compression) {
        this.root = root;
        this.hiresCompression = compression;
    }

    @Override
    public void initialize() {}

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public void close() throws IOException {}

    @Override
    public OutputStream writeMapTile(String mapId, int lod, Vector2i tile) throws IOException {
        Compression compression = lod == 0 ? this.hiresCompression : Compression.NONE;
        Path file = getFilePath(mapId, lod, tile);

        OutputStream os = FileHelper.createFilepartOutputStream(file);
        os = new BufferedOutputStream(os);

        try {
            os = compression.compress(os);
        } catch (IOException ex) {
            os.close();
            throw ex;
        }

        return os;
    }

    @Override
    public Optional<CompressedInputStream> readMapTile(String mapId, int lod, Vector2i tile) throws IOException {
        Compression compression = lod == 0 ? this.hiresCompression : Compression.NONE;
        Path file = getFilePath(mapId, lod, tile);

        if (!Files.exists(file)) return Optional.empty();

        InputStream is = Files.newInputStream(file, StandardOpenOption.READ);
        is = new BufferedInputStream(is);

        return Optional.of(new CompressedInputStream(is, compression));
    }

    @Override
    public Optional<TileInfo> readMapTileInfo(String mapId, int lod, Vector2i tile) throws IOException {
        Compression compression = lod == 0 ? this.hiresCompression : Compression.NONE;
        Path file = getFilePath(mapId, lod, tile);

        if (!Files.exists(file)) return Optional.empty();

        final long size = Files.size(file);
        final long lastModified = Files.getLastModifiedTime(file).toMillis();

        return Optional.of(new TileInfo() {
            @Override
            public CompressedInputStream readMapTile() throws IOException {
                return FileStorage.this.readMapTile(mapId, lod, tile)
                        .orElseThrow(() -> new IOException("Tile no longer present!"));
            }

            @Override
            public Compression getCompression() {
                return compression;
            }

            @Override
            public long getSize() {
                return size;
            }

            @Override
            public long getLastModified() {
                return lastModified;
            }
        });
    }

    @Override
    public void deleteMapTile(String mapId, int lod, Vector2i tile) throws IOException {
        Path file = getFilePath(mapId, lod, tile);
        Files.deleteIfExists(file);
    }

    @Override
    public OutputStream writeMeta(String mapId, String name) throws IOException {
        Path file = getMetaFilePath(mapId, name);

        OutputStream os = FileHelper.createFilepartOutputStream(file);
        os = new BufferedOutputStream(os);

        return os;
    }

    @Override
    public Optional<InputStream> readMeta(String mapId, String name) throws IOException {
        Path file = getMetaFilePath(mapId, name);

        if (!Files.exists(file)) return Optional.empty();

        InputStream is = Files.newInputStream(file, StandardOpenOption.READ);
        is = new BufferedInputStream(is);

        return Optional.of(is);
    }

    @Override
    public Optional<MetaInfo> readMetaInfo(String mapId, String name) throws IOException {
        Path file = getMetaFilePath(mapId, name);

        if (!Files.exists(file)) return Optional.empty();

        final long size = Files.size(file);

        return Optional.of(new MetaInfo() {
            @Override
            public InputStream readMeta() throws IOException {
                return FileStorage.this.readMeta(mapId, name)
                        .orElseThrow(() -> new IOException("Meta no longer present!"));
            }

            @Override
            public long getSize() {
                return size;
            }

        });
    }

    @Override
    public void deleteMeta(String mapId, String name) throws IOException {
        Path file = getMetaFilePath(mapId, name);
        Files.deleteIfExists(file);
    }

    @Override
    public void purgeMap(String mapId, Function<ProgressInfo, Boolean> onProgress) throws IOException {
        final Path directory = getFilePath(mapId);
        if (!Files.exists(directory)) return;

        final int subFilesCount;
        final LinkedList<Path> subFiles;

        // collect sub-files to be able to provide progress-updates
        try (Stream<Path> pathStream = Files.walk(directory, 3)) {
            subFiles = pathStream.collect(Collectors.toCollection(LinkedList::new));
        }
        subFilesCount = subFiles.size();

        // delete subFiles first to be able to track the progress and cancel
        while (!subFiles.isEmpty()) {
            Path subFile = subFiles.getLast();
            Files.walkFileTree(subFile, DeletingPathVisitor.INSTANCE);
            subFiles.removeLast();

            if (!onProgress.apply(
                    new ProgressInfo(1d - (subFiles.size() / (double) subFilesCount))
            )) return;
        }

        // make sure everything is deleted
        if (Files.exists(directory))
            Files.walkFileTree(directory, DeletingPathVisitor.INSTANCE);
    }

    @Override
    public Collection<String> collectMapIds() throws IOException {
        try (Stream<Path> fileStream = Files.list(root)) {
                return fileStream
                        .filter(Files::isDirectory)
                        .map(path -> path.getFileName().toString())
                        .collect(Collectors.toList());
        }
    }

    public Path getFilePath(String mapId, int lod, Vector2i tile){
        String path = "x" + tile.getX() + "z" + tile.getY();
        char[] cs = path.toCharArray();
        List<String> folders = new ArrayList<>();
        StringBuilder folder = new StringBuilder();
        for (char c : cs){
            folder.append(c);
            if (c >= '0' && c <= '9'){
                folders.add(folder.toString());
                folder.delete(0, folder.length());
            }
        }
        String fileName = folders.remove(folders.size() - 1);

        Path p = getFilePath(mapId).resolve("tiles").resolve(Integer.toString(lod));
        for (String s : folders){
            p = p.resolve(s);
        }

        if (lod == 0) {
            return p.resolve(fileName + ".json" + hiresCompression.getFileSuffix());
        } else {
            return p.resolve(fileName + ".png");
        }
    }

    public Path getFilePath(String mapId) {
        return root.resolve(mapId);
    }

    public Path getMetaFilePath(String mapId, String name) {
        return getFilePath(mapId).resolve(escapeMetaName(name)
                .replace("/", root.getFileSystem().getSeparator()));
    }

}
