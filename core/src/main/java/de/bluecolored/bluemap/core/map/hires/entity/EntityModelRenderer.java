package de.bluecolored.bluemap.core.map.hires.entity;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.entitystate.EntityState;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.entitystate.Part;
import de.bluecolored.bluemap.core.world.Entity;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;

public class EntityModelRenderer {

    private final ResourcePack resourcePack;
    private final LoadingCache<EntityRendererType, EntityRenderer> entityRenderers;

    public EntityModelRenderer(ResourcePack resourcePack, TextureGallery textureGallery, RenderSettings renderSettings) {
        this.resourcePack = resourcePack;
        this.entityRenderers = Caffeine.newBuilder()
                .build(type -> type.create(resourcePack, textureGallery, renderSettings));
    }

    public void render(Entity entity, BlockNeighborhood block, TileModelView tileModel) {
        int modelStart = tileModel.getStart();

        EntityState stateResource = resourcePack.getEntityState(entity.getId());
        if (stateResource == null) return;

        Part[] parts = stateResource.getParts();

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < parts.length; i++) {
            Part part = parts[i];
            entityRenderers.get(part.getRenderer())
                    .render(entity, block, part, tileModel.initialize());
        }

        tileModel.initialize(modelStart);
    }

}
