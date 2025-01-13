package de.bluecolored.bluemap.core.world.mca.data;

import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluenbt.NBTReader;
import de.bluecolored.bluenbt.TagType;
import de.bluecolored.bluenbt.TypeDeserializer;

import java.io.IOException;

public class Vector2fDeserializer implements TypeDeserializer<Vector2f> {

    @Override
    public Vector2f read(NBTReader reader) throws IOException {
        TagType tag = reader.peek();

        return switch (tag) {

            case INT_ARRAY, LONG_ARRAY, BYTE_ARRAY -> {
                long[] values = reader.nextArrayAsLongArray();
                if (values.length != 2) throw new IllegalStateException("Unexpected array length: " + values.length);
                yield new Vector2f(
                        values[0],
                        values[1]
                );
            }

            case LIST -> {
                reader.beginList();
                Vector2f value = new Vector2f(
                        reader.nextDouble(),
                        reader.nextDouble()
                );
                reader.endList();
                yield value;
            }

            case COMPOUND -> {
                double x = 0, y = 0, z = 0;
                reader.beginCompound();
                while (reader.peek() != TagType.END) {
                    switch (reader.name()) {
                        case "x", "yaw": x = reader.nextDouble(); break;
                        case "y", "pitch": y = reader.nextDouble(); break;
                        default: reader.skip();
                    }
                }
                reader.endCompound();
                yield new Vector2f(x, y);
            }

            case null, default -> throw new IllegalStateException("Unexpected tag-type: " + tag);

        };
    }

}
