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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.adapter.AbstractTypeAdapterFactory;
import de.bluecolored.bluemap.core.resources.adapter.ResourcesGson;
import de.bluecolored.bluemap.core.resources.pack.ResourcePool;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.AnimationMeta;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Key;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

@JsonAdapter(Source.Adapter.class)
@Getter
public class Source {

    private Key type;

    public void load(Path root, ResourcePool<Texture> textures, Predicate<Key> textureFilter) throws IOException {}

    public void bake(ResourcePool<Texture> textures, Predicate<Key> textureFilter) throws IOException {}

    protected @Nullable Texture loadTexture(Key key, Path file) throws IOException {
        BufferedImage image = loadImage(file);
        if (image == null) return null;
        AnimationMeta animation = loadAnimation(file);
        return Texture.from(key, image, animation);
    }

    protected @Nullable BufferedImage loadImage(Path imageFile) throws IOException {
        if (!Files.exists(imageFile)) return null;
        try (InputStream in = Files.newInputStream(imageFile)) {
            return ImageIO.read(in);
        }
    }

    protected @Nullable AnimationMeta loadAnimation(Path imageFile) throws IOException {
        Path animationPathFile = imageFile.resolveSibling(imageFile.getFileName() + ".mcmeta");
        if (!Files.exists(animationPathFile)) return null;
        try (Reader in = Files.newBufferedReader(animationPathFile, StandardCharsets.UTF_8)) {
            return ResourcesGson.INSTANCE.fromJson(in, AnimationMeta.class);
        }
    }

    protected Path getFile(Path root, Key key) {
        return root
                .resolve("assets")
                .resolve(key.getNamespace())
                .resolve("textures")
                .resolve(key.getValue().replace("/", root.getFileSystem().getSeparator()) + ".png");
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        if (getClass() != Source.class) return false;

        Source source = (Source) object;
        return type.equals(source.type);
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    static class Adapter extends AbstractTypeAdapterFactory<Source> {

        public Adapter() {
            super(Source.class);
        }

        @Override
        public Source read(JsonReader in, Gson gson) throws IOException {
            JsonElement element = gson.getAdapter(JsonElement.class).read(in);

            Source base = gson.getDelegateAdapter(this, TypeToken.get(Source.class)).fromJsonTree(element);
            SourceType type = SourceType.REGISTRY.get(base.getType());

            if (type == null) {
                Logger.global.logDebug("Unknown atlas-source type: " + base.getType());
                return base;
            }

            return gson.getDelegateAdapter(this, TypeToken.get(type.getType())).fromJsonTree(element);
        }

    }

}
