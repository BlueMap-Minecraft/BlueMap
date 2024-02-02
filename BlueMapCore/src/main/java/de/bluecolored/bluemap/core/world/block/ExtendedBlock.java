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
package de.bluecolored.bluemap.core.world.block;

import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.resources.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.world.*;

import java.util.Objects;

public class ExtendedBlock<T extends ExtendedBlock<T>> extends Block<T> {
    private final ResourcePack resourcePack;
    private final RenderSettings renderSettings;

    private BlockProperties properties;
    private Biome biome;

    private boolean insideRenderBoundsCalculated, insideRenderBounds;
    private boolean isCaveCalculated, isCave;

    public ExtendedBlock(ResourcePack resourcePack, RenderSettings renderSettings, World world, int x, int y, int z) {
        super(world, x, y, z);
        this.resourcePack = Objects.requireNonNull(resourcePack);
        this.renderSettings = renderSettings;
    }

    @Override
    protected void reset() {
        super.reset();

        this.properties = null;
        this.biome = null;

        this.insideRenderBoundsCalculated = false;
        this.isCaveCalculated = false;
    }

    public T copy(ExtendedBlock<?> source) {
        super.copy(source);

        this.properties = source.properties;
        this.biome = source.biome;

        this.insideRenderBoundsCalculated = source.insideRenderBoundsCalculated;
        this.insideRenderBounds = source.insideRenderBounds;

        this.isCaveCalculated = source.isCaveCalculated;
        this.isCave = source.isCave;

        return self();
    }

    @Override
    public BlockState getBlockState() {
        if (renderSettings.isRenderEdges() && !isInsideRenderBounds()) return BlockState.AIR;
        return super.getBlockState();
    }

    @Override
    public LightData getLightData() {
        LightData ld = super.getLightData();
        if (renderSettings.isRenderEdges() && !isInsideRenderBounds()) ld.set(getWorld().getDimensionType().hasSkylight() ? 16 : 0, ld.getBlockLight());
        return ld;
    }

    public BlockProperties getProperties() {
        if (properties == null) properties = resourcePack.getBlockProperties(getBlockState());
        return properties;
    }

    public Biome getBiome() {
        if (biome == null) biome = resourcePack.getBiome(getBiomeId());
        return biome;
    }

    public RenderSettings getRenderSettings() {
        return renderSettings;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isInsideRenderBounds() {
        if (!insideRenderBoundsCalculated) {
            insideRenderBounds = renderSettings.isInsideRenderBoundaries(getX(), getY(), getZ());
            insideRenderBoundsCalculated = true;
        }

        return insideRenderBounds;
    }

    public boolean isRemoveIfCave() {
        if (!isCaveCalculated) {
            isCave = getY() < renderSettings.getRemoveCavesBelowY() &&
                    (
                            !getChunk().hasOceanFloorHeights() ||
                            getY() < getChunk().getOceanFloorY(getX(), getZ()) +
                                    renderSettings.getCaveDetectionOceanFloor()
                    );
            isCaveCalculated = true;
        }

        return isCave;
    }

    public ResourcePack getResourcePack() {
        return resourcePack;
    }

}
