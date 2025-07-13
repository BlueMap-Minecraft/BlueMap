package de.bluecolored.bluemap.core.resources.pack.resourcepack.atlas;

import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.ResourcePool;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Key;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Predicate;

@Getter
@SuppressWarnings("unused")
public class SingleSource extends Source {

    private ResourcePath<Texture> resource;
    private @Nullable ResourcePath<Texture> sprite;

    @Override
    public void load(Path root, ResourcePool<Texture> textures, Predicate<Key> textureFilter) throws IOException {
        if (resource == null) return;

        ResourcePath<Texture> sprite = getSprite();
        if (textures.contains(sprite)) return;
        if (!textureFilter.test(sprite)) return;

        Path file = getFile(root, resource);
        if (!Files.exists(file)) return;

        Texture texture = loadTexture(sprite, file);
        if (texture != null) textures.put(sprite, texture);
    }

    public ResourcePath<Texture> getSprite() {
        return sprite == null ? resource : sprite;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        if (!super.equals(object)) return false;

        SingleSource that = (SingleSource) object;
        return Objects.equals(resource, that.resource) && Objects.equals(sprite, that.sprite);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(resource);
        result = 31 * result + Objects.hashCode(sprite);
        return result;
    }

}
