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
package de.bluecolored.bluemap.common.config.typeserializer;

import com.flowpowered.math.vector.Vector2d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

public class Vector2dTypeSerializer implements TypeSerializer<Vector2d> {

    @Override
    public Vector2d deserialize(Type type, ConfigurationNode node) throws SerializationException {
        var xNode = node.node("x");
        var yNode = node.node("y");

        if (yNode.virtual()) yNode = node.node("z"); // fallback to z if y is not present

        if (xNode.virtual() || yNode.virtual()) throw new SerializationException("Cannot parse Vector2d: value x or y (or z) missing");

        return Vector2d.from(
                xNode.getDouble(),
                yNode.getDouble()
        );
    }

    @Override
    public void serialize(Type type, @Nullable Vector2d obj, ConfigurationNode node) throws SerializationException {
        if (obj != null) {
            node.node("x").set(obj.getX());
            node.node("y").set(obj.getY());
        }
    }

}
