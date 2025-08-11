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
package de.bluecolored.bluemap.core.resources.pack.resourcepack.model;

import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.ResourcePool;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Direction;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal", "unused"})
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class Model {

    private @Nullable ResourcePath<Model> parent;
    private Map<String, TextureVariable> textures = new HashMap<>();
    private Element @Nullable [] elements;
    @Getter(AccessLevel.NONE) private Boolean ambientocclusion;

    private transient boolean culling = false;
    private transient boolean occluding = false;

    public Model(Map<String, TextureVariable> textures) {
        this.textures.putAll(textures);
    }

    public Model(Element @Nullable ... elements) {
        this.elements = elements;
    }

    public Model(Map<String, TextureVariable> textures, Element @Nullable ... elements) {
        this.textures.putAll(textures);
        this.elements = elements;
    }

    public Model(Map<String, TextureVariable> textures, Element @Nullable [] elements, boolean ambientocclusion) {
        this.textures.putAll(textures);
        this.elements = elements;
    }

    public Model(@Nullable ResourcePath<Model> parent, Map<String, TextureVariable> textures) {
        this.parent = parent;
        this.textures.putAll(textures);
    }

    public Model(@Nullable ResourcePath<Model> parent, Map<String, TextureVariable> textures, Element @Nullable [] elements, boolean ambientocclusion) {
        this.parent = parent;
        this.textures.putAll(textures);
        this.elements = elements;
    }

    public synchronized void optimize(ResourcePool<Texture> texturePool) {
        for (var variable : this.textures.values()) {
            variable.optimize(texturePool);
        }

        if (this.elements != null) {
            for (var element : elements) {
                if (element != null) element.optimize(texturePool);
            }
        }
    }

    public synchronized void applyParent(ResourcePool<Model> modelPool) {
        if (this.parent == null) return;

        // set parent to null early to avoid trying to resolve reference-loops
        ResourcePath<Model> parentPath = this.parent;
        this.parent = null;

        Model parent = parentPath.getResource(modelPool::get);
        if (parent != null) {
            parent.applyParent(modelPool);

            if (this.ambientocclusion == null && parent.ambientocclusion != null) {
                this.ambientocclusion = parent.ambientocclusion;
            }

            parent.textures.forEach(this::applyTextureVariable);
            if (this.elements == null && parent.elements != null) {
                this.elements = new Element[parent.elements.length];
                for (int i = 0; i < this.elements.length; i++){
                    if (parent.elements[i] == null) continue;
                    this.elements[i] = parent.elements[i].copy();
                }
            }
        }
    }

    private synchronized void applyTextureVariable(String key, TextureVariable value) {
        if (!this.textures.containsKey(key)) {
            this.textures.put(key, value.copy());
        }
    }

    public synchronized void calculateProperties(ResourcePool<Texture> texturePool) {
        if (elements == null) return;
        for (Element element : elements) {
            if (element != null && element.isFullCube()) {
                occluding = true;

                culling = true;
                for (Direction dir : Direction.values()) {
                    Face face = element.getFaces().get(dir);
                    if (face == null) {
                        culling = false;
                        break;
                    }

                    ResourcePath<Texture> textureResourcePath = face.getTexture().getTexturePath(textures::get);
                    if (textureResourcePath == null) {
                        culling = false;
                        break;
                    }

                    Texture texture = textureResourcePath.getResource(texturePool::get);
                    if (texture == null || texture.getColorStraight().a < 1) {
                        culling = false;
                        break;
                    }
                }

                break;
            }
        }
    }

    public boolean isAmbientocclusion() {
        if (ambientocclusion == null) return true;
        return ambientocclusion;
    }

}
