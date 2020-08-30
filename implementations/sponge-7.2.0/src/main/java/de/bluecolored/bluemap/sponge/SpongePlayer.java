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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;

import com.flowpowered.math.vector.Vector3d;

import de.bluecolored.bluemap.common.plugin.serverinterface.Gamemode;
import de.bluecolored.bluemap.common.plugin.serverinterface.Player;
import de.bluecolored.bluemap.common.plugin.text.Text;

public class SpongePlayer implements Player {
	
	private static final Map<GameMode, Gamemode> GAMEMODE_MAP = new HashMap<>(5);
	static {
		GAMEMODE_MAP.put(GameModes.ADVENTURE, Gamemode.ADVENTURE);
		GAMEMODE_MAP.put(GameModes.SURVIVAL, Gamemode.SURVIVAL);
		GAMEMODE_MAP.put(GameModes.CREATIVE, Gamemode.CREATIVE);
		GAMEMODE_MAP.put(GameModes.SPECTATOR, Gamemode.SPECTATOR);
		GAMEMODE_MAP.put(GameModes.NOT_SET, Gamemode.SURVIVAL);
	}
	
	private UUID uuid;
	private Text name;
	private UUID world;
	private Vector3d position;
	private boolean online;
	private boolean sneaking;
	private boolean invisible;
	private Gamemode gamemode;
	
	public SpongePlayer(UUID playerUUID) {
		this.uuid = playerUUID;
		update();
	}
	
	@Override
	public UUID getUuid() {
		return this.uuid;
	}

	@Override
	public Text getName() {
		return this.name;
	}

	@Override
	public UUID getWorld() {
		return this.world;
	}

	@Override
	public Vector3d getPosition() {
		return this.position;
	}

	@Override
	public boolean isOnline() {
		return this.online;
	}

	@Override
	public boolean isSneaking() {
		return this.sneaking;
	}

	@Override
	public boolean isInvisible() {
		return this.invisible;
	}

	@Override
	public Gamemode getGamemode() {
		return this.gamemode;
	}
	
	/**
	 * API access, only call on server thread!
	 */
	public void update() {
		org.spongepowered.api.entity.living.player.Player player = Sponge.getServer().getPlayer(uuid).orElse(null);
		if (player == null) {
			this.online = false;
			return;
		}
		
		this.gamemode = GAMEMODE_MAP.get(player.get(Keys.GAME_MODE).orElse(GameModes.NOT_SET));
		
		boolean invis = player.get(Keys.VANISH).orElse(false);
		if (!invis && player.get(Keys.INVISIBLE).orElse(false)) invis = true;
		if (!invis) {
			Optional<List<PotionEffect>> effects = player.get(Keys.POTION_EFFECTS);
			if (effects.isPresent()) {
				for (PotionEffect effect : effects.get()) {
					if (effect.getType().equals(PotionEffectTypes.INVISIBILITY) && effect.getDuration() > 0) invis = true;
				}
			}
		}
		this.invisible = invis;
		
		this.name = Text.of(player.getName());
		this.online = player.isOnline();
		this.position = player.getPosition();
		this.sneaking = player.get(Keys.IS_SNEAKING).orElse(false);
		this.world = player.getWorld().getUniqueId();
	}

}
