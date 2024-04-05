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
package de.bluecolored.bluemap.common.config.storage;

import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.common.config.ConfigurationException;
import de.bluecolored.bluemap.core.storage.Storage;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Keyed;
import de.bluecolored.bluemap.core.util.Registry;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.Locale;

@SuppressWarnings("FieldMayBeFinal")
@DebugDump
@ConfigSerializable
public abstract class StorageConfig {

    private String storageType = StorageType.FILE.getKey().getFormatted();

    public StorageType getStorageType() throws ConfigurationException {
        return parseKey(StorageType.REGISTRY, storageType, "storage-type");
    }

    public abstract Storage createStorage() throws ConfigurationException;

    static <T extends Keyed> T parseKey(Registry<T> registry, String key, String typeName) throws ConfigurationException {
        T type = registry.get(Key.parse(key, Key.BLUEMAP_NAMESPACE));

        if (type == null) {
            // try legacy config format
            Key legacyFormatKey = Key.bluemap(key.toLowerCase(Locale.ROOT));
            type = registry.get(legacyFormatKey);
        }

        if (type == null)
            throw new ConfigurationException("No " + typeName + " found for key: " + key + "!");

        return type;
    }

    @ConfigSerializable
    public static class Base extends StorageConfig {

        @Override
        public Storage createStorage() {
            throw new UnsupportedOperationException();
        }

    }

}
