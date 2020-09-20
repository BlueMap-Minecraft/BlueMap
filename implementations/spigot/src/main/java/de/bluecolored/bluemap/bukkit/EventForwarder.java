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
package de.bluecolored.bluemap.bukkit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;

import de.bluecolored.bluemap.common.plugin.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.common.plugin.text.Text;

public class EventForwarder implements Listener {

	private Collection<ServerEventListener> listeners;
	
	public EventForwarder() {
		listeners = new ArrayList<>();
	}
	
	public synchronized void addListener(ServerEventListener listener) {
		listeners.add(listener);
	}
	
	public synchronized void removeAllListeners() {
		listeners.clear();
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public synchronized void onChunkSaveToDisk(ChunkUnloadEvent evt) {
		if (evt.isSaveChunk()) {
			Vector2i chunkPos = new Vector2i(evt.getChunk().getX(), evt.getChunk().getZ());
			for (ServerEventListener listener : listeners) listener.onChunkSaveToDisk(evt.getWorld().getUID(), chunkPos);		
		}
	}
	
	/* Use ChunkSaveToDisk as it is the preferred event to use and more reliable on the chunk actually saved to disk
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public synchronized void onWorldSaveToDisk(WorldSaveEvent evt) {
		for (ServerEventListener listener : listeners) listener.onWorldSaveToDisk(evt.getWorld().getUID());		
	}
	*/

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockChange(BlockPlaceEvent evt) {
		onBlockChange(evt.getBlock().getLocation());
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockChange(BlockBreakEvent evt) {
		onBlockChange(evt.getBlock().getLocation());
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockChange(BlockGrowEvent evt) {
		onBlockChange(evt.getBlock().getLocation());
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockChange(BlockBurnEvent evt) {
		onBlockChange(evt.getBlock().getLocation());
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockChange(BlockExplodeEvent evt) {
		onBlockChange(evt.getBlock().getLocation());
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockChange(BlockFadeEvent evt) {
		onBlockChange(evt.getBlock().getLocation());
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockChange(BlockSpreadEvent evt) {
		onBlockChange(evt.getBlock().getLocation());
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockChange(BlockFormEvent evt) {
		onBlockChange(evt.getBlock().getLocation());
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockChange(BlockFertilizeEvent evt) {
		onBlockChange(evt.getBlock().getLocation());
	}
	
	private synchronized void onBlockChange(Location loc) {
		UUID world = loc.getWorld().getUID();
		Vector3i pos = new Vector3i(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
		for (ServerEventListener listener : listeners) listener.onBlockChange(world, pos);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public synchronized void onChunkFinishedGeneration(ChunkPopulateEvent evt) {
		Chunk chunk = evt.getChunk();
		UUID world = chunk.getWorld().getUID();
		Vector2i chunkPos = new Vector2i(chunk.getX(), chunk.getZ());
		for (ServerEventListener listener : listeners) listener.onChunkFinishedGeneration(world, chunkPos);
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public synchronized void onPlayerJoin(PlayerJoinEvent evt) {
		for (ServerEventListener listener : listeners) listener.onPlayerJoin(evt.getPlayer().getUniqueId());
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public synchronized void onPlayerLeave(PlayerQuitEvent evt) {
		for (ServerEventListener listener : listeners) listener.onPlayerJoin(evt.getPlayer().getUniqueId());
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public synchronized void onPlayerChat(AsyncPlayerChatEvent evt) {
		String message = String.format(evt.getFormat(), evt.getPlayer().getDisplayName(), evt.getMessage());
		for (ServerEventListener listener : listeners) listener.onChatMessage(Text.of(message));
	}

}
