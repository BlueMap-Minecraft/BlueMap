package de.bluecolored.bluemap.core.map.hires.blockmodel;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;

public class MissingModelRenderer implements BlockRenderer {

    private static final LoadingCache<BlockState, BlockRendererType> BLOCK_RENDERER_TYPES = Caffeine.newBuilder()
            .maximumSize(1000)
            .build(blockState -> {
                for (BlockRendererType type : BlockRendererType.REGISTRY.values())
                    if (type.isFallbackFor(blockState)) return type;
                return BlockRendererType.DEFAULT;
            });

    private final LoadingCache<BlockRendererType, BlockRenderer> blockRenderers;

    public MissingModelRenderer(ResourcePack resourcePack, TextureGallery textureGallery, RenderSettings renderSettings) {
        this.blockRenderers = Caffeine.newBuilder()
                .build(type -> type.create(resourcePack, textureGallery, renderSettings));
    }

    @Override
    public void render(BlockNeighborhood<?> block, Variant variant, TileModelView blockModel, Color blockColor) {
        blockRenderers.get(BLOCK_RENDERER_TYPES.get(block.getBlockState()))
                .render(block, variant, blockModel, blockColor);
    }

}
