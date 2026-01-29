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
