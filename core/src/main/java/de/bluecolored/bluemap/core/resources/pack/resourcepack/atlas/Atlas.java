package de.bluecolored.bluemap.core.resources.pack.resourcepack.atlas;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resources.pack.ResourcePool;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Key;
import lombok.Getter;
import org.jetbrains.annotations.Contract;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.function.Predicate;

@Getter
@SuppressWarnings("FieldMayBeFinal")
public class Atlas {

    private LinkedHashSet<Source> sources = new LinkedHashSet<>();

    @Contract("_ -> this")
    public Atlas add(Atlas atlas) {
        sources.addAll(atlas.getSources());
        return this;
    }

    public void load(Path root, ResourcePool<Texture> textures, Predicate<Key> textureFilter) throws IOException {
        sources.forEach(source -> {
            try {
                source.load(root, textures, textureFilter);
            } catch (IOException e) {
                Logger.global.logDebug("Failed to load atlas-source: " + e);
            }
        });
    }

    public void bake(ResourcePool<Texture> textures, Predicate<Key> textureFilter) throws IOException {
        sources.forEach(source -> {
            try {
                source.bake(textures, textureFilter);
            } catch (IOException e) {
                Logger.global.logDebug("Failed to bake atlas-source: " + e);
            }
        });
    }

}
