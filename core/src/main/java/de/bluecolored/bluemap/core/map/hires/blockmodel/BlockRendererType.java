package de.bluecolored.bluemap.core.map.hires.blockmodel;

import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Keyed;
import de.bluecolored.bluemap.core.util.Registry;
import de.bluecolored.bluemap.core.world.BlockState;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public interface BlockRendererType extends Keyed, BlockRendererFactory {

    BlockRendererType DEFAULT = new Impl(Key.bluemap("default"), ResourceModelRenderer::new);
    BlockRendererType LIQUID = new Impl(Key.bluemap("liquid"), LiquidModelRenderer::new);
    BlockRendererType MISSING = new Impl(Key.bluemap("missing"), MissingModelRenderer::new);

    Registry<BlockRendererType> REGISTRY = new Registry<>(
            DEFAULT,
            LIQUID,
            MISSING
    );

    /**
     * If the loaded resourcepack does not have any resources for this blockState, this method will be called.
     * If this method returns true, this renderer will be used to render the block instead of rendering the default
     * black-purple "missing block" model.
     * When rendering, the provided "variant" will always be bluemaps default "missing-block" resource.
     *
     * <p>
     *     This can (and should only then) be used to provide a way of rendering blocks that are completely dynamically
     *     created by a mod, and there is no way to provide static block-state resources that point at the correct renderer.
     * </p>
     *
     * @param blockState The {@link BlockState} that was not found in the loaded resources.
     * @return true if this renderer-type can render the provided {@link BlockState} despite missing resources.
     */
    default boolean isFallbackFor(BlockState blockState) {
        return false;
    }

    @RequiredArgsConstructor
    class Impl implements BlockRendererType {

        @Getter private final Key key;
        private final BlockRendererFactory rendererFactory;

        @Override
        public BlockRenderer create(ResourcePack resourcePack, TextureGallery textureGallery, RenderSettings renderSettings) {
            return rendererFactory.create(resourcePack, textureGallery, renderSettings);
        }

    }

}
