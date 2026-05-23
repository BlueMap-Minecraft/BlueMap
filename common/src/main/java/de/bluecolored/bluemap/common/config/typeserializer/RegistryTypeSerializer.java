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

import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Keyed;
import de.bluecolored.bluemap.core.util.Registry;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

@RequiredArgsConstructor
public class RegistryTypeSerializer<T extends Keyed> implements TypeSerializer<T> {

    private final Registry<T> registry;
    private final String defaultNamespace;
    private final T defaultValue;

    @Override
    public T deserialize(Type type, ConfigurationNode node) throws SerializationException {
        String keyString = node.getString();
        if (keyString == null) return defaultValue;

        Key key = Key.parse(keyString, defaultNamespace);
        T value = registry.get(key);
        if (value != null) return value;

        throw new SerializationException("Unknown key '" + keyString + "' for type '" + type.getTypeName() + "'");
    }

    @Override
    public void serialize(Type type, @Nullable T obj, ConfigurationNode node) throws SerializationException {
        if (obj == null) return;
        node.set(obj.getKey().getFormatted());
    }

}