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
package de.bluecolored.bluemap.fabric;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;

import de.bluecolored.bluemap.common.plugin.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.fabric.events.PlayerJoinCallback;
import de.bluecolored.bluemap.fabric.events.PlayerLeaveCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class FabricEventForwarder {

	private FabricMod mod;
	private Collection<ServerEventListener> eventListeners;
	
	public FabricEventForwarder(FabricMod mod) {
		this.mod = mod;
		this.eventListeners = new ArrayList<>(1);

		AttackBlockCallback.EVENT.register(this::onBlockAttack);
		UseBlockCallback.EVENT.register(this::onBlockUse);
		
		PlayerJoinCallback.EVENT.register(this::onPlayerJoin);
		PlayerLeaveCallback.EVENT.register(this::onPlayerLeave);
	}
	
	public synchronized void addEventListener(ServerEventListener listener) {
		this.eventListeners.add(listener);
	}
	
	public synchronized void removeAllListeners() {
		this.eventListeners.clear();
	}
	

	public ActionResult onBlockUse(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
		if (world instanceof ServerWorld) {
			onBlockChange((ServerWorld) world, hitResult.getBlockPos());
		}
		
		return ActionResult.PASS;
	}
	
	public ActionResult onBlockAttack(PlayerEntity player, World world, Hand hand, BlockPos pos, Direction direction) {
		if (world instanceof ServerWorld) {
			onBlockChange((ServerWorld) world, pos);
		}
		
		return ActionResult.PASS;
	}
	
	public synchronized void onBlockChange(ServerWorld world, BlockPos blockPos) {
		Vector3i position = new Vector3i(blockPos.getX(), blockPos.getY(), blockPos.getZ());
		
		try {
			UUID uuid = mod.getUUIDForWorld(world);
			for (ServerEventListener listener : eventListeners) listener.onBlockChange(uuid, position);
		} catch (IOException e) {
			Logger.global.noFloodError("Failed to get the UUID for a world!", e);
		}
	}
	
	public synchronized void onPlayerJoin(MinecraftServer server, ServerPlayerEntity player) {
		if (this.mod.getServer() != server) return;
		
		UUID uuid = player.getUuid();
		for (ServerEventListener listener : eventListeners) listener.onPlayerJoin(uuid);
	}
	
	public synchronized void onPlayerLeave(MinecraftServer server, ServerPlayerEntity player) {
		if (this.mod.getServer() != server) return;

		UUID uuid = player.getUuid();
		for (ServerEventListener listener : eventListeners) listener.onPlayerLeave(uuid);
	}
	
}
