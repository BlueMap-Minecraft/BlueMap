package de.bluecolored.bluemap.core.world.mca.data;

import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluenbt.NBTReader;
import de.bluecolored.bluenbt.TagType;
import de.bluecolored.bluenbt.TypeDeserializer;

import java.io.IOException;

public class Vector3dDeserializer implements TypeDeserializer<Vector3d> {

    @Override
    public Vector3d read(NBTReader reader) throws IOException {
        TagType tag = reader.peek();

        return switch (tag) {

            case INT_ARRAY, LONG_ARRAY, BYTE_ARRAY -> {
                long[] values = reader.nextArrayAsLongArray();
                if (values.length != 3) throw new IllegalStateException("Unexpected array length: " + values.length);
                yield new Vector3d(
                        values[0],
                        values[1],
                        values[2]
                );
            }

            case LIST -> {
                reader.beginList();
                Vector3d value = new Vector3d(
                        reader.nextDouble(),
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
                        case "x": x = reader.nextDouble(); break;
                        case "y": y = reader.nextDouble(); break;
                        case "z": z = reader.nextDouble(); break;
                        default: reader.skip();
                    }
                }
                reader.endCompound();
                yield new Vector3d(x, y, z);
            }

            case null, default -> throw new IllegalStateException("Unexpected tag-type: " + tag);

        };
    }

}
