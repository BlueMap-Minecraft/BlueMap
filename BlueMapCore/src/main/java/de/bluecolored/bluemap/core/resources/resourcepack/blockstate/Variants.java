package de.bluecolored.bluemap.core.resources.resourcepack.blockstate;

import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import de.bluecolored.bluemap.api.debug.DebugDump;
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
@DebugDump
@JsonAdapter(Variants.Adapter.class)
public class Variants {

    private List<VariantSet> variants = new ArrayList<>();
    private VariantSet defaultVariant;

    private Variants(){}

    public List<VariantSet> getVariants() {
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
            Variants result = new Variants();

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
                    result.defaultVariant = variantSet;
                } else if (variantSet.getCondition() != BlockStateCondition.none()) {
                    result.variants.add(variantSet);
                }
            }
            in.endObject();

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
