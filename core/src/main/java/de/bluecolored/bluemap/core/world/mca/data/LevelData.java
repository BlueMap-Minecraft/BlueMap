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
package de.bluecolored.bluemap.core.world.mca.data;

import de.bluecolored.bluemap.core.world.DimensionType;
import de.bluecolored.bluenbt.NBTName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@SuppressWarnings("FieldMayBeFinal")
public class LevelData {

    @NBTName("Data")
    private Data data = new Data();

    @Getter
    public static class Data {

        @NBTName("LevelName")
        private String levelName = "world";

        @NBTName("SpawnX")
        private int spawnX = 0;

        @NBTName("SpawnY")
        private int spawnY = 0;

        @NBTName("SpawnZ")
        private int spawnZ = 0;

        @NBTName("WorldGenSettings")
        private WGSettings worldGenSettings = new WGSettings();

    }

    @Getter
    public static class WGSettings {
        private Map<String, Dimension> dimensions = new HashMap<>();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Dimension {
        private DimensionType type = DimensionType.OVERWORLD;
    }

}
