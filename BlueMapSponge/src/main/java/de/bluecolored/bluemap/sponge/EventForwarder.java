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

import java.util.Optional;

import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.filter.type.Exclude;
import org.spongepowered.api.event.world.SaveWorldEvent;
import org.spongepowered.api.event.world.chunk.PopulateChunkEvent;
import org.spongepowered.api.world.Location;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;

import de.bluecolored.bluemap.common.plugin.serverinterface.ServerEventListener;

public class EventForwarder {

	private ServerEventListener listener;
	
	public EventForwarder(ServerEventListener listener) {
		this.listener = listener;
	}

	@Listener(order = Order.POST)
	public void onWorldSaveToDisk(SaveWorldEvent evt) {
		listener.onWorldSaveToDisk(evt.getTargetWorld().getUniqueId());		
	}
	
	@Listener(order = Order.POST)
	@Exclude({ChangeBlockEvent.Post.class, ChangeBlockEvent.Pre.class, ChangeBlockEvent.Modify.class})
	public void onBlockChange(ChangeBlockEvent evt) {
		for (Transaction<BlockSnapshot> tr : evt.getTransactions()) {
			if(!tr.isValid()) continue;
			
			Optional<Location<org.spongepowered.api.world.World>> ow = tr.getFinal().getLocation();
			if (ow.isPresent()) {
				listener.onBlockChange(ow.get().getExtent().getUniqueId(), ow.get().getPosition().toInt());
			}
		}
	}

	@Listener(order = Order.POST)
	public void onChunkFinishedGeneration(PopulateChunkEvent.Post evt) {
		Vector3i chunkPos = evt.getTargetChunk().getPosition();
		listener.onChunkFinishedGeneration(evt.getTargetChunk().getWorld().getUniqueId(), new Vector2i(chunkPos.getX(), chunkPos.getZ()));
	}

}
