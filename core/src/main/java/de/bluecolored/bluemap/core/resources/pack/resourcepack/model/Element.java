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
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.adapter.AbstractTypeAdapterFactory;
import de.bluecolored.bluemap.core.resources.adapter.PostDeserialize;
import de.bluecolored.bluemap.core.resources.pack.ResourcePool;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Direction;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal", "unused"})
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class Element {
    private static final Vector3f FULL_BLOCK_MIN = Vector3f.ZERO;
    private static final Vector3f FULL_BLOCK_MAX = new Vector3f(16, 16, 16);

    private Vector3f from = FULL_BLOCK_MIN, to = FULL_BLOCK_MAX;
    private Rotation rotation = Rotation.ZERO;
    private boolean shade = true;
    private int lightEmission = 0;
    private EnumMap<Direction, Face> faces = new EnumMap<>(Direction.class);

    public Element(Vector3f from, Vector3f to, Map<Direction, Face> faces) {
        this.from = from;
        this.to = to;
        this.faces.putAll(faces);
        init();
    }

    public Element(Vector3f from, Vector3f to, Rotation rotation, Map<Direction, Face> faces) {
        this.from = from;
        this.to = to;
        this.rotation = rotation;
        this.faces.putAll(faces);
        init();
    }

    public Element(Vector3f from, Vector3f to, Rotation rotation, boolean shade, int lightEmission, Map<Direction, Face> faces) {
        this.from = from;
        this.to = to;
        this.rotation = rotation;
        this.shade = shade;
        this.lightEmission = lightEmission;
        this.faces.putAll(faces);
        init();
    }

    private Element(Element copyFrom) {
        this.from = copyFrom.from;
        this.to = copyFrom.to;
        this.rotation = copyFrom.rotation;
        this.shade = copyFrom.shade;
        this.lightEmission = copyFrom.lightEmission;

        copyFrom.faces.forEach((direction, face) -> this.faces.put(direction, face.copy()));
    }

    @PostDeserialize
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
                    from.getX(), 16 - to.getY(),
                    to.getX(), 16 - from.getY()
            );
            case WEST, EAST -> new Vector4f(
                    from.getZ(), 16 - to.getY(),
                    to.getZ(), 16 - from.getY()
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

}
