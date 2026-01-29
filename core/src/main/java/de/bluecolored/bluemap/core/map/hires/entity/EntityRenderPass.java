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
package de.bluecolored.bluemap.core.map.hires.entity;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.TileMetaConsumer;
import de.bluecolored.bluemap.core.map.hires.RenderPass;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.map.hires.block.BlockStateModelRenderer;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.Chunk;
import de.bluecolored.bluemap.core.world.World;
import de.bluecolored.bluemap.core.world.block.Block;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;

public class EntityRenderPass implements RenderPass {

    private final ResourcePack resourcePack;
    private final RenderSettings renderSettings;
    private final EntityModelRenderer entityRenderer;

    public EntityRenderPass(ResourcePack resourcePack, TextureGallery textureGallery, RenderSettings renderSettings) {
        this.resourcePack = resourcePack;
        this.renderSettings = renderSettings;
        this.entityRenderer = new EntityModelRenderer(resourcePack, textureGallery, renderSettings);
    }

    @Override
    public void render(World world, Vector3i modelMin, Vector3i modelMax, Vector3i modelAnchor, TileModelView model, TileMetaConsumer tileMetaConsumer) {
        BlockNeighborhood block = new BlockNeighborhood(new Block(world, 0, 0, 0), resourcePack, renderSettings, world.getDimensionType());
        world.iterateEntities(modelMin.getX(), modelMin.getZ(), modelMax.getX(), modelMax.getZ(), entity -> {
            Vector3d pos = entity.getPos();
            block.set(pos.getFloorX(), pos.getFloorY(), pos.getFloorZ());
            entityRenderer.render(entity, block, model.initialize());
            model.translate(
                    (float) pos.getX() - modelAnchor.getX(),
                    (float) pos.getY() - modelAnchor.getY(),
                    (float) pos.getZ() - modelAnchor.getZ()
            );
        });
    }
}
