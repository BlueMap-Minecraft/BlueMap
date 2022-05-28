package de.bluecolored.bluemap.core.resources.resourcepack.blockstate;

import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import de.bluecolored.bluemap.core.debug.DebugDump;
import de.bluecolored.bluemap.core.resources.AbstractTypeAdapterFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("FieldMayBeFinal")
@DebugDump
@JsonAdapter(VariantSet.Adapter.class)
public class VariantSet {

    private BlockStateCondition condition;
    private List<Variant> variants;

    private transient double totalWeight;

    public VariantSet(List<Variant> variants) {
        this(BlockStateCondition.all(), variants);
    }

    public VariantSet(BlockStateCondition condition, List<Variant> variants) {
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

    public List<Variant> getVariants() {
        return variants;
    }

    private double summarizeWeights() {
        return variants.stream()
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
        final long hash = x * 73438747 ^ y * 9357269 ^ z * 4335792;
        return (hash * (hash + 456149) & 0x00ffffff) / (float) 0x01000000;
    }

    static class Adapter extends AbstractTypeAdapterFactory<VariantSet> {

        public Adapter() {
            super(VariantSet.class);
        }

        @Override
        public VariantSet read(JsonReader in, Gson gson) throws IOException {
            List<Variant> variants;
            if (in.peek() == JsonToken.BEGIN_ARRAY) {
                variants = gson.fromJson(in, new TypeToken<List<Variant>>(){}.getType());
            } else {
                variants = Collections.singletonList(gson.fromJson(in, Variant.class));
            }

            return new VariantSet(variants);
        }

    }

}
