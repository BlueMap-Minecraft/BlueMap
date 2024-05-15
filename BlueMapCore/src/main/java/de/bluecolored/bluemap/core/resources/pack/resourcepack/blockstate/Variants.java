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
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resources.AbstractTypeAdapterFactory;
import de.bluecolored.bluemap.core.world.BlockState;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("FieldMayBeFinal")
@JsonAdapter(Variants.Adapter.class)
public class Variants {

    private VariantSet[] variants = new VariantSet[0];
    private VariantSet defaultVariant;

    private Variants(){}

    public VariantSet[] getVariants() {
        return variants;
    }

    @Nullable
    public VariantSet getDefaultVariant() {
        return defaultVariant;
    }

    public void forEach(BlockState blockState, int x, int y, int z, Consumer<Variant> consumer) {
        for (VariantSet variant : variants){
            if (variant.getCondition().matches(blockState)){
                variant.forEach(x, y, z, consumer);
                return;
            }
        }

        // still here? do default
        if (defaultVariant != null) {
            defaultVariant.forEach(x, y, z, consumer);
        }
    }

    static class Adapter extends AbstractTypeAdapterFactory<Variants> {

        public Adapter() {
            super(Variants.class);
        }

        @Override
        public Variants read(JsonReader in, Gson gson) throws IOException {
            VariantSet defaultVariant = null;
            List<VariantSet> variants = new ArrayList<>();

            in.beginObject();
            while (in.hasNext()) {
                String name = in.nextName();
                if (name.equals(JSON_COMMENT)) {
                    in.skipValue();
                    continue;
                }

                BlockStateCondition condition = parseConditionString(name);
                VariantSet variantSet = gson.fromJson(in, VariantSet.class);
                variantSet.setCondition(condition);

                if (variantSet.getCondition() == BlockStateCondition.all()) {
                    defaultVariant = variantSet;
                } else if (variantSet.getCondition() != BlockStateCondition.none()) {
                    variants.add(variantSet);
                }
            }
            in.endObject();

            Variants result = new Variants();
            result.defaultVariant = defaultVariant;
            result.variants = variants.toArray(VariantSet[]::new);
            return result;
        }

        private BlockStateCondition parseConditionString(String conditionString) {
            List<BlockStateCondition> conditions = new ArrayList<>();
            boolean invalid = false;
            if (!conditionString.isEmpty() && !conditionString.equals("default") && !conditionString.equals("normal")) {
                String[] conditionSplit = StringUtils.split(conditionString, ',');
                for (String element : conditionSplit) {
                    String[] keyval = StringUtils.split(element, "=", 2);
                    if (keyval.length < 2) {
                        Logger.global.logDebug("Failed to parse condition: Condition-String '" + conditionString + "' is invalid!");
                        invalid = true;
                        continue;
                    }
                    conditions.add(BlockStateCondition.property(keyval[0], keyval[1]));
                }
            }

            BlockStateCondition condition;
            if (conditions.isEmpty()) {
                condition = invalid ? BlockStateCondition.none() : BlockStateCondition.all();
            } else if (conditions.size() == 1) {
                condition = conditions.get(0);
            } else {
                condition = BlockStateCondition.and(conditions.toArray(new BlockStateCondition[0]));
            }

            return condition;
        }

    }

}
