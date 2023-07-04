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
package de.bluecolored.bluemap.core.resources.resourcepack.blockmodel;

import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@DebugDump
public class BlockModel {

    private ResourcePath<BlockModel> parent;
    private Map<String, TextureVariable> textures = new HashMap<>();
    private Element[] elements;
    private boolean ambientocclusion = true;

    private transient boolean liquid = false;
    private transient boolean culling = false;
    private transient boolean occluding = false;

    private BlockModel(){}

    @Nullable
    public ResourcePath<BlockModel> getParent() {
        return parent;
    }

    public Map<String, TextureVariable> getTextures() {
        return textures;
    }

    @Nullable
    public Element[] getElements() {
        return elements;
    }

    public boolean isAmbientocclusion() {
        return ambientocclusion;
    }

    public boolean isLiquid() {
        return liquid;
    }

    public boolean isCulling() {
        return culling;
    }

    public boolean isOccluding() {
        return occluding;
    }

    public synchronized void optimize(ResourcePack resourcePack) {
        for (var variable : this.textures.values()) {
            variable.optimize(resourcePack);
        }

        if (this.elements != null) {
            for (var element : elements) {
                if (element != null) element.optimize(resourcePack);
            }
        }
    }

    public synchronized void applyParent(ResourcePack resourcePack) {
        if (this.parent == null) return;

        // set parent to null early to avoid trying to resolve reference-loops
        ResourcePath<BlockModel> parentPath = this.parent;
        this.parent = null;

        if (parentPath.getFormatted().equals("bluemap:builtin/liquid")) {
            this.liquid = true;
            return;
        }

        BlockModel parent = parentPath.getResource(resourcePack::getBlockModel);
        if (parent != null) {
            parent.applyParent(resourcePack);

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

    public synchronized void calculateProperties(ResourcePack resourcePack) {
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

                    Texture texture = textureResourcePath.getResource(resourcePack::getTexture);
                    if (texture == null || texture.getColorStraight().a < 1) {
                        culling = false;
                        break;
                    }
                }

                break;
            }
        }
    }

}
