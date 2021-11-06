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
package de.bluecolored.bluemap.core.storage;

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.core.debug.DebugDump;
import de.bluecolored.bluemap.core.util.AtomicFileHelper;
import de.bluecolored.bluemap.core.util.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

@DebugDump
public class FileStorage extends Storage {

    private static final EnumMap<MetaType, String> metaTypeFileNames = new EnumMap<>(MetaType.class);
    static {
        metaTypeFileNames.put(MetaType.TEXTURES, "../textures.json");
        metaTypeFileNames.put(MetaType.SETTINGS, "../settings.json");
        metaTypeFileNames.put(MetaType.MARKERS, "../markers.json");
        metaTypeFileNames.put(MetaType.RENDER_STATE, ".rstate");
    }

    private final Path root;
    private final Compression compression;

    public FileStorage(Path root, Compression compression) {
        this.root = root;
        this.compression = compression;
    }

    @Override
    public OutputStream writeMapTile(String mapId, TileType tileType, Vector2i tile) throws IOException {
        Path file = getFilePath(mapId, tileType, tile);

        OutputStream os = AtomicFileHelper.createFilepartOutputStream(file);
        os = new BufferedOutputStream(os);
        os = compression.compress(os);

        return os;
    }

    @Override
    public Optional<InputStream> readMapTile(String mapId, TileType tileType, Vector2i tile) throws IOException {
        Path file = getFilePath(mapId, tileType, tile);

        if (!Files.exists(file)) return Optional.empty();

        InputStream is = Files.newInputStream(file, StandardOpenOption.READ);
        is = new BufferedInputStream(is);
        is = compression.decompress(is);

        return Optional.of(is);
    }

    @Override
    public void deleteMapTile(String mapId, TileType tileType, Vector2i tile) throws IOException {
        Path file = getFilePath(mapId, tileType, tile);
        FileUtils.delete(file.toFile());
    }

    @Override
    public OutputStream writeMeta(String mapId, MetaType metaType) throws IOException {
        Path file = getFilePath(mapId).resolve(getFilename(metaType));

        OutputStream os = AtomicFileHelper.createFilepartOutputStream(file);
        os = new BufferedOutputStream(os);

        return os;
    }

    @Override
    public Optional<InputStream> readMeta(String mapId, MetaType metaType) throws IOException {
        Path file = getFilePath(mapId).resolve(getFilename(metaType));

        if (!Files.exists(file)) return Optional.empty();

        InputStream is = Files.newInputStream(file, StandardOpenOption.READ);
        is = new BufferedInputStream(is);

        return Optional.of(is);
    }

    @Override
    public void purgeMap(String mapId) throws IOException {
        FileUtils.delete(getFilePath(mapId).toFile());
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

    private static String getFilename(MetaType metaType) {
        return metaTypeFileNames.getOrDefault(metaType, metaType.name().toLowerCase(Locale.ROOT));
    }

}
