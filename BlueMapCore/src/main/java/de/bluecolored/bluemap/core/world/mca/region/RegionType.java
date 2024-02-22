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

import de.bluecolored.bluemap.core.world.Region;
import de.bluecolored.bluemap.core.world.mca.MCAWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;

public enum RegionType {

    MCA (MCARegion::new, MCARegion.FILE_SUFFIX, MCARegion::getRegionFileName),
    LINEAR (LinearRegion::new, LinearRegion.FILE_SUFFIX, LinearRegion::getRegionFileName);

    // we do this to improve performance, as calling values() creates a new array each time
    private final static RegionType[] VALUES = values();
    private final static RegionType DEFAULT = MCA;

    private final String fileSuffix;
    private final RegionFactory regionFactory;
    private final RegionFileNameFunction regionFileNameFunction;

    RegionType(RegionFactory regionFactory, String fileSuffix, RegionFileNameFunction regionFileNameFunction) {
        this.fileSuffix = fileSuffix;
        this.regionFactory = regionFactory;
        this.regionFileNameFunction = regionFileNameFunction;
    }

    public String getFileSuffix() {
        return fileSuffix;
    }

    public Region createRegion(MCAWorld world, Path regionFile) {
        return this.regionFactory.create(world, regionFile);
    }

    public String getRegionFileName(int regionX, int regionZ) {
        return regionFileNameFunction.getRegionFileName(regionX, regionZ);
    }

    public Path getRegionFile(Path regionFolder, int regionX, int regionZ) {
        return regionFolder.resolve(getRegionFileName(regionX, regionZ));
    }

    @Nullable
    public static RegionType forFileName(String fileName) {
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < VALUES.length; i++) {
            RegionType regionType = VALUES[i];
            if (fileName.endsWith(regionType.fileSuffix))
                return regionType;
        }
        return null;
    }

    @NotNull
    public static Region loadRegion(MCAWorld world, Path regionFolder, int regionX, int regionZ) {
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < VALUES.length; i++) {
            RegionType regionType = VALUES[i];
            Path regionFile = regionType.getRegionFile(regionFolder, regionX, regionZ);
            if (Files.exists(regionFile)) return regionType.createRegion(world, regionFile);
        }
        return DEFAULT.createRegion(world, DEFAULT.getRegionFile(regionFolder, regionX, regionZ));
    }

    @FunctionalInterface
    interface RegionFactory {
        Region create(MCAWorld world, Path regionFile);
    }

    @FunctionalInterface
    interface RegionFileNameFunction {
        String getRegionFileName(int regionX, int regionZ);
    }

}
