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
package de.bluecolored.bluemap.sponge8;

import de.bluecolored.bluemap.common.plugin.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.common.plugin.text.Text;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.adventure.SpongeComponents;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.message.PlayerChatEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.event.world.chunk.ChunkEvent;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;

import java.util.Optional;

public class EventForwarder {

	private ServerEventListener listener;
	
	public EventForwarder(ServerEventListener listener) {
		this.listener = listener;
	}

	/* Use ChunkSaveToDisk as it is the preferred event to use and more reliable on the chunk actually saved to disk
	@Listener(order = Order.POST)
	public void onWorldSaveToDisk(SaveWorldEvent evt) {
		listener.onWorldSaveToDisk(evt.getTargetWorld().getUniqueId());		
	}
	*/
	
	@Listener(order = Order.POST)
	public void onChunkSaveToDisk(ChunkEvent.Save.Pre evt) {
		ResourceKey worldKey = evt.chunkWorld();
		Optional<ServerWorld> world = Sponge.server().worldManager().world(worldKey);
		if (world.isPresent()) {
			listener.onChunkSaveToDisk(world.get().uniqueId(), SpongePlugin.fromSpongePoweredVector(evt.targetChunk().chunkPosition().toVector2(true)));
		}
	}

	@Listener(order = Order.POST)
	public void onBlockChange(ChangeBlockEvent.All evt) {
		for (Transaction<BlockSnapshot> tr : evt.transactions()) {
			if(!tr.isValid()) continue;

			Optional<ServerLocation> ow = tr.finalReplacement().location();
			if (ow.isPresent()) {
				listener.onBlockChange(ow.get().world().uniqueId(), SpongePlugin.fromSpongePoweredVector(ow.get().position().toInt()));
			}
		}
	}

	// TODO event missing in Sponge
//	@Listener(order = Order.POST)
//	public void onChunkFinishedGeneration(PopulateChunkEvent.Post evt) {
//		Vector3i chunkPos = evt.getTargetChunk().getPosition();
//		listener.onChunkFinishedGeneration(evt.getTargetChunk().getWorld().getUniqueId(), new Vector2i(chunkPos.getX(), chunkPos.getZ()));
//	}

	@Listener(order = Order.POST)
	public void onPlayerJoin(ServerSideConnectionEvent.Join evt) {
		listener.onPlayerJoin(evt.player().uniqueId());
	}
	
	@Listener(order = Order.POST)
	public void onPlayerLeave(ServerSideConnectionEvent.Disconnect evt) {
		listener.onPlayerJoin(evt.player().uniqueId());
	}
	
	@Listener(order = Order.POST)
	public void onPlayerChat(PlayerChatEvent evt) {
		listener.onChatMessage(Text.of(SpongeComponents.plainSerializer().serialize(evt.message())));
	}
	
}
