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
package de.bluecolored.bluemap.common.plugin;

import de.bluecolored.bluemap.core.debug.DebugDump;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@DebugDump
public class PluginConfig {

	private boolean liveUpdatesEnabled;
	private boolean skinDownloadEnabled;
	private Collection<String> hiddenGameModes;
	private boolean hideInvisible;
	private boolean hideSneaking;
	private int playerRenderLimit;
	private long fullUpdateIntervalMinutes;
	
	public PluginConfig(ConfigurationNode node) {

		//liveUpdates
		liveUpdatesEnabled = node.node("liveUpdates").getBoolean(true);

		//skinDownloadEnabled
		skinDownloadEnabled = node.node("skinDownload").getBoolean(true);
		
		//hiddenGameModes
		hiddenGameModes = new ArrayList<>();
		for (ConfigurationNode gameModeNode : node.node("hiddenGameModes").childrenList()) {
			hiddenGameModes.add(gameModeNode.getString());
		}
		hiddenGameModes = Collections.unmodifiableCollection(hiddenGameModes);
		
		//hideInvisible
		hideInvisible = node.node("hideInvisible").getBoolean(true);
		
		//hideSneaking
		hideSneaking = node.node("hideSneaking").getBoolean(false);

		//playerRenderLimit
		playerRenderLimit = node.node("playerRenderLimit").getInt(-1);

		//periodic map updates
		fullUpdateIntervalMinutes = node.node("fullUpdateInterval").getLong(TimeUnit.HOURS.toMinutes(24));
		
	}

	public boolean isLiveUpdatesEnabled() {
		return this.liveUpdatesEnabled;
	}
	
	public boolean isSkinDownloadEnabled() {
		return this.skinDownloadEnabled;
	}

	public Collection<String> getHiddenGameModes() {
		return this.hiddenGameModes;
	}

	public boolean isHideInvisible() {
		return this.hideInvisible;
	}

	public boolean isHideSneaking() {
		return this.hideSneaking;
	}

	public int getPlayerRenderLimit() {
		return playerRenderLimit;
	}

	public long getFullUpdateIntervalMinutes() {
		return fullUpdateIntervalMinutes;
	}

}
