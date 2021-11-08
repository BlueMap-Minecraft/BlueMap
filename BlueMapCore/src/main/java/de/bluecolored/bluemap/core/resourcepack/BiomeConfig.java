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
package de.bluecolored.bluemap.core.resourcepack;

import de.bluecolored.bluemap.core.world.Biome;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.Map.Entry;

public class BiomeConfig {

    private Biome[] biomes;

    public BiomeConfig() {
        biomes = new Biome[10];
    }

    public void load(ConfigurationNode node) {
        for (Entry<Object, ? extends ConfigurationNode> e : node.childrenMap().entrySet()){
            String id = e.getKey().toString();
            Biome biome = Biome.create(id, e.getValue());

            int numeralId = biome.getNumeralId();
            ensureAvailability(numeralId);
            biomes[numeralId] = biome;
        }
    }

    public Biome getBiome(int id) {
        if (id > 0 && id < biomes.length) {
            Biome biome = biomes[id];
            return biome != null ? biome : Biome.DEFAULT;
        }

        return Biome.DEFAULT;
    }

    private void ensureAvailability(int id) {
        if (id >= biomes.length) {
            int newSize = biomes.length;
            do {
                newSize = (int) (newSize * 1.5) + 1;
            } while (id >= newSize);

            Biome[] newArray = new Biome[newSize];
            System.arraycopy(biomes, 0, newArray, 0, biomes.length);
            biomes = newArray;
        }
    }

}
