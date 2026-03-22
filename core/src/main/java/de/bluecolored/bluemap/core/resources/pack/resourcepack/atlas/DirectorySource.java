/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.core.resources.pack.resourcepack.atlas;

import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.ResourcePool;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Key;
import lombok.*;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Predicate;

import static de.bluecolored.bluemap.core.resources.pack.Pack.list;
import static de.bluecolored.bluemap.core.resources.pack.Pack.walk;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("unused")
public class DirectorySource extends Source {

    private String source;
    private String prefix = "";

    public DirectorySource(String source) {
        this.source = source;
    }

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
    protected @Nullable Texture loadTexture(Key key, Path file) {
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
