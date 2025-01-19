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
package de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate;

import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import de.bluecolored.bluemap.core.map.hires.block.BlockRendererType;
import de.bluecolored.bluemap.core.resources.AbstractTypeAdapterFactory;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.util.math.MatrixM3f;
import de.bluecolored.bluemap.core.util.math.MatrixM4f;
import lombok.Getter;

import java.io.IOException;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@JsonAdapter(Variant.Adapter.class)
@Getter
public class Variant {

    private BlockRendererType renderer = BlockRendererType.DEFAULT;
    private ResourcePath<Model> model = ResourcePack.MISSING_BLOCK_MODEL;
    private float x = 0, y = 0;
    private boolean uvlock = false;
    private double weight = 1;

    private transient boolean transformed;
    private transient MatrixM4f transformMatrix;

    private Variant(){}

    private void init() {
        this.transformed = x != 0 || y != 0;
        this.transformMatrix = new MatrixM4f()
                .translate(-0.5f, -0.5f, -0.5f)
                .rotate(-x, -y, 0)
                .translate(0.5f, 0.5f, 0.5f);
    }

    static class Adapter extends AbstractTypeAdapterFactory<Variant> {

        public Adapter() {
            super(Variant.class);
        }

        @Override
        public Variant read(JsonReader in, Gson gson) throws IOException {
            Variant variant = gson.getDelegateAdapter(this, TypeToken.get(Variant.class)).read(in);
            variant.init();
            return variant;
        }

    }

}
