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
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Keyed;
import de.bluecolored.bluemap.core.util.Registry;
import de.bluecolored.bluemap.core.world.Region;
import de.bluecolored.bluemap.core.world.mca.ChunkLoader;
import de.bluecolored.bluemap.core.world.mca.chunk.MCAChunkLoader;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface RegionType extends Keyed {

    RegionType MCA = new Impl(Key.bluemap("mca"), MCARegion::new, MCARegion::getRegionFileName, MCARegion.FILE_PATTERN);
    RegionType LINEAR = new Impl(Key.bluemap("linear"), LinearRegion::new, LinearRegion::getRegionFileName, LinearRegion.FILE_PATTERN);

    RegionType DEFAULT = MCA;
    Registry<RegionType> REGISTRY = new Registry<>(
            MCA,
            LINEAR
    );

    /**
     * Creates a new {@link Region} from the given world and region-file
     */
    <T> Region<T> createRegion(ChunkLoader<T> chunkLoader, Path regionFile);

    /**
     * Converts region coordinates into the region-file name.
     */
    String getRegionFileName(int regionX, int regionZ);

    /**
     * Converts the region-file name into region coordinates.
     * Returns null if the name does not match the expected format.
     */
    @Nullable Vector2i getRegionFromFileName(String fileName);

    static @Nullable RegionType forFileName(String fileName) {
        for (RegionType regionType : REGISTRY.values()) {
            if (regionType.getRegionFromFileName(fileName) != null)
                return regionType;
        }

        return null;
    }

    static @Nullable Vector2i regionForFileName(String fileName) {
        for (RegionType regionType : REGISTRY.values()) {
            Vector2i pos = regionType.getRegionFromFileName(fileName);
            if (pos != null) return pos;
        }

        return null;
    }

    static <T> Region<T> loadRegion(ChunkLoader<T> chunkLoader, Path regionFolder, int regionX, int regionZ) {
        for (RegionType regionType : REGISTRY.values()) {
            Path regionFile = regionFolder.resolve(regionType.getRegionFileName(regionX, regionZ));
            if (Files.exists(regionFile)) return regionType.createRegion(chunkLoader, regionFile);
        }
        return DEFAULT.createRegion(chunkLoader, regionFolder.resolve(DEFAULT.getRegionFileName(regionX, regionZ)));
    }

    @RequiredArgsConstructor
    class Impl implements RegionType {

        @Getter private final Key key;
        private final RegionFactory regionFactory;
        private final RegionFileNameFunction regionFileNameFunction;
        private final Pattern regionFileNamePattern;

        public <T> Region<T> createRegion(ChunkLoader<T> chunkLoader, Path regionFile) {
            return this.regionFactory.create(chunkLoader, regionFile);
        }

        public String getRegionFileName(int regionX, int regionZ) {
            return regionFileNameFunction.getRegionFileName(regionX, regionZ);
        }

        @Override
        public @Nullable Vector2i getRegionFromFileName(String fileName) {
            Matcher matcher = regionFileNamePattern.matcher(fileName);
            if (!matcher.matches()) return null;

            try {

                int regionX = Integer.parseInt(matcher.group(1));
                int regionZ = Integer.parseInt(matcher.group(2));

                // sanity-check for roughly minecraft max boundaries (-30 000 000 to 30 000 000)
                if (
                        regionX < -100000 || regionX > 100000 ||
                        regionZ < -100000 || regionZ > 100000
                ) {
                    return null;
                }

                return new Vector2i(regionX, regionZ);

            } catch (NumberFormatException ex) {
                return null;
            }
        }

    }

    @FunctionalInterface
    interface RegionFactory {
        <T> Region<T> create(ChunkLoader<T> chunkLoader, Path regionFile);
    }

    @FunctionalInterface
    interface RegionFileNameFunction {
        String getRegionFileName(int regionX, int regionZ);
    }

}
