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
package de.bluecolored.bluemap.common.api;

import de.bluecolored.bluemap.api.AssetStorage;
import de.bluecolored.bluemap.core.storage.Storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

public class AssetStorageImpl implements AssetStorage {

    private static final String ASSET_PATH = "assets/";

    private final Storage storage;
    private final String mapId;

    public AssetStorageImpl(Storage storage, String mapId) {
        this.storage = storage;
        this.mapId = mapId;
    }

    @Override
    public OutputStream writeAsset(String name) throws IOException {
        return storage.writeMeta(mapId, ASSET_PATH + name);
    }

    @Override
    public Optional<InputStream> readAsset(String name) throws IOException {
        return storage.readMeta(mapId, ASSET_PATH + name);
    }

    @Override
    public boolean assetExists(String name) throws IOException {
        return storage.readMetaInfo(mapId, ASSET_PATH + name).isPresent();
    }

    @Override
    public String getAssetUrl(String name) {
        return "maps/" + mapId + "/" + Storage.escapeMetaName(ASSET_PATH + name);
    }

    @Override
    public void deleteAsset(String name) throws IOException {
        storage.deleteMeta(mapId, ASSET_PATH + name);
    }

}
