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
package de.bluecolored.bluemap.core.mca.region;

import com.flowpowered.math.vector.Vector2i;
import com.github.luben.zstd.ZstdInputStream;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.mca.MCAChunk;
import de.bluecolored.bluemap.core.mca.MCAWorld;
import de.bluecolored.bluemap.core.world.Chunk;
import de.bluecolored.bluemap.core.world.EmptyChunk;
import de.bluecolored.bluemap.core.world.Region;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.Tag;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class LinearRegion implements Region {

    public static final String FILE_SUFFIX = ".linear";

    private static final List<Byte> SUPPORTED_VERSIONS = Arrays.asList((byte) 1, (byte) 2);
    private static final long SUPERBLOCK = -4323716122432332390L;
    private static final int HEADER_SIZE = 32;
    private static final int FOOTER_SIZE = 8;

    private final MCAWorld world;
    private final Path regionFile;
    private final Vector2i regionPos;


    public LinearRegion(MCAWorld world, Path regionFile) throws IllegalArgumentException {
        this.world = world;
        this.regionFile = regionFile;

        String[] filenameParts = regionFile.getFileName().toString().split("\\.");
        int rX = Integer.parseInt(filenameParts[1]);
        int rZ = Integer.parseInt(filenameParts[2]);

        this.regionPos = new Vector2i(rX, rZ);
    }

    @Override
    public Chunk loadChunk(int chunkX, int chunkZ, boolean ignoreMissingLightData) throws IOException {
        if (Files.notExists(regionFile)) return EmptyChunk.INSTANCE;

        long fileLength = Files.size(regionFile);
        if (fileLength == 0) return EmptyChunk.INSTANCE;

        try (InputStream inputStream = Files.newInputStream(regionFile);
             DataInputStream rawDataStream = new DataInputStream(inputStream)) {

            long superBlock = rawDataStream.readLong();
            if (superBlock != SUPERBLOCK)
                throw new RuntimeException("Invalid superblock: " + superBlock + " file " + regionFile);

            byte version = rawDataStream.readByte();
            if (!SUPPORTED_VERSIONS.contains(version))
                throw new RuntimeException("Invalid version: " + version + " file " + regionFile);

            // Skip newestTimestamp (Long) + Compression level (Byte) + Chunk count (Short): Unused.
            rawDataStream.skipBytes(11);

            int dataCount = rawDataStream.readInt();
            if (fileLength != HEADER_SIZE + dataCount + FOOTER_SIZE)
                throw new RuntimeException("Invalid file length: " + this.regionFile + " " + fileLength + " " + (HEADER_SIZE + dataCount + FOOTER_SIZE));

            // Skip data hash (Long): Unused.
            rawDataStream.skipBytes(8);

            byte[] rawCompressed = new byte[dataCount];
            rawDataStream.readFully(rawCompressed, 0, dataCount);

            superBlock = rawDataStream.readLong();
            if (superBlock != SUPERBLOCK)
                throw new RuntimeException("Invalid footer superblock: " + this.regionFile);

            try (DataInputStream dis = new DataInputStream(new ZstdInputStream(new ByteArrayInputStream(rawCompressed)))) {
                int x = chunkX - (regionPos.getX() << 5);
                int z = chunkZ - (regionPos.getY() << 5);
                int pos = (z << 5) + x;
                int skip = 0;

                for (int i = 0; i < pos; i++) {
                    skip += dis.readInt(); // Size of the chunk (bytes) to skip
                    dis.skipBytes(4); // Skip timestamps
                }

                int size = dis.readInt();
                if (size <= 0) return EmptyChunk.INSTANCE;

                dis.skipBytes(((1024 - pos - 1) << 3) + 4); // Skip current chunk 0 and unneeded other chunks zero/size
                dis.skipBytes(skip); // Skip unneeded chunks data

                Tag<?> tag = Tag.deserialize(dis, Tag.DEFAULT_MAX_DEPTH);
                if (tag instanceof CompoundTag) {
                    MCAChunk chunk = MCAChunk.create(world, (CompoundTag) tag);
                    if (!chunk.isGenerated()) return EmptyChunk.INSTANCE;
                    return chunk;
                } else {
                    throw new IOException("Invalid data tag: " + (tag == null ? "null" : tag.getClass().getName()));
                }
            }
        } catch (RuntimeException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Collection<Vector2i> listChunks(long modifiedSince) {
        if (Files.notExists(regionFile)) return Collections.emptyList();

        long fileLength;
        try {
            fileLength = Files.size(regionFile);
            if (fileLength == 0) return Collections.emptyList();
        } catch (IOException ex) {
            Logger.global.logWarning("Failed to read file-size for file: " + regionFile);
            return Collections.emptyList();
        }

        List<Vector2i> chunks = new ArrayList<>(1024); //1024 = 32 x 32 chunks per region-file
        try (InputStream inputStream = Files.newInputStream(regionFile);
             DataInputStream rawDataStream = new DataInputStream(inputStream)) {

            long superBlock = rawDataStream.readLong();
            if (superBlock != SUPERBLOCK)
                throw new RuntimeException("Invalid superblock: " + superBlock + " file " + regionFile);

            byte version = rawDataStream.readByte();
            if (!SUPPORTED_VERSIONS.contains(version))
                throw new RuntimeException("Invalid version: " + version + " file " + regionFile);

            int date = (int) (modifiedSince / 1000);

            // If whole region is the same - skip.
            long newestTimestamp = rawDataStream.readLong();
            if (newestTimestamp < date) return Collections.emptyList();

            // Linear v1 files store whole region timestamp, not chunk timestamp. We need to render the whole region file.
            if (version == 1) {
                for(int i = 0 ; i < 1024; i++)
                    chunks.add(new Vector2i((regionPos.getX() << 5) + (i & 31), (regionPos.getY() << 5) + (i >> 5)));
                return chunks;
            }
            // Linear v2: Chunk timestamps are here!
            // Skip Compression level (Byte) + Chunk count (Short): Unused.
            rawDataStream.skipBytes(3);

            int dataCount = rawDataStream.readInt();
            if (fileLength != HEADER_SIZE + dataCount + FOOTER_SIZE)
                throw new RuntimeException("Invalid file length: " + this.regionFile + " " + fileLength + " " + (HEADER_SIZE + dataCount + FOOTER_SIZE));

            // Skip data hash (Long): Unused.
            rawDataStream.skipBytes(8);

            byte[] rawCompressed = new byte[dataCount];
            rawDataStream.readFully(rawCompressed, 0, dataCount);

            superBlock = rawDataStream.readLong();
            if (superBlock != SUPERBLOCK)
                throw new RuntimeException("Invalid footer SuperBlock: " + this.regionFile);

            try (DataInputStream dis = new DataInputStream(new ZstdInputStream(new ByteArrayInputStream(rawCompressed)))) {
                for (int i = 0; i < 1024; i++) {
                    dis.skipBytes(4); // Skip size of the chunk
                    int timestamp = dis.readInt();
                    if (timestamp >= date) // Timestamps
                        chunks.add(new Vector2i((regionPos.getX() << 5) + (i & 31), (regionPos.getY() << 5) + (i >> 5)));
                }
            }
        } catch (RuntimeException | IOException ex) {
            Logger.global.logWarning("Failed to read .linear file: " + regionFile + " (" + ex + ")");
        }
        return chunks;
    }

    @Override
    public Path getRegionFile() {
        return regionFile;
    }

    public static String getRegionFileName(int regionX, int regionZ) {
        return "r." + regionX + "." + regionZ + FILE_SUFFIX;
    }

}
