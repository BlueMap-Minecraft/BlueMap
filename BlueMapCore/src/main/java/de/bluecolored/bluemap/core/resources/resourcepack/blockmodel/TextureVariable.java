package de.bluecolored.bluemap.core.resources.resourcepack.blockmodel;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.bluecolored.bluemap.core.debug.DebugDump;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.resourcepack.texture.Texture;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

@DebugDump
@JsonAdapter(TextureVariable.Adapter.class)
public class TextureVariable {

    private String referenceName;
    private ResourcePath<Texture> texturePath;

    private transient volatile boolean isReference, isResolving;

    private TextureVariable(TextureVariable copyFrom) {
        this.referenceName = copyFrom.referenceName;
        this.texturePath = copyFrom.texturePath;
        this.isReference = copyFrom.isReference;
        this.isResolving = copyFrom.isResolving;
    }

    public TextureVariable(String referenceName) {
        this.referenceName = Objects.requireNonNull(referenceName);
        this.texturePath = null;
        this.isReference = true;
        this.isResolving = false;
    }

    public TextureVariable(ResourcePath<Texture> texturePath) {
        this.referenceName = null;
        this.texturePath = Objects.requireNonNull(texturePath);
        this.isReference = false;
        this.isResolving = false;
    }

    @Nullable
    public String getReferenceName() {
        return referenceName;
    }

    public void setReferenceName(String referenceName) {
        this.referenceName = referenceName;
    }

    @Nullable
    public ResourcePath<Texture> getTexturePath() {
        return texturePath;
    }

    @Nullable
    public ResourcePath<Texture> getTexturePath(Function<String, TextureVariable> supplier) {
        if (this.isReference) return resolveTexturePath(supplier);
        return this.texturePath;
    }

    @Nullable
    private ResourcePath<Texture> resolveTexturePath(Function<String, TextureVariable> supplier) {
        synchronized (TextureVariable.class) {
            if (this.isReference && !this.isResolving) {
                this.isResolving = true; // set to avoid trying to resolve reference-loops

                // resolve
                TextureVariable referenced = supplier.apply(this.referenceName);
                if (referenced != null) {
                    this.texturePath = referenced.getTexturePath(supplier);
                }

                this.isReference = false;
                this.isResolving = false;
            }
            return this.texturePath;
        }
    }

    public void setTexturePath(ResourcePath<Texture> texturePath) {
        this.texturePath = texturePath;
    }

    public boolean isReference() {
        return isReference;
    }

    public TextureVariable copy() {
        synchronized (TextureVariable.class) {
            return new TextureVariable(this);
        }
    }

    public void optimize(ResourcePack resourcePack) {
        synchronized (TextureVariable.class) {
            if (texturePath != null) {
                texturePath = resourcePack.getTexturePath(texturePath.getFormatted());
            }
        }
    }

    static class Adapter extends TypeAdapter<TextureVariable> {

        @Override
        public void write(JsonWriter out, TextureVariable value) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public TextureVariable read(JsonReader in) throws IOException {
            String value = in.nextString();
            if (value.isEmpty()) throw new IOException("Can't parse an empty String into a TextureVariable");
            if (value.charAt(0) == '#') {
                return new TextureVariable(value.substring(1));
            } else {
                return new TextureVariable(new ResourcePath<>(value));
            }
        }

    }

}
