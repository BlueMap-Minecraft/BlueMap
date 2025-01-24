package de.bluecolored.bluemap.core.map.hires.entity;

import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.entitystate.Part;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.Entity;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;
import de.bluecolored.bluemap.core.world.mca.entity.Fox;

public class FoxRenderer extends ResourceModelRenderer {

    private final ResourcePath<Model>
        FOX_ADULT_RED = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/fox/adult"),
        FOX_ADULT_SNOW = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/fox/adult_snow");

    public FoxRenderer(ResourcePack resourcePack, TextureGallery textureGallery, RenderSettings renderSettings) {
        super(resourcePack, textureGallery, renderSettings);
    }

    @Override
    public void render(Entity entity, BlockNeighborhood block, Part part, TileModelView tileModel) {
        if (!(entity instanceof Fox fox)) return;

        // choose correct model
        ResourcePath<Model> model;
        model = switch (fox.getType()) {
            case RED -> FOX_ADULT_RED;
            case SNOW -> FOX_ADULT_SNOW;
        };

        // render chosen model
        super.render(entity, block, model.getResource(resourcePack::getModel), TintColorProvider.NO_TINT, tileModel);

        // apply part transform
        if (part.isTransformed())
            tileModel.transform(part.getTransformMatrix());
    }

}
