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

import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Keyed;
import de.bluecolored.bluemap.core.util.Registry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public interface EntityRendererType extends Keyed, EntityRendererFactory {

    EntityRendererType DEFAULT = new Impl(Key.bluemap("default"), ResourceModelRenderer::new);
    EntityRendererType MISSING = new Impl(Key.bluemap("missing"), MissingModelRenderer::new);

    EntityRendererType LLAMA = new Impl(Key.minecraft("llama"), LlamaRenderer::new);
    EntityRendererType BEE = new Impl(Key.minecraft("bee"), BeeRenderer::new);
    EntityRendererType CAT = new Impl(Key.minecraft("cat"), CatRenderer::new);
    EntityRendererType OCELOT = new Impl(Key.minecraft("ocelot"), OcelotRenderer::new);
    EntityRendererType CHICKEN = new Impl(Key.minecraft("chicken"), ChickenRenderer::new);
    EntityRendererType FOX = new Impl(Key.minecraft("fox"), FoxRenderer::new);

    Registry<EntityRendererType> REGISTRY = new Registry<>(
            DEFAULT,
            MISSING,
            LLAMA,
            BEE,
            CAT,
            OCELOT,
            CHICKEN,
            FOX
    );

    /**
     * If the loaded resourcepack does not have any resources for this entity, this method will be called.
     * If this method returns true, this renderer will be used to render the entity instead.
     *
     * <p>
     *     This can (and should only then) be used to provide a way of rendering entities that are completely dynamically
     *     created by a mod, and there is no way to provide static entity resources that point at the correct renderer.
     * </p>
     *
     * @param entityType The entity-type {@link Key} that was not found in the loaded resources.
     * @return true if this renderer-type can render the provided entity-type {@link Key} despite missing resources.
     */
    default boolean isFallbackFor(Key entityType) {
        return false;
    }

    @RequiredArgsConstructor
    class Impl implements EntityRendererType {

        @Getter private final Key key;
        private final EntityRendererFactory rendererFactory;

        @Override
        public EntityRenderer create(ResourcePack resourcePack, TextureGallery textureGallery, RenderSettings renderSettings) {
            return rendererFactory.create(resourcePack, textureGallery, renderSettings);
        }

    }

}
