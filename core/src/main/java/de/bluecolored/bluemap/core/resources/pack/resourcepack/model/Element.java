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

import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector4f;
import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import de.bluecolored.bluemap.core.resources.adapter.AbstractTypeAdapterFactory;
import de.bluecolored.bluemap.core.resources.pack.ResourcePool;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Direction;
import lombok.Getter;

import java.io.IOException;
import java.util.EnumMap;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@JsonAdapter(Element.Adapter.class)
@Getter
public class Element {
    private static final Vector3f FULL_BLOCK_MIN = Vector3f.ZERO;
    private static final Vector3f FULL_BLOCK_MAX = new Vector3f(16, 16, 16);

    private Vector3f from = FULL_BLOCK_MIN, to = FULL_BLOCK_MAX;
    private Rotation rotation = Rotation.ZERO;
    private boolean shade = true;
    private int lightEmission = 0;
    private EnumMap<Direction, Face> faces = new EnumMap<>(Direction.class);

    @SuppressWarnings("unused")
    private Element() {}

    private Element(Element copyFrom) {
        this.from = copyFrom.from;
        this.to = copyFrom.to;
        this.rotation = copyFrom.rotation;
        this.shade = copyFrom.shade;
        this.lightEmission = copyFrom.lightEmission;

        copyFrom.faces.forEach((direction, face) -> this.faces.put(direction, face.copy()));
    }

    private void init() {
        faces.forEach((direction, face) -> face.init(direction, this::calculateDefaultUV));
    }

    private Vector4f calculateDefaultUV(Direction face) {
        return switch (face) {
            case DOWN, UP -> new Vector4f(
                    from.getX(), from.getZ(),
                    to.getX(), to.getZ()
            );
            case NORTH, SOUTH -> new Vector4f(
                    from.getX(), from.getY(),
                    to.getX(), to.getY()
            );
            case WEST, EAST -> new Vector4f(
                    from.getZ(), from.getY(),
                    to.getZ(), to.getY()
            );
        };
    }

    public Element copy() {
        return new Element(this);
    }

    boolean isFullCube() {
        if (!(FULL_BLOCK_MIN.equals(from) && FULL_BLOCK_MAX.equals(to))) return false;
        for (Direction dir : Direction.values()) {
            if (!faces.containsKey(dir)) return false;
        }
        return true;
    }

    public void optimize(ResourcePool<Texture> texturePool) {
        for (var face : faces.values())  {
            face.optimize(texturePool);
        }
    }

    static class Adapter extends AbstractTypeAdapterFactory<Element> {

        public Adapter() {
            super(Element.class);
        }

        @Override
        public Element read(JsonReader in, Gson gson) throws IOException {
            Element element = gson.getDelegateAdapter(this, TypeToken.get(Element.class)).read(in);
            element.init();
            return element;
        }

    }

}
