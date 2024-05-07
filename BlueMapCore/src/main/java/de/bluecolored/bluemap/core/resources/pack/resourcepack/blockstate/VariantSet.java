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
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.core.resources.AbstractTypeAdapterFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

@SuppressWarnings("FieldMayBeFinal")
@DebugDump
@JsonAdapter(VariantSet.Adapter.class)
public class VariantSet {

    private BlockStateCondition condition;
    private Variant[] variants;

    private transient double totalWeight;

    public VariantSet(Variant... variants) {
        this(BlockStateCondition.all(), variants);
    }

    public VariantSet(BlockStateCondition condition, Variant... variants) {
        this.condition = condition;
        this.variants = variants;

        this.totalWeight = summarizeWeights();
    }

    public BlockStateCondition getCondition() {
        return condition;
    }

    public void setCondition(BlockStateCondition condition) {
        this.condition = condition;
    }

    public Variant[] getVariants() {
        return variants;
    }

    private double summarizeWeights() {
        return Arrays.stream(variants)
                .mapToDouble(Variant::getWeight)
                .sum();
    }

    public void forEach(int x, int y, int z, Consumer<Variant> consumer) {
        double selection = hashToFloat(x, y, z) * totalWeight; // random based on position
        for (Variant variant : variants) {
            selection -= variant.getWeight();
            if (selection <= 0) {
                consumer.accept(variant);
                return;
            }
        }
    }

    private static float hashToFloat(int x, int y, int z) {
        final long hash = x * 73438747L ^ y * 9357269L ^ z * 4335792L;
        return (hash * (hash + 456149) & 0x00ffffff) / (float) 0x01000000;
    }

    static class Adapter extends AbstractTypeAdapterFactory<VariantSet> {

        public Adapter() {
            super(VariantSet.class);
        }

        @Override
        public VariantSet read(JsonReader in, Gson gson) throws IOException {
            Variant[] variants;
            if (in.peek() == JsonToken.BEGIN_ARRAY) {
                variants = gson.fromJson(in, Variant[].class);
            } else {
                variants = new Variant[]{ gson.fromJson(in, Variant.class) };
            }

            return new VariantSet(variants);
        }

    }

}
