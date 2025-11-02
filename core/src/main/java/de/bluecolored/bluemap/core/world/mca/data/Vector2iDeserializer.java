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
                        case "x" -> x = reader.nextDouble();
                        case "y", "z" -> y = reader.nextDouble();
                        default -> reader.skip();
                    }
                }
                reader.endCompound();
                yield new Vector2i(x, y);
            }

            case null, default -> throw new IllegalStateException("Unexpected tag-type: " + tag);

        };
    }

}
