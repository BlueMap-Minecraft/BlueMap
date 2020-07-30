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
package de.bluecolored.bluemap.sponge;

import java.lang.ref.WeakReference;

import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;

import de.bluecolored.bluemap.common.plugin.serverinterface.Gamemode;
import de.bluecolored.bluemap.common.plugin.serverinterface.PlayerState;
import de.bluecolored.bluemap.common.plugin.text.Text;

public class SpongePlayerState extends PlayerState {

	private WeakReference<Player> playerRef;
	
	public SpongePlayerState(Player player) {
		super(player.getUniqueId(), Text.of(player.getName()), player.getWorld().getUniqueId(), player.getPosition());
		this.playerRef = new WeakReference<Player>(player);
		
		update();
	}
	
	void update() {
		Player player = playerRef.get();
		if (player == null) {
			this.online = false;
			return;
		}
		
		this.online = player.isOnline();
		
		if (this.online) {
			this.name = Text.of(player.getName());
			
			this.world = player.getWorld().getUniqueId();
			this.position = player.getPosition();
			
			GameMode gm = player.get(Keys.GAME_MODE).orElse(GameModes.NOT_SET);
			if (gm == GameModes.SURVIVAL) this.gamemode = Gamemode.SURVIVAL;
			else if (gm == GameModes.CREATIVE) this.gamemode = Gamemode.CREATIVE;
			else if (gm == GameModes.SPECTATOR) this.gamemode = Gamemode.SPECTATOR;
			else if (gm == GameModes.ADVENTURE) this.gamemode = Gamemode.ADVENTURE;
			else this.gamemode = Gamemode.SURVIVAL;
			
			this.invisible = player.get(Keys.INVISIBLE).orElse(false);
			this.sneaking = player.get(Keys.IS_SNEAKING).orElse(false);
		}
	}
	
}
