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
import de.bluecolored.bluemap.core.world.mca.entity.Llama;

public class LlamaRenderer extends ResourceModelRenderer {

    private final ResourcePath<Model>
            LAMA_CREAMY = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/llama_creamy"),
            LAMA_WHITE = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/llama_white"),
            LAMA_BROWN = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/llama_brown"),
            LAMA_GRAY = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/llama_gray"),
            LAMA_CHEST_CREAMY = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/llama_chest_creamy"),
            LAMA_CHEST_WHITE = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/llama_chest_white"),
            LAMA_CHEST_BROWN = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/llama_chest_brown"),
            LAMA_CHEST_GRAY = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/llama_chest_gray");

    public LlamaRenderer(ResourcePack resourcePack, TextureGallery textureGallery, RenderSettings renderSettings) {
        super(resourcePack, textureGallery, renderSettings);
    }

    @Override
    public void render(Entity entity, BlockNeighborhood block, Part part, TileModelView tileModel) {
        if (!(entity instanceof Llama llama)) return;

        // choose correct model
        ResourcePath<Model> model;
        if (llama.isWithChest()) {
            model = switch (llama.getVariant()) {
                case CREAMY -> LAMA_CHEST_CREAMY;
                case WHITE -> LAMA_CHEST_WHITE;
                case BROWN -> LAMA_CHEST_BROWN;
                case GRAY -> LAMA_CHEST_GRAY;
            };
        } else {
            model = switch (llama.getVariant()) {
                case CREAMY -> LAMA_CREAMY;
                case WHITE -> LAMA_WHITE;
                case BROWN -> LAMA_BROWN;
                case GRAY -> LAMA_GRAY;
            };
        }

        // render chosen model
        super.render(entity, block, model.getResource(resourcePack::getModel), TintColorProvider.NO_TINT, tileModel);

        // apply part transform
        if (part.isTransformed())
            tileModel.transform(part.getTransformMatrix());
    }

}
