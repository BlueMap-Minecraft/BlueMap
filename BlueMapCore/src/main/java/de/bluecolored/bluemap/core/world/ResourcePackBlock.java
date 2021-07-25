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
package de.bluecolored.bluemap.core.world;

import de.bluecolored.bluemap.core.resourcepack.ResourcePack;

import java.util.Objects;

public class ResourcePackBlock<T extends ResourcePackBlock<T>> extends Block<T> {

    private final ResourcePack resourcePack;
    private BlockProperties properties;
    private Biome biome;

    public ResourcePackBlock(ResourcePack resourcePack, World world, int x, int y, int z) {
        super(world, x, y, z);
        this.resourcePack = Objects.requireNonNull(resourcePack);
    }

    @Override
    protected void reset() {
        super.reset();

        this.properties = null;
        this.biome = null;
    }

    public BlockProperties getProperties() {
        if (properties == null) properties = resourcePack.getBlockProperties(getBlockState());
        return properties;
    }

    public Biome getBiome() {
        if (biome == null) biome = resourcePack.getBiome(getBiomeId());
        return biome;
    }

    public ResourcePack getResourcePack() {
        return resourcePack;
    }

}
