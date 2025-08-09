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
package de.bluecolored.bluemap.common.config.mask;

import de.bluecolored.bluemap.common.config.ConfigurationException;
import de.bluecolored.bluemap.core.map.mask.CombinedMask;
import de.bluecolored.bluemap.core.map.mask.Mask;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Keyed;
import de.bluecolored.bluemap.core.util.Registry;
import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@SuppressWarnings("FieldMayBeFinal")
@ConfigSerializable
public abstract class MaskConfig {

    private String type = MaskType.BOX.getKey().getFormatted();
    @Getter private boolean subtract = false;

    public MaskType getMaskType() throws ConfigurationException {
        return parseKey(MaskType.REGISTRY, type, "mask-type");
    }

    public abstract Mask createMask() throws ConfigurationException;

    public void addTo(CombinedMask combinedMask) throws ConfigurationException {
        Mask mask = createMask();
        combinedMask.add(mask, !subtract);
    }

    static <T extends Keyed> T parseKey(Registry<T> registry, String key, String typeName) throws ConfigurationException {
        T type = registry.get(Key.parse(key, Key.BLUEMAP_NAMESPACE));

        if (type == null)
            throw new ConfigurationException("No " + typeName + " found for key: " + key + "!");

        return type;
    }

    @ConfigSerializable
    public static class Base extends MaskConfig {

        @Override
        public Mask createMask() {
            throw new UnsupportedOperationException();
        }

    }

}
