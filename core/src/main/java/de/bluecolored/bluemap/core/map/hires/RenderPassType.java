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

import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.block.BlockRenderPass;
import de.bluecolored.bluemap.core.map.hires.entity.EntityRenderPass;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Keyed;
import de.bluecolored.bluemap.core.util.Registry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public interface RenderPassType extends Keyed, RenderPassFactory {

    RenderPassType BLOCKS = new Impl(Key.bluemap("blocks"), BlockRenderPass::new);
    RenderPassType ENTITIES = new Impl(Key.bluemap("entities"), EntityRenderPass::new);

    Registry<RenderPassType> REGISTRY = new Registry<>(
            BLOCKS,
            ENTITIES
    );

    @RequiredArgsConstructor
    class Impl implements RenderPassType {

        @Getter
        private final Key key;
        private final RenderPassFactory factory;

        @Override
        public RenderPass create(ResourcePack resourcePack, TextureGallery textureGallery, RenderSettings renderSettings) {
            return factory.create(resourcePack, textureGallery, renderSettings);
        }

    }

}
