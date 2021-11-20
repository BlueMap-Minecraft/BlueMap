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

import java.util.Arrays;

public class LegacyBiomes {

    private static final String[] BIOME_IDS = new String[170];
    static {
        Arrays.fill(BIOME_IDS, "minecraft:ocean");
        BIOME_IDS[0] = "minecraft:ocean";
        BIOME_IDS[1] = "minecraft:plains";
        BIOME_IDS[2] = "minecraft:desert";
        BIOME_IDS[3] = "minecraft:mountains";
        BIOME_IDS[4] = "minecraft:forest";
        BIOME_IDS[5] = "minecraft:taiga";
        BIOME_IDS[6] = "minecraft:swamp";
        BIOME_IDS[7] = "minecraft:river";
        BIOME_IDS[8] = "minecraft:nether";
        BIOME_IDS[9] = "minecraft:the_end";
        BIOME_IDS[10] = "minecraft:frozen_ocean";
        BIOME_IDS[11] = "minecraft:frozen_river";
        BIOME_IDS[12] = "minecraft:snowy_tundra";
        BIOME_IDS[13] = "minecraft:snowy_mountains";
        BIOME_IDS[14] = "minecraft:mushroom_fields";
        BIOME_IDS[15] = "minecraft:mushroom_field_shore";
        BIOME_IDS[16] = "minecraft:beach";
        BIOME_IDS[17] = "minecraft:desert_hills";
        BIOME_IDS[18] = "minecraft:wooded_hills";
        BIOME_IDS[19] = "minecraft:taiga_hills";
        BIOME_IDS[20] = "minecraft:mountain_edge";
        BIOME_IDS[21] = "minecraft:jungle";
        BIOME_IDS[22] = "minecraft:jungle_hills";
        BIOME_IDS[23] = "minecraft:jungle_edge";
        BIOME_IDS[24] = "minecraft:deep_ocean";
        BIOME_IDS[25] = "minecraft:stone_shore";
        BIOME_IDS[26] = "minecraft:snowy_beach";
        BIOME_IDS[27] = "minecraft:birch_forest";
        BIOME_IDS[28] = "minecraft:birch_forest_hills";
        BIOME_IDS[29] = "minecraft:dark_forest";
        BIOME_IDS[30] = "minecraft:snowy_taiga";
        BIOME_IDS[31] = "minecraft:snowy_taiga_hills";
        BIOME_IDS[32] = "minecraft:giant_tree_taiga";
        BIOME_IDS[33] = "minecraft:giant_tree_taiga_hills";
        BIOME_IDS[34] = "minecraft:wooded_mountains";
        BIOME_IDS[35] = "minecraft:savanna";
        BIOME_IDS[36] = "minecraft:savanna_plateau";
        BIOME_IDS[37] = "minecraft:badlands";
        BIOME_IDS[38] = "minecraft:wooded_badlands_plateau";
        BIOME_IDS[39] = "minecraft:badlands_plateau";
        BIOME_IDS[40] = "minecraft:small_end_islands";
        BIOME_IDS[41] = "minecraft:end_midlands";
        BIOME_IDS[42] = "minecraft:end_highlands";
        BIOME_IDS[43] = "minecraft:end_barrens";
        BIOME_IDS[44] = "minecraft:warm_ocean";
        BIOME_IDS[45] = "minecraft:lukewarm_ocean";
        BIOME_IDS[46] = "minecraft:cold_ocean";
        BIOME_IDS[47] = "minecraft:deep_warm_ocean";
        BIOME_IDS[48] = "minecraft:deep_lukewarm_ocean";
        BIOME_IDS[49] = "minecraft:deep_cold_ocean";
        BIOME_IDS[50] = "minecraft:deep_frozen_ocean";
        BIOME_IDS[127] = "minecraft:the_void";
        BIOME_IDS[129] = "minecraft:sunflower_plains";
        BIOME_IDS[130] = "minecraft:desert_lakes";
        BIOME_IDS[131] = "minecraft:gravelly_mountains";
        BIOME_IDS[132] = "minecraft:flower_forest";
        BIOME_IDS[133] = "minecraft:taiga_mountains";
        BIOME_IDS[134] = "minecraft:swamp_hills";
        BIOME_IDS[140] = "minecraft:ice_spikes";
        BIOME_IDS[149] = "minecraft:modified_jungle";
        BIOME_IDS[151] = "minecraft:modified_jungle_edge";
        BIOME_IDS[155] = "minecraft:tall_birch_forest";
        BIOME_IDS[156] = "minecraft:tall_birch_hills";
        BIOME_IDS[157] = "minecraft:dark_forest_hills";
        BIOME_IDS[158] = "minecraft:snowy_taiga_mountains";
        BIOME_IDS[160] = "minecraft:giant_spruce_taiga";
        BIOME_IDS[161] = "minecraft:giant_spruce_taiga_hills";
        BIOME_IDS[162] = "minecraft:modified_gravelly_mountains";
        BIOME_IDS[163] = "minecraft:shattered_savanna";
        BIOME_IDS[164] = "minecraft:shattered_savanna_plateau";
        BIOME_IDS[165] = "minecraft:eroded_badlands";
        BIOME_IDS[166] = "minecraft:modified_wooded_badlands_plateau";
        BIOME_IDS[167] = "minecraft:modified_badlands_plateau";
        BIOME_IDS[168] = "minecraft:bamboo_jungle";
        BIOME_IDS[169] = "minecraft:bamboo_jungle_hills";
    }

    public static String idFor(int legacyId) {
        if (legacyId < 0 || legacyId > BIOME_IDS.length) legacyId = 0;
        return BIOME_IDS[legacyId];
    }

}
