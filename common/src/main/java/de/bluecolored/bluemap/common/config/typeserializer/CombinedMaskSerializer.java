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

import de.bluecolored.bluemap.common.config.ConfigurationException;
import de.bluecolored.bluemap.common.config.mask.MaskConfig;
import de.bluecolored.bluemap.common.config.mask.MaskType;
import de.bluecolored.bluemap.core.map.mask.CombinedMask;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Objects;

public class CombinedMaskSerializer implements TypeSerializer<CombinedMask> {

    @Override
    public CombinedMask deserialize(Type type, ConfigurationNode node) throws SerializationException {
        try {
            CombinedMask combinedMask = new CombinedMask();
            for (ConfigurationNode listNode : node.childrenList()) {
                MaskConfig maskConfig = Objects.requireNonNull(listNode.get(MaskConfig.Base.class));
                MaskType maskType = maskConfig.getMaskType();
                maskConfig = Objects.requireNonNull(listNode.get(maskType.getConfigType()));
                maskConfig.addTo(combinedMask);
            }
            return combinedMask;
        } catch (ConfigurationException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void serialize(Type type, @Nullable CombinedMask obj, ConfigurationNode node) {
        // serialization not supported
        // configurate calls this method sometimes during deserialization for some reason, so we just do nothing here
    }

}
