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
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.world.*;
import de.bluecolored.bluemap.core.world.biome.Biome;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ExtendedBlock implements BlockAccess {

    private int x, y, z;
    private BlockAccess blockAccess;

    @Getter private ResourcePack resourcePack;
    @Getter private RenderSettings renderSettings;
    @Getter private DimensionType dimensionType;

    private @Nullable BlockProperties properties;

    private boolean insideRenderBoundsCalculated, insideRenderBounds;
    private boolean isCaveCalculated, isCave;

    public ExtendedBlock(BlockAccess blockAccess, ResourcePack resourcePack, RenderSettings renderSettings, DimensionType dimensionType) {
        this.blockAccess = Objects.requireNonNull(blockAccess);
        this.resourcePack = Objects.requireNonNull(resourcePack);
        this.renderSettings = Objects.requireNonNull(renderSettings);
        this.dimensionType = Objects.requireNonNull(dimensionType);
    }

    @Override
    public void set(int x, int y, int z) {
        if (this.y == y && this.x == x && this.z == z)
            return;

        this.x = x;
        this.y = y;
        this.z = z;
        this.properties = null;
        this.insideRenderBoundsCalculated = false;
        this.isCaveCalculated = false;

        blockAccess.set(x, y, z);
    }

    @Override
    public ExtendedBlock copy() {
        return new ExtendedBlock(blockAccess.copy(), resourcePack, renderSettings, dimensionType);
    }

    protected void copyFrom(ExtendedBlock extendedBlock) {
        this.blockAccess = extendedBlock.blockAccess;
        this.resourcePack = extendedBlock.resourcePack;
        this.renderSettings = extendedBlock.renderSettings;
        this.dimensionType = extendedBlock.dimensionType;
        this.properties = extendedBlock.properties;
        this.insideRenderBoundsCalculated = extendedBlock.insideRenderBoundsCalculated;
        this.insideRenderBounds = extendedBlock.insideRenderBounds;
        this.isCaveCalculated = extendedBlock.isCaveCalculated;
        this.isCave = extendedBlock.isCave;
    }

    @Override
    public int getX() {
        return blockAccess.getX();
    }

    @Override
    public int getY() {
        return blockAccess.getY();
    }

    @Override
    public int getZ() {
        return blockAccess.getZ();
    }

    @Override
    public BlockState getBlockState() {
        if (renderSettings.isRenderEdges() && !isInsideRenderBounds()) return BlockState.AIR;
        return blockAccess.getBlockState();
    }

    @Override
    public LightData getLightData() {
        LightData ld = blockAccess.getLightData();
        if (renderSettings.isRenderEdges() && !isInsideRenderBounds()) ld.set(dimensionType.hasSkylight() ? 16 : 0, ld.getBlockLight());
        return ld;
    }

    @Override
    public Biome getBiome() {
        return blockAccess.getBiome();
    }

    @Override
    public @Nullable BlockEntity getBlockEntity() {
        return blockAccess.getBlockEntity();
    }

    @Override
    public boolean hasOceanFloorY() {
        return blockAccess.hasOceanFloorY();
    }

    @Override
    public int getOceanFloorY() {
        return blockAccess.getOceanFloorY();
    }

    public BlockProperties getProperties() {
        if (properties == null) properties = resourcePack.getBlockProperties(getBlockState());
        return properties;
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
                            !hasOceanFloorY() ||
                            getY() < getOceanFloorY() +
                                    renderSettings.getCaveDetectionOceanFloor()
                    );
            isCaveCalculated = true;
        }

        return isCave;
    }

}
