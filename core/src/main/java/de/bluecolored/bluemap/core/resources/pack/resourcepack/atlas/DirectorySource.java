package de.bluecolored.bluemap.core.resources.pack.resourcepack.atlas;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.ResourcePool;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Key;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Predicate;

import static de.bluecolored.bluemap.core.resources.pack.Pack.list;
import static de.bluecolored.bluemap.core.resources.pack.Pack.walk;

@Getter
@SuppressWarnings("unused")
public class DirectorySource extends Source {

    private String source;
    private String prefix = "";

    @Override
    public void load(Path root, ResourcePool<Texture> textures, Predicate<Key> textureFilter) throws IOException {
        if (this.source == null) return;

        String source = this.source.replace("/", root.getFileSystem().getSeparator());

        list(root.resolve("assets"))
                .forEach(namespacePath -> {
                    String namespace = namespacePath.getFileName().toString();
                    Path sourcePath = namespacePath
                            .resolve("textures")
                            .resolve(source);
                    walk(sourcePath)
                            .filter(path -> path.getFileName().toString().endsWith(".png"))
                            .filter(Files::isRegularFile)
                            .forEach(file -> {
                                Path namePath = sourcePath.relativize(file);
                                String name = prefix + namePath.toString().replace(root.getFileSystem().getSeparator(), "/");
                                // remove .png
                                name = name.substring(0, name.length() - 4);
                                ResourcePath<Texture> resourcePath = new ResourcePath<>(namespace, name);
                                if (textureFilter.test(resourcePath))
                                    textures.load(resourcePath, rp -> loadTexture(resourcePath, file));
                            });
                });
    }

    @Override
    @SneakyThrows
    protected @Nullable Texture loadTexture(ResourcePath<Texture> key, Path file) {
        return super.loadTexture(key, file);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        if (!super.equals(object)) return false;

        DirectorySource that = (DirectorySource) object;
        return Objects.equals(source, that.source) && Objects.equals(prefix, that.prefix);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(source);
        result = 31 * result + Objects.hashCode(prefix);
        return result;
    }

}
