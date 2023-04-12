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
package de.bluecolored.bluemap.core.mca;

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.world.Chunk;
import de.bluecolored.bluemap.core.world.EmptyChunk;
import de.bluecolored.bluemap.core.world.Region;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.Tag;
import net.querz.nbt.mca.CompressionType;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MCARegion implements Region {

    private final MCAWorld world;
    private final File regionFile;
    private final Vector2i regionPos;

    public MCARegion(MCAWorld world, File regionFile) throws IllegalArgumentException {
        this.world = world;
        this.regionFile = regionFile;

        String[] filenameParts = regionFile.getName().split("\\.");
        int rX = Integer.parseInt(filenameParts[1]);
        int rZ = Integer.parseInt(filenameParts[2]);

        this.regionPos = new Vector2i(rX, rZ);
    }

    @Override
    public Chunk loadChunk(int chunkX, int chunkZ, boolean ignoreMissingLightData) throws IOException {
        if (!regionFile.exists() || regionFile.length() == 0) return EmptyChunk.INSTANCE;

        try (RandomAccessFile raf = new RandomAccessFile(regionFile, "r")) {

            int xzChunk = Math.floorMod(chunkZ, 32) * 32 + Math.floorMod(chunkX, 32);

            raf.seek(xzChunk * 4L);
            int offset = raf.read() << 16;
            offset |= (raf.read() & 0xFF) << 8;
            offset |= raf.read() & 0xFF;
            offset *= 4096;

            int size = raf.readByte() * 4096;
            if (size == 0) {
                return EmptyChunk.INSTANCE;
            }

            raf.seek(offset + 4); // +4 skip chunk size

            byte compressionTypeByte = raf.readByte();
            CompressionType compressionType = CompressionType.getFromID(compressionTypeByte);
            if (compressionType == null) {
                throw new IOException("Invalid compression type " + compressionTypeByte);
            }

            DataInputStream dis = new DataInputStream(new BufferedInputStream(compressionType.decompress(new FileInputStream(raf.getFD()))));
            Tag<?> tag = Tag.deserialize(dis, Tag.DEFAULT_MAX_DEPTH);
            if (tag instanceof CompoundTag) {
                MCAChunk chunk = MCAChunk.create(world, (CompoundTag) tag);
                if (!chunk.isGenerated()) return EmptyChunk.INSTANCE;
                return chunk;
            } else {
                throw new IOException("Invalid data tag: " + (tag == null ? "null" : tag.getClass().getName()));
            }

        } catch (RuntimeException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Collection<Vector2i> listChunks(long modifiedSince) {
        if (!regionFile.exists() || regionFile.length() == 0) return Collections.emptyList();

        List<Vector2i> chunks = new ArrayList<>(1024); //1024 = 32 x 32 chunks per region-file

        try (RandomAccessFile raf = new RandomAccessFile(regionFile, "r")) {
            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {
                    Vector2i chunk = new Vector2i(regionPos.getX() * 32 + x, regionPos.getY() * 32 + z);
                    int xzChunk = z * 32 + x;

                    raf.seek(xzChunk * 4 + 3);
                    int size = raf.readByte() * 4096;

                    if (size == 0) continue;

                    raf.seek(xzChunk * 4 + 4096);
                    int timestamp = raf.read() << 24;
                    timestamp |= (raf.read() & 0xFF) << 16;
                    timestamp |= (raf.read() & 0xFF) << 8;
                    timestamp |= raf.read() & 0xFF;

                    if (timestamp >= (modifiedSince / 1000)) {
                        chunks.add(chunk);
                    }
                }
            }
        } catch (RuntimeException | IOException ex) {
            Logger.global.logWarning("Failed to read .mca file: " + regionFile.getAbsolutePath() + " (" + ex + ")");
        }

        return chunks;
    }

    @Override
    public File getRegionFile() {
        return regionFile;
    }

}
