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
package de.bluecolored.bluemap.core.resources.adapter;

import com.flowpowered.math.vector.*;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockmodel.Face;
import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.math.Axis;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.biome.GrassColorModifier;

import java.util.EnumMap;

public class ResourcesGson {

    public static final Gson INSTANCE = addAdapter(new GsonBuilder())
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setLenient()
            .create();

    public static GsonBuilder addAdapter(GsonBuilder builder) {
        return builder
                .registerTypeAdapter(Axis.class, new AxisAdapter())
                .registerTypeAdapter(Color.class, new ColorAdapter())
                .registerTypeAdapter(Direction.class, new DirectionAdapter())
                .registerTypeAdapter(Vector2i.class, new Vector2iAdapter())
                .registerTypeAdapter(Vector3d.class, new Vector3dAdapter())
                .registerTypeAdapter(Vector3f.class, new Vector3fAdapter())
                .registerTypeAdapter(Vector4d.class, new Vector4dAdapter())
                .registerTypeAdapter(Vector4f.class, new Vector4fAdapter())
                .registerTypeAdapter(
                        new TypeToken<EnumMap<Direction, Face>>(){}.getType(),
                        new EnumMapInstanceCreator<Direction, Face>(Direction.class)
                )
                .registerTypeAdapter(GrassColorModifier.class, new RegistryAdapter<>(
                        GrassColorModifier.REGISTRY,
                        Key.MINECRAFT_NAMESPACE,
                        GrassColorModifier.NONE
                ));
    }

}
