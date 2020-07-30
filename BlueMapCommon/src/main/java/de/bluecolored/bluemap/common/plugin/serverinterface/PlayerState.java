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
package de.bluecolored.bluemap.common.plugin.serverinterface;

import java.util.UUID;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.base.MoreObjects;

import de.bluecolored.bluemap.common.plugin.text.Text;

public class PlayerState {

	private final UUID uuid;
	protected Text name;
	protected UUID world;
	protected Vector3d position = Vector3d.ZERO;
	protected boolean online = false;
	protected boolean sneaking = false;
	protected boolean invisible = false;
	protected Gamemode gamemode = Gamemode.SURVIVAL;
	
	public PlayerState(UUID uuid, Text name, UUID world, Vector3d position) {
		this.uuid = uuid;
		this.name = name;
		this.world = world;
		this.position = position;
	}
	
	public UUID getUuid() {
		return uuid;
	}
	
	public Text getName() {
		return name;
	}
	
	public UUID getWorld() {
		return world;
	}
	
	public Vector3d getPosition() {
		return position;
	}

	public boolean isOnline() {
		return online;
	}
	
	/**
	 * Return <code>true</code> if the player is sneaking.
	 * <p><i>If the player is offline the value of this method is undetermined.</i></p>
	 * @return
	 */
	public boolean isSneaking() {
		return sneaking;
	}
	
	/**
	 * Returns <code>true</code> if the player has an invisibillity effect
	 * <p><i>If the player is offline the value of this method is undetermined.</i></p>
	 */
	public boolean isInvisible() {
		return invisible;
	}
	
	/**
	 * Returns the {@link Gamemode} this player is in
	 * <p><i>If the player is offline the value of this method is undetermined.</i></p>
	 */
	public Gamemode getGamemode() {
		return gamemode;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
		.add("uuid", uuid)
		//.add("name", name)
		//.add("world", world)
		//.add("position", position)
		//.add("online", online)
		//.add("sneaking", sneaking)
		//.add("invisible", invisible)
		//.add("gamemode", gamemode)
		.toString();
	}
	
}
