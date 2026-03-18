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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.regex.Pattern;

@Getter
public class LinearRegion<T> implements Region<T> {

    public static final String FILE_SUFFIX = ".linear";
    public static final Pattern FILE_PATTERN = Pattern.compile("^r\\.(-?\\d+)\\.(-?\\d+)\\.linear$");

    private static final long MAGIC = 0xc3ff13183cca9d9aL;
    private static final byte V1_VERSION_MIN = 1;
    private static final byte V1_VERSION_MAX = 2;
    private static final byte V2_VERSION = 3;
    private static final int REGION_SIZE = 32;
    private static final int CHUNK_EXISTENCE_BITMAP_BYTES = 128;
    private static final int CHUNKS_PER_REGION = REGION_SIZE * REGION_SIZE;
    private static final int[] VALID_GRID_SIZES = { 1, 2, 4, 8, 16, 32 };

    private final ChunkLoader<T> chunkLoader;
    private final Path regionFile;
    private final Vector2i regionPos;

    private boolean initialized = false;

    private byte version;
    private long newestTimestamp;
    private byte compressionLevel;
    private short chunkCount;
    private int dataLength;
    private long dataHash;
    private byte[] compressedData;

    private boolean linearV2;
    private int gridSize;
    private int bucketSize;
    private byte[][] bucketData;

    public LinearRegion(ChunkLoader<T> chunkLoader, Path regionFile) throws IllegalArgumentException {
        this.chunkLoader = chunkLoader;
        this.regionFile = regionFile;

        String[] filenameParts = regionFile.getFileName().toString().split("\\.");
        int rX = Integer.parseInt(filenameParts[1]);
        int rZ = Integer.parseInt(filenameParts[2]);

        this.regionPos = new Vector2i(rX, rZ);
    }

    private synchronized void init() throws IOException {
        if (initialized) return;
        if (Files.notExists(regionFile)) return;

        long fileLength = Files.size(regionFile);
        if (fileLength == 0) return;

        try (
            InputStream in = Files.newInputStream(regionFile, StandardOpenOption.READ);
            BufferedInputStream bIn = new BufferedInputStream(in);
            DataInputStream dIn = new DataInputStream(bIn)
        ) {
            if (dIn.readLong() != MAGIC)
                throw new IOException("Linear region-file format: invalid header magic");

            byte fileVersion = dIn.readByte();

            if (fileVersion >= V1_VERSION_MIN && fileVersion <= V1_VERSION_MAX) {
                initLinearV1(dIn, fileLength, fileVersion);
            } else if (fileVersion >= V2_VERSION) {
                initLinearV2(dIn);
            } else {
                throw new IOException("Linear region-file format: Unsupported version: " + fileVersion);
            }
        }

        initialized = true;
    }

    private void initLinearV1(DataInputStream dIn, long fileLength, byte fileVersion) throws IOException {
        version = fileVersion;
        newestTimestamp = dIn.readLong();
        compressionLevel = dIn.readByte();
        chunkCount = dIn.readShort();
        dataLength = dIn.readInt();
        dataHash = dIn.readLong();

        if (fileLength != dataLength + 40)
            throw new IOException("Linear region-file format: Invalid file length. Expected " + (dataLength + 40) + " but got " + fileLength);

        compressedData = new byte[dataLength];
        dIn.readFully(compressedData, 0, dataLength);

        if (dIn.readLong() != MAGIC)
            throw new IOException("Linear region-file format: invalid footer magic");
    }

    private void initLinearV2(DataInputStream dIn) throws IOException {
        linearV2 = true;
        newestTimestamp = dIn.readLong();

        gridSize = dIn.readByte() & 0xFF;
        if (!isValidGridSize(gridSize))
            throw new IOException("Linear region-file format: Invalid grid size: " + gridSize);
        bucketSize = REGION_SIZE / gridSize;

        dIn.readInt(); // region X
        dIn.readInt(); // region Z
        dIn.skipBytes(CHUNK_EXISTENCE_BITMAP_BYTES);
        skipNbtFeatures(dIn);

        int bucketCount = gridSize * gridSize;
        int[] bucketSizes = new int[bucketCount];
        for (int i = 0; i < bucketCount; i++) {
            bucketSizes[i] = dIn.readInt();
            dIn.readByte();  // compression level
            dIn.readLong();  // xxhash64
        }

        bucketData = new byte[bucketCount][];
        for (int i = 0; i < bucketCount; i++) {
            if (bucketSizes[i] > 0) {
                bucketData[i] = new byte[bucketSizes[i]];
                dIn.readFully(bucketData[i]);
            }
        }

        if (dIn.readLong() != MAGIC)
            throw new IOException("Linear region-file format: invalid footer magic");
    }

    private static boolean isValidGridSize(int gridSize) {
        for (int valid : VALID_GRID_SIZES)
            if (gridSize == valid) return true;
        return false;
    }

    private static void skipNbtFeatures(DataInputStream dIn) throws IOException {
        while (true) {
            int keyLen = dIn.readByte() & 0xFF;
            if (keyLen == 0) break;
            dIn.skipBytes(keyLen);
            dIn.readInt();
        }
    }

    @Override
    public void iterateAllChunks(ChunkConsumer<T> consumer) throws IOException {
        if (!initialized) init();

        if (linearV2) {
            iterateAllChunksV2(consumer);
        } else {
            iterateAllChunksV1(consumer);
        }
    }

    private void iterateAllChunksV1(ChunkConsumer<T> consumer) throws IOException {
        int chunkStartX = regionPos.getX() * REGION_SIZE;
        int chunkStartZ = regionPos.getY() * REGION_SIZE;
        byte[] chunkDataBuffer = null;

        try (
            InputStream in = Compression.ZSTD.decompress(new ByteArrayInputStream(compressedData));
            DataInputStream dIn = new DataInputStream(in)
        ) {
            int[] chunkDataLengths = new int[CHUNKS_PER_REGION];
            int[] chunkTimestamps = new int[CHUNKS_PER_REGION];
            for (int i = 0; i < CHUNKS_PER_REGION; i++) {
                chunkDataLengths[i] = dIn.readInt();
                chunkTimestamps[i] = dIn.readInt();
            }

            int i = 0;
            int toBeSkipped = 0;
            for (int z = 0; z < REGION_SIZE; z++) {
                for (int x = 0; x < REGION_SIZE; x++) {
                    int length = chunkDataLengths[i];
                    if (length > 0) {
                        int chunkX = chunkStartX + x;
                        int chunkZ = chunkStartZ + z;
                        int timestamp = version == V1_VERSION_MAX ? chunkTimestamps[i] : (int) newestTimestamp;

                        if (consumer.filter(chunkX, chunkZ, timestamp)) {
                            if (toBeSkipped > 0) skipNBytes(dIn, toBeSkipped);
                            toBeSkipped = 0;

                            if (chunkDataBuffer == null || chunkDataBuffer.length < length)
                                chunkDataBuffer = new byte[length];

                            dIn.readFully(chunkDataBuffer, 0, length);
                            acceptChunk(consumer, chunkX, chunkZ, chunkDataBuffer, length);
                        } else {
                            toBeSkipped += length;
                        }
                    }
                    i++;
                }
            }
        }
    }

    private void iterateAllChunksV2(ChunkConsumer<T> consumer) throws IOException {
        int chunkStartX = regionPos.getX() * REGION_SIZE;
        int chunkStartZ = regionPos.getY() * REGION_SIZE;
        BucketCache cache = new BucketCache(bucketData, gridSize * gridSize, bucketSize * bucketSize);
        byte[] chunkDataBuffer = null;

        for (int x = 0; x < REGION_SIZE; x++) {
            for (int z = 0; z < REGION_SIZE; z++) {
                int chunkX = chunkStartX + x;
                int chunkZ = chunkStartZ + z;

                int bx = x / bucketSize;
                int bz = z / bucketSize;
                int bucketIdx = bx * gridSize + bz;

                int slot = (x % bucketSize) * bucketSize + (z % bucketSize);

                BucketChunkEntry entry = cache.getOrParse(bucketIdx, slot);
                if (entry == null || entry.length <= 0) continue;

                if (!consumer.filter(chunkX, chunkZ, (int) entry.timestamp)) continue;

                byte[] decompressed = cache.getDecompressedBucket(bucketIdx);
                if (chunkDataBuffer == null || chunkDataBuffer.length < entry.length)
                    chunkDataBuffer = new byte[entry.length];

                System.arraycopy(decompressed, entry.offset, chunkDataBuffer, 0, entry.length);
                acceptChunk(consumer, chunkX, chunkZ, chunkDataBuffer, entry.length);
            }
        }
    }

    private void acceptChunk(ChunkConsumer<T> consumer, int chunkX, int chunkZ, byte[] buffer, int length) throws IOException {
        try {
            T chunk = chunkLoader.load(buffer, 0, length, Compression.NONE);
            consumer.accept(chunkX, chunkZ, chunk);
        } catch (IOException ex) {
            consumer.fail(chunkX, chunkZ, ex);
        } catch (Exception ex) {
            consumer.fail(chunkX, chunkZ, new IOException(ex));
        }
    }

    private static final class BucketCache {
        private final byte[][] bucketData;
        private final BucketChunkEntry[][] entries;
        private final byte[][] decompressed;
        private final int chunksPerBucket;

        BucketCache(byte[][] bucketData, int bucketCount, int chunksPerBucket) {
            this.bucketData = bucketData;
            this.entries = new BucketChunkEntry[bucketCount][];
            this.decompressed = new byte[bucketCount][];
            this.chunksPerBucket = chunksPerBucket;
        }

        BucketChunkEntry getOrParse(int bucketIdx, int slot) throws IOException {
            if (entries[bucketIdx] == null)
                entries[bucketIdx] = parseBucket(bucketIdx, bucketData, decompressed, chunksPerBucket);
            return entries[bucketIdx][slot];
        }

        byte[] getDecompressedBucket(int bucketIdx) throws IOException {
            if (decompressed[bucketIdx] == null)
                entries[bucketIdx] = parseBucket(bucketIdx, bucketData, decompressed, chunksPerBucket);
            return decompressed[bucketIdx];
        }
    }

    private static BucketChunkEntry[] parseBucket(
            int bucketIdx,
            byte[][] bucketData,
            byte[][] decompressedOut,
            int chunksPerBucket
    ) throws IOException {
        byte[] raw = bucketData[bucketIdx];
        if (raw == null || raw.length == 0) {
            decompressedOut[bucketIdx] = new byte[0];
            return emptyBucketEntries(chunksPerBucket);
        }

        byte[] decompressed;
        try (InputStream in = Compression.ZSTD.decompress(new ByteArrayInputStream(raw))) {
            decompressed = in.readAllBytes();
        }
        decompressedOut[bucketIdx] = decompressed;

        DataInputStream dIn = new DataInputStream(new ByteArrayInputStream(decompressed));
        BucketChunkEntry[] entries = new BucketChunkEntry[chunksPerBucket];
        int currentPos = 0;

        for (int i = 0; i < chunksPerBucket; i++) {
            int size = dIn.readInt();
            long timestamp = dIn.readLong();
            int dataLength = size > 8 ? size - 8 : 0;
            int dataOffset = currentPos + 4 + 8;

            entries[i] = new BucketChunkEntry(dataOffset, dataLength, timestamp);
            currentPos += 4 + 8 + dataLength;

            if (dataLength > 0)
                skipNBytes(dIn, dataLength);
        }

        return entries;
    }

    private static BucketChunkEntry[] emptyBucketEntries(int count) {
        BucketChunkEntry[] e = new BucketChunkEntry[count];
        for (int i = 0; i < count; i++)
            e[i] = new BucketChunkEntry(0, 0, 0L);
        return e;
    }

    private static final class BucketChunkEntry {
        final int offset;
        final int length;
        final long timestamp;

        BucketChunkEntry(int offset, int length, long timestamp) {
            this.offset = offset;
            this.length = length;
            this.timestamp = timestamp;
        }
    }

    @Override
    public T emptyChunk() {
        return chunkLoader.emptyChunk();
    }

    public static String getRegionFileName(int regionX, int regionZ) {
        return "r." + regionX + "." + regionZ + FILE_SUFFIX;
    }

    private static void skipNBytes(InputStream in, long n) throws IOException {
        while (n > 0) {
            long ns = in.skip(n);
            if (ns > 0 && ns <= n) {
                n -= ns;
            } else if (ns == 0) {
                if (in.read() == -1) throw new EOFException();
                n--;
            } else {
                throw new IOException("Unable to skip exactly");
            }
        }
    }
}
