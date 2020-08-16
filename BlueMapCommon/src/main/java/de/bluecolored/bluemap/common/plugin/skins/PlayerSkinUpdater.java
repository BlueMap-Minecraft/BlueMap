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

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import de.bluecolored.bluemap.common.plugin.serverinterface.ServerEventListener;

public class PlayerSkinUpdater implements ServerEventListener {

	private File storageFolder;
	
	private Map<UUID, PlayerSkin> skins;
	
	public PlayerSkinUpdater(File storageFolder) {
		this.storageFolder = storageFolder;
		this.skins = new ConcurrentHashMap<>();
		
		this.storageFolder.mkdirs();
	}
	
	public void updateSkin(UUID playerUuid) {
		PlayerSkin skin = skins.get(playerUuid);
		
		if (skin == null) {
			skin = new PlayerSkin(playerUuid);
			skins.put(playerUuid, skin);
		}
		
		skin.update(storageFolder);
	}
	
	@Override
	public void onPlayerJoin(UUID playerUuid) {
		updateSkin(playerUuid);
	}
	
}
