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
import de.bluecolored.bluemap.core.world.mca.entity.Bee;
import de.bluecolored.bluemap.core.world.mca.entity.Cat;

public class CatRenderer extends ResourceModelRenderer {

    private final ResourcePath<Model>
        CAT_BLACK = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/cat/cat_black"),
        CAT_ALL_BLACK = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/cat/cat_all_black"),
        CAT_BRITISH_SHORTHAIR = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/cat/cat_british_shorthair"),
        CAT_CALICO = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/cat/cat_calico"),
        CAT_JELLIE = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/cat/cat_jellie"),
        CAT_WHITE = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/cat/cat_white"),
        CAT_PERSIAN = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/cat/cat_persian"),
        CAT_RAGDOLL = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/cat/cat_ragdoll"),
        CAT_RED = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/cat/cat_red"),
        CAT_SIAMESE = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/cat/cat_siamese"),
        CAT_TABBY = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/cat/cat_tabby");

    public CatRenderer(ResourcePack resourcePack, TextureGallery textureGallery, RenderSettings renderSettings) {
        super(resourcePack, textureGallery, renderSettings);
    }

    @Override
    public void render(Entity entity, BlockNeighborhood block, Part part, TileModelView tileModel) {
        if (!(entity instanceof Cat cat)) return;

        // choose correct model
        ResourcePath<Model> model;
        model = switch (cat.getVariant()) {
            case BLACK -> CAT_BLACK;
            case ALL_BLACK -> CAT_ALL_BLACK;
            case BRITISH_SHORTHAIR -> CAT_BRITISH_SHORTHAIR;
            case CALICO -> CAT_CALICO;
            case JELLIE -> CAT_JELLIE;
            case WHITE -> CAT_WHITE;
            case PERSIAN -> CAT_PERSIAN;
            case RAGDOLL -> CAT_RAGDOLL;
            case RED -> CAT_RED;
            case SIAMESE -> CAT_SIAMESE;
            case TABBY -> CAT_TABBY;
        };

        // render chosen model
        super.render(entity, block, model.getResource(resourcePack::getModel), TintColorProvider.NO_TINT, tileModel);

        // apply part transform
        if (part.isTransformed())
            tileModel.transform(part.getTransformMatrix());
    }

}
