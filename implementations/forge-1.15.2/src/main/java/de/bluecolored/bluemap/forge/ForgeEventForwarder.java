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
package de.bluecolored.bluemap.forge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;

import de.bluecolored.bluemap.common.plugin.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ForgeEventForwarder  {
	
	private ForgeMod mod;
	private Collection<ServerEventListener> eventListeners;
	
	private Deque<WorldChunk> loadChunkEvents;
	private Thread loadChunkEventProcessor;
	
	public ForgeEventForwarder(ForgeMod mod) {
		this.mod = mod;
		this.eventListeners = new ArrayList<>(1);
		
		loadChunkEvents = new ConcurrentLinkedDeque<>();

		MinecraftForge.EVENT_BUS.register(this);
		
		//see processLoadChunkEvents JavaDoc comment
		loadChunkEventProcessor = new Thread(this::processLoadChunkEvents);
		loadChunkEventProcessor.setDaemon(true);
		loadChunkEventProcessor.start();
	}

	public synchronized void addEventListener(ServerEventListener listener) {
		this.eventListeners.add(listener);
	}
	
	public synchronized void removeAllListeners() {
		this.eventListeners.clear();
	}
	
	@SubscribeEvent
	public void onBlockBreak(BlockEvent.BreakEvent evt) {
		onBlockChange(evt);
	}
	
	@SubscribeEvent
	public void onBlockPlace(BlockEvent.EntityPlaceEvent evt) {
		onBlockChange(evt);
	}
	
	private synchronized void onBlockChange(BlockEvent evt) {
		if (!(evt.getWorld() instanceof ServerWorld)) return;
		
		try {
			UUID world = mod.getUUIDForWorld((ServerWorld) evt.getWorld());
			Vector3i position = new Vector3i(
					evt.getPos().getX(),
					evt.getPos().getY(),
					evt.getPos().getZ()
				);
			
			for (ServerEventListener listener : eventListeners) listener.onBlockChange(world, position);
			
		} catch (IOException e) {
			Logger.global.noFloodError("Failed to get the UUID for a world!", e);
		}
	}
	
	@SubscribeEvent
	public synchronized void onChunkSave(ChunkDataEvent.Save evt) {
		if (!(evt.getWorld() instanceof ServerWorld)) return;
		
		Vector2i chunkPos = new Vector2i(evt.getChunk().getPos().x, evt.getChunk().getPos().z);
		
		try {
			UUID world = mod.getUUIDForWorld((ServerWorld) evt.getWorld());
			for (ServerEventListener listener : eventListeners) listener.onChunkSaveToDisk(world, chunkPos);
		} catch (IOException e) {
			Logger.global.noFloodError("Failed to get the UUID for a world!", e);
		}
	}

	/* Use ChunkSaveToDisk as it is the preferred event to use and more reliable on the chunk actually saved to disk
	@SubscribeEvent
	public synchronized void onWorldSave(WorldEvent.Save evt) {
		if (!(evt.getWorld() instanceof ServerWorld)) return;
		
		try {
			UUID world = mod.getUUIDForWorld((ServerWorld) evt.getWorld());
			for (ServerEventListener listener : eventListeners) listener.onWorldSaveToDisk(world);
		} catch (IOException e) {
			Logger.global.noFloodError("Failed to get the UUID for a world!", e);
		}
	}
	*/

	@SubscribeEvent
	public synchronized void onPlayerJoin(PlayerLoggedInEvent evt) {
		UUID uuid = evt.getPlayer().getUniqueID();
		for (ServerEventListener listener : eventListeners) listener.onPlayerJoin(uuid);
	}

	@SubscribeEvent
	public synchronized void onPlayerLeave(PlayerLoggedOutEvent evt) {
		UUID uuid = evt.getPlayer().getUniqueID();
		for (ServerEventListener listener : eventListeners) listener.onPlayerLeave(uuid);
	}
	
	@SubscribeEvent
	public void onChunkLoad(ChunkEvent.Load evt) {
		if (!(evt.getWorld() instanceof ServerWorld)) return;

		try {
			UUID world = mod.getUUIDForWorld((ServerWorld) evt.getWorld());
			Vector2i chunk = new Vector2i(evt.getChunk().getPos().x, evt.getChunk().getPos().z);

			synchronized (loadChunkEvents) {
				loadChunkEvents.add(new WorldChunk(world, chunk));
				loadChunkEvents.notify();
			}
		} catch (IOException e) {
			Logger.global.noFloodError("Failed to get the UUID for a world!", e);
		}
	}
	
	/**
	 * This is a workaround for forge not providing a way to detect if chunks are newly generated:
	 * Each time a chunk-load-event occurs, it is (asynchronously) tested if the chunk is already generated on the world files.
	 * If it is a new chunk it will likely not be saved to the disk right away.
	 */
	private void processLoadChunkEvents() {
		while (!Thread.interrupted()) {
			WorldChunk worldChunk;
			if (mod.getPlugin().isLoaded() && (worldChunk = loadChunkEvents.poll()) != null) {
				try {
					World world = mod.getPlugin().getWorld(worldChunk.world);
					if (world == null || world.isChunkGenerated(worldChunk.chunk)) continue;
	
					for (ServerEventListener listener : eventListeners) listener.onChunkFinishedGeneration(worldChunk.world, worldChunk.chunk);
					
				} catch (RuntimeException e) {
					Logger.global.noFloodWarning("processLoadChunkEventsError", "Failed to test if a chunk is newly generated:" + e);
				}
			} else {
				synchronized (loadChunkEvents) {
					try {
						loadChunkEvents.wait(10000);
					} catch (InterruptedException e) {
						break;
					}	
				}
			}
		}
		
		Thread.currentThread().interrupt();
	}
	
	private class WorldChunk {
		final UUID world;
		final Vector2i chunk;
		
		public WorldChunk(UUID world, Vector2i chunk) {
			this.world = world;
			this.chunk = chunk;
		}
	}

}
