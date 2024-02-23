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

import de.bluecolored.bluemap.core.storage.Storage;
import de.bluecolored.bluemap.core.storage.b2.B2Storage;
import de.bluecolored.bluemap.core.storage.file.FileStorage;
import de.bluecolored.bluemap.core.storage.sql.SQLStorage;

public enum StorageType {

    FILE (FileConfig.class, FileStorage::new),
    SQL (SQLConfig.class, SQLStorage::create),
    B2 (B2Config.class, B2Storage::new);

    private final Class<? extends StorageConfig> configType;
    private final StorageFactory<? extends StorageConfig> storageFactory;

    <C extends StorageConfig> StorageType(Class<C> configType, StorageFactory<C> storageFactory) {
        this.configType = configType;
        this.storageFactory = storageFactory;
    }

    public Class<? extends StorageConfig> getConfigType() {
        return configType;
    }

    @SuppressWarnings("unchecked")
    public <C extends StorageConfig> StorageFactory<C> getStorageFactory(Class<C> configType) {
        if (!configType.isAssignableFrom(this.configType)) throw new ClassCastException(this.configType + " can not be cast to " + configType);
        return (StorageFactory<C>) storageFactory;
    }

    @FunctionalInterface
    public interface StorageFactory<C extends StorageConfig> {
        Storage provideRaw(C config) throws Exception;

        @SuppressWarnings("unchecked")
        default Storage provide(StorageConfig config) throws Exception {
            return provideRaw((C) config);
        }
    }

}
