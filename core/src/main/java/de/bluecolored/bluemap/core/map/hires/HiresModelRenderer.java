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
package de.bluecolored.bluemap.core.map.hires;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.TileMetaConsumer;
import de.bluecolored.bluemap.core.map.hires.block.BlockStateModelRenderer;
import de.bluecolored.bluemap.core.map.hires.entity.EntityModelRenderer;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.Chunk;
import de.bluecolored.bluemap.core.world.World;
import de.bluecolored.bluemap.core.world.block.Block;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;

public class HiresModelRenderer {

    private final ResourcePack resourcePack;
    private final RenderSettings renderSettings;

    private final ThreadLocal<BlockStateModelRenderer> threadLocalBlockRenderer;
    private final ThreadLocal<EntityModelRenderer> threadLocalEntityRenderer;

    public HiresModelRenderer(ResourcePack resourcePack, TextureGallery textureGallery, RenderSettings renderSettings) {
        this.resourcePack = resourcePack;
        this.renderSettings = renderSettings;

        this.threadLocalBlockRenderer = ThreadLocal.withInitial(() -> new BlockStateModelRenderer(resourcePack, textureGallery, renderSettings));
        this.threadLocalEntityRenderer = ThreadLocal.withInitial(() -> new EntityModelRenderer(resourcePack, textureGallery, renderSettings));
    }

    public void render(World world, Vector3i modelMin, Vector3i modelMax, TileModel model) {
        render(world, modelMin, modelMax, model, (x, z, c, h, l) -> {});
    }

    public void render(World world, Vector3i modelMin, Vector3i modelMax, TileModel tileModel, TileMetaConsumer tileMetaConsumer) {
        try {
            Vector3i min = modelMin.max(renderSettings.getMinPos());
            Vector3i max = modelMax.min(renderSettings.getMaxPos());
            Vector3i modelAnchor = new Vector3i(modelMin.getX(), 0, modelMin.getZ());

            // render blocks
            BlockStateModelRenderer blockRenderer = threadLocalBlockRenderer.get();
            EntityModelRenderer entityRenderer = threadLocalEntityRenderer.get();

            int maxHeight, minY, maxY;
            double topBlockLight;
            Color columnColor = new Color(), blockColor = new Color();
            BlockNeighborhood block = new BlockNeighborhood(new Block(world, 0, 0, 0), resourcePack, renderSettings, world.getDimensionType());
            TileModelView tileModelView = new TileModelView(tileModel);

            int x, y, z;
            for (x = modelMin.getX(); x <= modelMax.getX(); x++) {
                for (z = modelMin.getZ(); z <= modelMax.getZ(); z++) {

                    maxHeight = Integer.MIN_VALUE;
                    topBlockLight = 0;

                    columnColor.set(0, 0, 0, 0, true);

                    if (renderSettings.isInsideRenderBoundaries(x, z)) {
                        Chunk chunk = world.getChunkAtBlock(x, z);
                        minY = Math.max(min.getY(), chunk.getMinY(x, z));
                        maxY = Math.min(max.getY(), chunk.getMaxY(x, z));

                        for (y = maxY; y >= minY; y--) {
                            if (x == -1743 && y == 64 && z == 1393)
                                System.out.println();

                            block.set(x, y, z);
                            if (!block.isInsideRenderBounds()) continue;

                            tileModelView.initialize();

                            blockRenderer.render(block, tileModelView, blockColor);

                            //update topBlockLight
                            topBlockLight = Math.max(topBlockLight, block.getBlockLightLevel() * (1 - columnColor.a));

                            // move block-model to correct position
                            tileModelView.translate(x - modelAnchor.getX(), y - modelAnchor.getY(), z - modelAnchor.getZ());

                            //update color and height (only if not 100% translucent)
                            if (blockColor.a > 0) {
                                if (maxHeight < y) maxHeight = y;
                                columnColor.underlay(blockColor.premultiplied());
                            }

                            if (renderSettings.isRenderTopOnly() && blockColor.a > 0.999 && block.getProperties().isCulling())
                                break;
                        }
                    }

                    if (maxHeight == Integer.MIN_VALUE)
                        maxHeight = 0;

                    tileMetaConsumer.set(x, z, columnColor, maxHeight, (int) topBlockLight);
                }
            }

            // render entities
            world.iterateEntities(min.getX(), min.getZ(), max.getX(), max.getZ(), entity -> {
                Vector3d pos = entity.getPos();
                block.set(pos.getFloorX(), pos.getFloorY(), pos.getFloorZ());
                entityRenderer.render(entity, block, tileModelView.initialize());
                tileModelView.translate(
                        (float) pos.getX() - modelAnchor.getX(),
                        (float) pos.getY() - modelAnchor.getY(),
                        (float) pos.getZ() - modelAnchor.getZ()
                );
            });
        } catch (MaxCapacityReachedException ex) {
            Logger.global.noFloodWarning("max-capacity-reached",
                    "One or more map-tiles are too complex to be completed (@~ %s to %s): %s".formatted(modelMin, modelMax, ex));
        }
    }
}
