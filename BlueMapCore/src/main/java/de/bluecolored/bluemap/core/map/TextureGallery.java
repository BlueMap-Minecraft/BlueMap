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

import com.google.gson.JsonIOException;
import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.adapter.ResourcesGson;
import de.bluecolored.bluemap.core.resources.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.resourcepack.texture.Texture;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@DebugDump
public class TextureGallery {

    private final Map<ResourcePath<Texture>, Integer> ordinalMap;
    private int nextId;

    public TextureGallery() {
        this.ordinalMap = new HashMap<>();
        this.nextId = 0;
    }

    public void clear() {
        this.ordinalMap.clear();
        this.nextId = 0;
    }

    public int get(@Nullable ResourcePath<Texture> textureResourcePath) {
        if (textureResourcePath == null) textureResourcePath = ResourcePack.MISSING_TEXTURE;
        Integer ordinal = ordinalMap.get(textureResourcePath);
        return ordinal != null ? ordinal : 0;
    }

    public synchronized int put(ResourcePath<Texture> textureResourcePath) {
        Integer ordinal = ordinalMap.putIfAbsent(textureResourcePath, nextId);
        if (ordinal == null) return nextId++;
        return ordinal;
    }

    public synchronized void put(ResourcePack resourcePack) {
        resourcePack.getTextures().keySet().forEach(this::put);
    }

    public void writeTexturesFile(ResourcePack resourcePack, OutputStream out) throws IOException {
        Texture[] textures = new Texture[nextId];
        Arrays.fill(textures, Texture.MISSING);

        ordinalMap.forEach((textureResourcePath, ordinal) -> {
            Texture texture = textureResourcePath.getResource(resourcePack::getTexture);
            if (texture != null) textures[ordinal] = texture;

            // make sure the resource-path doesn't get lost
            if (textures[ordinal].getResourcePath().equals(ResourcePack.MISSING_TEXTURE))
                textures[ordinal] = Texture.missing(textureResourcePath);
        });

        try (Writer writer = new OutputStreamWriter(out)) {
            ResourcesGson.INSTANCE.toJson(textures, Texture[].class, writer);
        } catch (JsonIOException ex) {
            throw new IOException(ex);
        }
    }

    public static TextureGallery readTexturesFile(InputStream in) throws IOException {
        TextureGallery gallery = new TextureGallery();
        try (Reader reader = new InputStreamReader(in)) {
            Texture[] textures = ResourcesGson.INSTANCE.fromJson(reader, Texture[].class);
            if (textures == null) throw new IOException("Texture data is empty!");
            gallery.nextId = textures.length;
            for (int ordinal = 0; ordinal < textures.length; ordinal++) {
                Texture texture = textures[ordinal];
                if (texture != null) {
                    gallery.ordinalMap.put(textures[ordinal].getResourcePath(), ordinal);
                }
            }
        } catch (JsonIOException ex) {
            throw new IOException(ex);
        }
        return gallery;
    }

}
