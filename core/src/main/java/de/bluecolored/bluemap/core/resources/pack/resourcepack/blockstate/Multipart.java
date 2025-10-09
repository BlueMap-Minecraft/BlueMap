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
import de.bluecolored.bluemap.core.resources.adapter.AbstractTypeAdapterFactory;
import de.bluecolored.bluemap.core.world.BlockState;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("FieldMayBeFinal")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@JsonAdapter(Multipart.Adapter.class)
public class Multipart {

    @Getter
    private VariantSet[] parts = new VariantSet[0];

    public void forEach(Consumer<Variant> consumer) {
        for (VariantSet part : parts) {
            part.forEach(consumer);
        }
    }

    public void forEach(BlockState blockState, int x, int y, int z, Consumer<Variant> consumer) {
        for (VariantSet part : parts) {
            if (part.getCondition().matches(blockState)) {
                part.forEach(x, y, z, consumer);
            }
        }
    }

    static class Adapter extends AbstractTypeAdapterFactory<Multipart> {

        public Adapter() {
            super(Multipart.class);
        }

        @Override
        public Multipart read(JsonReader in, Gson gson) throws IOException {
            List<VariantSet> parts = new ArrayList<>();

            in.beginArray();
            while (in.hasNext()) {
                VariantSet variantSet = null;
                BlockStateCondition condition = null;

                in.beginObject();
                while (in.hasNext()) {
                    String key = in.nextName();
                    switch (key) {
                        case "when" -> condition = readCondition(in);
                        case "apply" -> variantSet = gson.fromJson(in, VariantSet.class);
                        default -> in.skipValue();
                    }
                }
                in.endObject();

                if (variantSet == null) continue;
                if (condition != null) variantSet.setCondition(condition);
                parts.add(variantSet);
            }
            in.endArray();

            Multipart result = new Multipart();
            result.parts = parts.toArray(VariantSet[]::new);
            return result;
        }

        public BlockStateCondition readCondition(JsonReader in) throws IOException {
            List<BlockStateCondition> andConditions = new ArrayList<>();
            in.beginObject();
            while (in.hasNext()) {
                String name = in.nextName();
                switch (name) {
                    case JSON_COMMENT -> in.skipValue();
                    case "OR" -> {
                        List<BlockStateCondition> orConditions = new ArrayList<>();
                        in.beginArray();
                        while (in.hasNext()) {
                            orConditions.add(readCondition(in));
                        }
                        in.endArray();
                        andConditions.add(
                                BlockStateCondition.or(orConditions.toArray(new BlockStateCondition[0])));
                    }
                    case "AND" -> {
                        List<BlockStateCondition> andArray = new ArrayList<>();
                        in.beginArray();
                        while (in.hasNext()) {
                            andArray.add(readCondition(in));
                        }
                        in.endArray();
                        andConditions.add(
                                BlockStateCondition.and(andArray.toArray(new BlockStateCondition[0])));
                    }
                    default -> {
                        String[] values = nextStringOrBoolean(in).split("\\|");
                        andConditions.add(BlockStateCondition.property(name, values));
                    }
                }
            }
            in.endObject();

            return BlockStateCondition.and(andConditions.toArray(new BlockStateCondition[0]));
        }

        private String nextStringOrBoolean(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.BOOLEAN) return Boolean.toString(in.nextBoolean());
            return in.nextString();
        }

    }

}
