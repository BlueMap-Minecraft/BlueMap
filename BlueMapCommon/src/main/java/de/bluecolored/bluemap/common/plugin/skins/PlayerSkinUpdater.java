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
package de.bluecolored.bluemap.common.plugin.skins;

import de.bluecolored.bluemap.common.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.core.debug.DebugDump;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@DebugDump
public class PlayerSkinUpdater implements ServerEventListener {

    private File storageFolder;
    private File defaultSkin;

    private final Map<UUID, PlayerSkin> skins;

    public PlayerSkinUpdater(File storageFolder, File defaultSkin) throws IOException {
        this.storageFolder = storageFolder;
        this.defaultSkin = defaultSkin;
        this.skins = new ConcurrentHashMap<>();

        FileUtils.forceMkdir(this.storageFolder);
    }

    public void updateSkin(UUID playerUuid) {
        PlayerSkin skin = skins.get(playerUuid);

        if (skin == null) {
            skin = new PlayerSkin(playerUuid);
            skins.put(playerUuid, skin);
        }

        skin.update(storageFolder, defaultSkin);
    }

    @Override
    public void onPlayerJoin(UUID playerUuid) {
        updateSkin(playerUuid);
    }

    public File getStorageFolder() {
        return storageFolder;
    }

    public void setStorageFolder(File storageFolder) {
        this.storageFolder = storageFolder;
    }

    public File getDefaultSkin() {
        return defaultSkin;
    }

    public void setDefaultSkin(File defaultSkin) {
        this.defaultSkin = defaultSkin;
    }

}
