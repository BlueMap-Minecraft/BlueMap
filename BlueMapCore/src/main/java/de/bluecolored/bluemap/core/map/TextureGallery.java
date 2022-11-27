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
