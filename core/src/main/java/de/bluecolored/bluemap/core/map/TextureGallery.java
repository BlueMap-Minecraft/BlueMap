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
package de.bluecolored.bluemap.core.map;

import com.google.gson.*;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.adapter.ResourcesGson;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class TextureGallery {

    private static final Gson GSON = ResourcesGson.addAdapter(new GsonBuilder())
            .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
            .create();

    private final Map<ResourcePath<Texture>, TextureMapping> textureMappings;
    private int nextId;

    public TextureGallery() {
        this.textureMappings = new HashMap<>();
        this.nextId = 0;
    }

    public void clear() {
        this.textureMappings.clear();
        this.nextId = 0;
    }

    public int get(@Nullable ResourcePath<Texture> textureResourcePath) {
        if (textureResourcePath == null) textureResourcePath = ResourcePack.MISSING_TEXTURE;
        TextureMapping mapping = textureMappings.get(textureResourcePath);
        return mapping != null ? mapping.getId() : 0;
    }

    public synchronized void put(ResourcePath<Texture> textureResourcePath) {
        textureMappings.compute(textureResourcePath, (r, mapping) -> {
            if (mapping == null)
                return new TextureMapping(nextId++, textureResourcePath.getResource());

            Texture texture = textureResourcePath.getResource();
            if (texture != null) mapping.setTexture(texture);
            return mapping;
        });
    }

    public synchronized void put(ResourcePack resourcePack) {
        this.put(ResourcePack.MISSING_TEXTURE); // put this first
        resourcePack.getTextures().keySet()
                .stream()
                .sorted(Comparator
                        .comparing((ResourcePath<Texture> r) ->  {
                            Texture texture = r.getResource(resourcePack::getTexture);
                            return texture != null && texture.getColorPremultiplied().a < 1f;
                        })
                        .thenComparing(Key::getFormatted))
                .forEach(this::put);
    }

    public void writeTexturesFile(OutputStream out) throws IOException {
        Texture[] textures = new Texture[nextId];
        Arrays.fill(textures, Texture.MISSING);

        this.textureMappings.forEach((textureResourcePath, mapping) -> {
            int ordinal = mapping.getId();
            Texture texture = mapping.getTexture();
            if (texture == null) texture = Texture.missing(textureResourcePath);
            textures[ordinal] = texture;
        });

        try (Writer writer = new OutputStreamWriter(out)) {
            GSON.toJson(textures, Texture[].class, writer);
        } catch (JsonIOException ex) {
            throw new IOException(ex);
        }
    }

    public static TextureGallery readTexturesFile(InputStream in) throws IOException {
        TextureGallery gallery = new TextureGallery();
        try (Reader reader = new InputStreamReader(in)) {
            Texture[] textures = GSON.fromJson(reader, Texture[].class);
            if (textures == null) throw new IOException("Texture data is empty!");
            gallery.nextId = textures.length;
            for (int ordinal = 0; ordinal < textures.length; ordinal++) {
                Texture texture = textures[ordinal];
                if (texture != null) {
                    gallery.textureMappings.put(texture.getResourcePath(), new TextureMapping(ordinal, texture));
                }
            }
        } catch (JsonParseException ex) {
            throw new IOException(ex);
        }
        return gallery;
    }

    static class TextureMapping {
        private final int id;
        private @Nullable Texture texture;

        public TextureMapping(int id, @Nullable Texture texture) {
            this.id = id;
            this.texture = texture;
        }

        public int getId() {
            return id;
        }

        public @Nullable Texture getTexture() {
            return texture;
        }

        public void setTexture(@Nullable Texture texture) {
            this.texture = texture;
        }

    }

}
