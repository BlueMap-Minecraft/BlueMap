package de.bluecolored.bluemap.core.world.mca.data;

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluenbt.NBTReader;
import de.bluecolored.bluenbt.TagType;
import de.bluecolored.bluenbt.TypeDeserializer;

import java.io.IOException;

public class Vector2iDeserializer implements TypeDeserializer<Vector2i> {

    @Override
    public Vector2i read(NBTReader reader) throws IOException {
        TagType tag = reader.peek();

        return switch (tag) {

            case INT_ARRAY, LONG_ARRAY, BYTE_ARRAY -> {
                long[] values = reader.nextArrayAsLongArray();
                if (values.length != 2) throw new IllegalStateException("Unexpected array length: " + values.length);
                yield new Vector2i(
                        values[0],
                        values[1]
                );
            }

            case LIST -> {
                reader.beginList();
                Vector2i value = new Vector2i(
                        reader.nextDouble(),
                        reader.nextDouble()
                );
                reader.endList();
                yield value;
            }

            case COMPOUND -> {
                double x = 0, y = 0;
                reader.beginCompound();
                while (reader.peek() != TagType.END) {
                    switch (reader.name()) {
                        case "x": x = reader.nextDouble(); break;
                        case "y", "z": y = reader.nextDouble(); break;
                        default: reader.skip();
                    }
                }
                reader.endCompound();
                yield new Vector2i(x, y);
            }

            case null, default -> throw new IllegalStateException("Unexpected tag-type: " + tag);

        };
    }

}
