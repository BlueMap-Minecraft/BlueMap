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
package de.bluecolored.bluemap.core.world.mca.region;

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.core.storage.compression.Compression;
import de.bluecolored.bluemap.core.world.ChunkConsumer;
import de.bluecolored.bluemap.core.world.Region;
import de.bluecolored.bluemap.core.world.mca.ChunkLoader;
import lombok.Getter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.regex.Pattern;

@Getter
public class MCARegion<T> implements Region<T> {

    public static final String FILE_SUFFIX = ".mca";
    public static final Pattern FILE_PATTERN = Pattern.compile("^r\\.(-?\\d+)\\.(-?\\d+)\\.mca$");

    public static final Compression[] CHUNK_COMPRESSION_MAP = new Compression[255];
    static {
        CHUNK_COMPRESSION_MAP[0] = Compression.NONE;
        CHUNK_COMPRESSION_MAP[1] = Compression.GZIP;
        CHUNK_COMPRESSION_MAP[2] = Compression.DEFLATE;
        CHUNK_COMPRESSION_MAP[3] = Compression.NONE;
        CHUNK_COMPRESSION_MAP[4] = Compression.LZ4;
    }

    private final Path regionFile;
    private final ChunkLoader<T> chunkLoader;
    private final Vector2i regionPos;

    public MCARegion(ChunkLoader<T> chunkLoader, Path regionFile) throws IllegalArgumentException {
        this.chunkLoader = chunkLoader;
        this.regionFile = regionFile;

        String[] filenameParts = regionFile.getFileName().toString().split("\\.");
        int rX = Integer.parseInt(filenameParts[1]);
        int rZ = Integer.parseInt(filenameParts[2]);

        this.regionPos = new Vector2i(rX, rZ);
    }

    @Override
    public T loadChunk(int chunkX, int chunkZ) throws IOException {
        if (Files.notExists(regionFile)) return chunkLoader.emptyChunk();

        long fileLength = Files.size(regionFile);
        if (fileLength == 0) return chunkLoader.emptyChunk();

        try (FileChannel channel = FileChannel.open(regionFile, StandardOpenOption.READ)) {
            int xzChunk = (chunkZ & 0b11111) << 5 | (chunkX & 0b11111);

            byte[] header = new byte[4];
            channel.position(xzChunk * 4);
            readFully(channel, header, 0, 4);

            long offset = (header[0] & 0xFF) << 16;
            offset |= (header[1] & 0xFF) << 8;
            offset |= header[2] & 0xFF;
            offset *= 4096;
            int size = (header[3] & 0xFF) * 4096;

            if (size <= 0) return chunkLoader.emptyChunk();

            byte[] chunkDataBuffer = new byte[size];

            channel.position(offset);
            readFully(channel, chunkDataBuffer, 0, size);

            return loadChunk(chunkDataBuffer, size);
        } catch (IOException | RuntimeException ex) {
            throw new IOException("Exception trying to read chunk (%d,%d) from region '%s'".formatted(chunkX, chunkZ, regionFile), ex);
        }
    }

    @Override
    public void iterateAllChunks(ChunkConsumer<T> consumer) throws IOException {
        if (Files.notExists(regionFile)) return;

        long fileLength = Files.size(regionFile);
        if (fileLength == 0) return;

        int chunkStartX = regionPos.getX() * 32;
        int chunkStartZ = regionPos.getY() * 32;

        try (FileChannel channel = FileChannel.open(regionFile, StandardOpenOption.READ)) {
            byte[] header = new byte[1024 * 8];
            byte[] chunkDataBuffer = null;

            // read the header
            readFully(channel, header, 0, header.length);

            // iterate over all chunks
            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {
                    int xzChunk = (z & 0b11111) << 5 | (x & 0b11111);

                    int size = (header[xzChunk * 4 + 3] & 0xFF) * 4096;
                    if (size <= 0) continue;

                    int chunkX = chunkStartX + x;
                    int chunkZ = chunkStartZ + z;

                    int i = xzChunk * 4 + 4096;
                    int timestamp = header[i++] << 24;
                    timestamp |= (header[i++] & 0xFF) << 16;
                    timestamp |= (header[i++] & 0xFF) << 8;
                    timestamp |= header[i] & 0xFF;

                    // load chunk only if consumers filter returns true
                    if (consumer.filter(chunkX, chunkZ, timestamp)) {
                        i = xzChunk * 4;
                        long offset = (header[i++] & 0xFF) << 16;
                        offset |= (header[i++] & 0xFF) << 8;
                        offset |= header[i] & 0xFF;
                        offset *= 4096;

                        if (chunkDataBuffer == null || chunkDataBuffer.length < size)
                            chunkDataBuffer = new byte[size];

                        channel.position(offset);
                        readFully(channel, chunkDataBuffer, 0, size);

                        try {
                            T chunk = loadChunk(chunkDataBuffer, size);
                            consumer.accept(chunkX, chunkZ, chunk);
                        } catch (IOException ex) {
                            consumer.fail(chunkX, chunkZ, ex);
                        } catch (Exception ex) {
                            consumer.fail(chunkX, chunkZ, new IOException(ex));
                        }

                    }
                }
            }
        } catch (IOException | RuntimeException ex) {
            throw new IOException("Exception trying to iterate chunks in region '%s'".formatted(regionFile), ex);
        }
    }

    @Override
    public T emptyChunk() {
        return chunkLoader.emptyChunk();
    }

    private T loadChunk(byte[] data, int size) throws IOException {
        int compressionTypeId = Byte.toUnsignedInt(data[4]);
        Compression compression = CHUNK_COMPRESSION_MAP[compressionTypeId];
        if (compression == null)
            throw new IOException("Unknown chunk compression-id: " + compressionTypeId);

        return chunkLoader.load(data, 5, size - 5, compression);
    }

    public static String getRegionFileName(int regionX, int regionZ) {
        return "r." + regionX + "." + regionZ + FILE_SUFFIX;
    }

    @SuppressWarnings("SameParameterValue")
    private static void readFully(ReadableByteChannel src, byte[] dst, int off, int len) throws IOException {
        readFully(src, ByteBuffer.wrap(dst), off, len);
    }

    private static void readFully(ReadableByteChannel src, ByteBuffer bb, int off, int len) throws IOException {
        int limit = off + len;
        if (limit > bb.capacity()) throw new IllegalArgumentException("buffer too small");

        bb.limit(limit);
        bb.position(off);

        do {
            int read = src.read(bb);
            if (read < 0)
                // zero out all the remaining data from the buffer
                while (bb.remaining() > 0)
                    bb.put((byte) 0);
        } while (bb.remaining() > 0);
    }

}
