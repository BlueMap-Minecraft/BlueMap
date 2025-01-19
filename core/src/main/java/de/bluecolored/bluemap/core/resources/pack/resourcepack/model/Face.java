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

import com.flowpowered.math.vector.Vector4f;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.util.Direction;
import lombok.Getter;

import java.util.function.Function;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@Getter
public class Face {

    private static final TextureVariable DEFAULT_TEXTURE = new TextureVariable(ResourcePack.MISSING_TEXTURE);

    private Vector4f uv;
    private TextureVariable texture = DEFAULT_TEXTURE;
    private Direction cullface;
    private int rotation = 0;
    private int tintindex = -1;

    @SuppressWarnings("unused")
    private Face() {}

    private Face(Face copyFrom) {
        this.uv = copyFrom.uv;
        this.texture = copyFrom.texture.copy();
        this.cullface = copyFrom.cullface;
        this.rotation = copyFrom.rotation;
        this.tintindex = copyFrom.tintindex;
    }

    void init(Direction direction, Function<Direction, Vector4f> defaultUvCalculator, Function<Direction, Direction> defaultCullfaceCalculator) {
        if (cullface == null) cullface = defaultCullfaceCalculator.apply(direction);
        if (uv == null) uv = defaultUvCalculator.apply(direction);
    }

    public Face copy() {
        return new Face(this);
    }

    public void optimize(ResourcePack resourcePack) {
        this.texture.optimize(resourcePack);
    }
}
