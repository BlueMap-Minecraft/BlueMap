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
import de.bluecolored.bluemap.core.util.AtomicFileHelper;
import de.bluecolored.bluemap.core.util.DeletingPathVisitor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@DebugDump
public class FileStorage extends Storage {

    private final Path root;
    private final Compression compression;

    public FileStorage(FileStorageSettings config) {
        this.root = config.getRoot();
        this.compression = config.getCompression();
    }

    public FileStorage(Path root, Compression compression) {
        this.root = root;
        this.compression = compression;
    }

    @Override
    public void initialize() {}

    @Override
    public void close() throws IOException {}

    @Override
    public OutputStream writeMapTile(String mapId, TileType tileType, Vector2i tile) throws IOException {
        Path file = getFilePath(mapId, tileType, tile);

        OutputStream os = AtomicFileHelper.createFilepartOutputStream(file);
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
    public Optional<CompressedInputStream> readMapTile(String mapId, TileType tileType, Vector2i tile) throws IOException {
        Path file = getFilePath(mapId, tileType, tile);

        if (!Files.exists(file)) return Optional.empty();

        InputStream is = Files.newInputStream(file, StandardOpenOption.READ);
        is = new BufferedInputStream(is);

        return Optional.of(new CompressedInputStream(is, compression));
    }

    @Override
    public Optional<TileData> readMapTileData(String mapId, TileType tileType, Vector2i tile) throws IOException {
        Path file = getFilePath(mapId, tileType, tile);

        if (!Files.exists(file)) return Optional.empty();

        final long size = Files.size(file);
        final long lastModified = Files.getLastModifiedTime(file).toMillis();

        return Optional.of(new TileData() {
            @Override
            public CompressedInputStream readMapTile() throws IOException {
                return FileStorage.this.readMapTile(mapId, tileType, tile)
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
    public void deleteMapTile(String mapId, TileType tileType, Vector2i tile) throws IOException {
        Path file = getFilePath(mapId, tileType, tile);
        Files.deleteIfExists(file);
    }

    @Override
    public OutputStream writeMeta(String mapId, MetaType metaType) throws IOException {
        Path file = getFilePath(mapId).resolve(metaType.getFilePath());

        OutputStream os = AtomicFileHelper.createFilepartOutputStream(file);
        os = new BufferedOutputStream(os);

        return os;
    }

    @Override
    public Optional<CompressedInputStream> readMeta(String mapId, MetaType metaType) throws IOException {
        Path file = getFilePath(mapId).resolve(metaType.getFilePath());

        if (!Files.exists(file)) return Optional.empty();

        InputStream is = Files.newInputStream(file, StandardOpenOption.READ);
        is = new BufferedInputStream(is);

        return Optional.of(new CompressedInputStream(is, Compression.NONE));
    }

    @Override
    public void deleteMeta(String mapId, MetaType metaType) throws IOException {
        Path file = getFilePath(mapId).resolve(metaType.getFilePath());
        Files.deleteIfExists(file);
    }

    @Override
    public void purgeMap(String mapId) throws IOException {
        Files.walkFileTree(getFilePath(mapId), DeletingPathVisitor.INSTANCE);
    }

    public Path getFilePath(String mapId, TileType tileType, Vector2i tile){
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

        Path p = getFilePath(mapId).resolve(tileType.getTypeId());
        for (String s : folders){
            p = p.resolve(s);
        }

        return p.resolve(fileName + ".json" + compression.getFileSuffix());
    }

    public Path getFilePath(String mapId) {
        return root.resolve(mapId);
    }

}
