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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.regex.Pattern;

/*
 * LinearFormat:
 *
 *  REGION-FILE:
 *   8 byte - MAGIC value
 *   1 byte - version
 *   8 byte - region timestamp
 *   1 byte - compression level
 *   2 byte - chunk count
 *   4 byte - data-length in bytes
 *   8 byte - data-hash
 *   ? byte - data
 *   8 byte - MAGIC value
 *
 *  DATA: (zstd compressed)
 *   32 * 32 * 8 - header:
 *    4 byte - chunk-data-length
 *    4 byte - timestamp
 *   ? - chunks
 *
 */

@Getter
public class LinearRegion<T> implements Region<T> {

    public static final String FILE_SUFFIX = ".linear";
    public static final Pattern FILE_PATTERN = Pattern.compile("^r\\.(-?\\d+)\\.(-?\\d+)\\.linear$");

    private static final long MAGIC = 0xc3ff13183cca9d9aL;

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

            // read the header
            version = dIn.readByte();
            newestTimestamp = dIn.readLong();
            compressionLevel = dIn.readByte();
            chunkCount = dIn.readShort();
            dataLength = dIn.readInt();
            dataHash = dIn.readLong();

            if (version < 1 || version > 2)
                throw new IOException("Linear region-file format: Unsupported version: " + version);

            if (fileLength != dataLength + 40) // 40 = header + footer
                throw new IOException("Linear region-file format: Invalid file length. Expected " + (dataLength + 40) + " but got " + fileLength);

            compressedData = new byte[dataLength];
            dIn.readFully(compressedData, 0, dataLength);

            if (dIn.readLong() != MAGIC)
                throw new IOException("Linear region-file format: invalid footer magic");

        }

        initialized = true;
    }

    @Override
    public void iterateAllChunks(ChunkConsumer<T> consumer) throws IOException {
        if (!initialized) init();

        int chunkStartX = regionPos.getX() * 32;
        int chunkStartZ = regionPos.getY() * 32;

        byte[] chunkDataBuffer = null;

        try (
                InputStream in = Compression.ZSTD.decompress(new ByteArrayInputStream(compressedData));
                DataInputStream dIn = new DataInputStream(in)
        ) {
            int[] chunkDataLengths = new int[1024];
            int[] chunkTimestamps = new int[1024];
            for (int i = 0 ; i < 1024 ; i++) {
                chunkDataLengths[i] = dIn.readInt();
                chunkTimestamps[i] = dIn.readInt();
            }

            int i = 0;
            int toBeSkipped = 0;
            for (int z = 0; z < 32; z++) {
                for (int x = 0; x < 32; x++) {
                    int length = chunkDataLengths[i];
                    if (length > 0) {
                        int chunkX = chunkStartX + x;
                        int chunkZ = chunkStartZ + z;
                        int timestamp = version == 2 ? chunkTimestamps[i] : (int) newestTimestamp; //TODO: check if in seconds or milliseconds

                        if (consumer.filter(chunkX, chunkZ, timestamp)) {
                            if (toBeSkipped > 0) skipNBytes(dIn, toBeSkipped);

                            if (chunkDataBuffer == null || chunkDataBuffer.length < length)
                                chunkDataBuffer = new byte[length];
                            dIn.readFully(chunkDataBuffer, 0, length);

                            try {
                                T chunk = chunkLoader.load(chunkDataBuffer, 0, length, Compression.NONE);
                                consumer.accept(chunkX, chunkZ, chunk);
                            } catch (IOException ex) {
                                consumer.fail(chunkX, chunkZ, ex);
                            } catch (Exception ex) {
                                consumer.fail(chunkX, chunkZ, new IOException(ex));
                            }

                        } else {
                            // skip before reading the next chunk, but only if there is a next chunk
                            // that we actually want to read, to avoid decompressing unnecessary data
                            toBeSkipped += length;
                        }
                    }

                    i++;
                }
            }

        }
    }

    @Override
    public T emptyChunk() {
        return chunkLoader.emptyChunk();
    }

    public static String getRegionFileName(int regionX, int regionZ) {
        return "r." + regionX + "." + regionZ + FILE_SUFFIX;
    }

    /**
     * This method is taken here from a newer version of {@link InputStream},
     * to ensure Java 11 compatibility.
     */
    private static void skipNBytes(InputStream in, long n) throws IOException {
        while (n > 0) {
            long ns = in.skip(n);
            if (ns > 0 && ns <= n) {
                // adjust number to skip
                n -= ns;
            } else if (ns == 0) { // no bytes skipped
                // read one byte to check for EOS
                if (in.read() == -1) {
                    throw new EOFException();
                }
                // one byte read so decrement number to skip
                n--;
            } else { // skipped negative or too many bytes
                throw new IOException("Unable to skip exactly");
            }
        }
    }

}