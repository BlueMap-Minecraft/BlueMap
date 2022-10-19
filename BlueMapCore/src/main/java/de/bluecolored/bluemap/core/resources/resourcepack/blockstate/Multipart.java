package de.bluecolored.bluemap.core.resources.resourcepack.blockstate;

import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.core.resources.AbstractTypeAdapterFactory;
import de.bluecolored.bluemap.core.resources.adapter.ResourcesGson;
import de.bluecolored.bluemap.core.world.BlockState;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("FieldMayBeFinal")
@DebugDump
@JsonAdapter(Multipart.Adapter.class)
public class Multipart {

    private VariantSet[] parts = new VariantSet[0];

    private Multipart(){}

    public VariantSet[] getParts() {
        return parts;
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
                        case "when": condition = readCondition(in); break;
                        case "apply": variantSet = gson.fromJson(in, VariantSet.class); break;
                        default: in.skipValue(); break;
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
                if (name.equals(JSON_COMMENT)) {
                    in.skipValue();
                    continue;
                }

                if (name.equals("OR")) {
                    List<BlockStateCondition> orConditions = new ArrayList<>();
                    in.beginArray();
                    while (in.hasNext()) {
                        orConditions.add(readCondition(in));
                    }
                    in.endArray();
                    andConditions.add(
                            BlockStateCondition.or(orConditions.toArray(new BlockStateCondition[0])));
                } else if (name.equals("AND")) {
                    List<BlockStateCondition> andArray = new ArrayList<>();
                    in.beginArray();
                    while (in.hasNext()) {
                        andArray.add(readCondition(in));
                    }
                    in.endArray();
                    andConditions.add(
                            BlockStateCondition.and(andArray.toArray(new BlockStateCondition[0])));
                } else {
                    String[] values = StringUtils.split(ResourcesGson.nextStringOrBoolean(in), '|');
                    andConditions.add(BlockStateCondition.property(name, values));
                }
            }
            in.endObject();

            return BlockStateCondition.and(andConditions.toArray(new BlockStateCondition[0]));
        }

    }

}
