package de.bluecolored.bluemap.sponge;

import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.Sponge;
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
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

public class MapUpdateHandler {

	public Multimap<MapType, Vector2i> updateBuffer;
	
	public MapUpdateHandler() {
		updateBuffer = MultimapBuilder.hashKeys().hashSetValues().build();
		
		Sponge.getEventManager().registerListeners(SpongePlugin.getInstance(), this);
	}
	
	@Listener(order = Order.POST)
	public void onWorldSave(SaveWorldEvent.Post evt) {
		UUID worldUuid = evt.getTargetWorld().getUniqueId();
		RenderManager renderManager = SpongePlugin.getInstance().getRenderManager();
		
		synchronized (updateBuffer) {
			Iterator<MapType> iterator = updateBuffer.keys().iterator();
			while (iterator.hasNext()) {
				MapType map = iterator.next();
				if (map.getWorld().getUUID().equals(worldUuid)) {
					renderManager.createTickets(map, updateBuffer.get(map));
					iterator.remove();
				}
			}
			
		}
	}
	
	@Listener(order = Order.POST)
	@Exclude({ChangeBlockEvent.Post.class, ChangeBlockEvent.Pre.class, ChangeBlockEvent.Modify.class})
	public void onBlockChange(ChangeBlockEvent evt) {
		synchronized (updateBuffer) {
			for (Transaction<BlockSnapshot> tr : evt.getTransactions()) {
				if (!tr.isValid()) continue;
				
				Optional<Location<org.spongepowered.api.world.World>> ow = tr.getFinal().getLocation();
				if (ow.isPresent()) {
					updateBlock(ow.get().getExtent().getUniqueId(), ow.get().getPosition().toInt());
				}
			}
		}
	}
	
	@Listener(order = Order.POST)
	public void onChunkPopulate(PopulateChunkEvent.Post evt) {
		UUID world = evt.getTargetChunk().getWorld().getUniqueId();
		
		int x = evt.getTargetChunk().getPosition().getX();
		int z = evt.getTargetChunk().getPosition().getZ();
		
		// also update the chunks around, because they might be modified or not rendered yet due to finalizations
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				updateChunk(world, new Vector2i(x + dx, z + dz));
			}
		}
	}
	
	private void updateChunk(UUID world, Vector2i chunkPos) {
		Vector3i min = new Vector3i(chunkPos.getX() * 16, 0, chunkPos.getY() * 16);
		Vector3i max = min.add(15, 255, 15);
		
		Vector3i xmin = new Vector3i(min.getX(), 0, max.getY());
		Vector3i xmax = new Vector3i(max.getX(), 255, min.getY());
		
		//update all corners so we always update all tiles containing this chunk
		synchronized (updateBuffer) {
			updateBlock(world, min);
			updateBlock(world, max);
			updateBlock(world, xmin);
			updateBlock(world, xmax);
		}
	}
	
	private void updateBlock(UUID world, Vector3i pos){
		synchronized (updateBuffer) {
			for (MapType mapType : SpongePlugin.getInstance().getMapTypes()) {
				if (mapType.getWorld().getUUID().equals(world)) {
					mapType.getWorld().invalidateChunkCache(mapType.getWorld().blockPosToChunkPos(pos));
					
					Vector2i tile = mapType.getTileRenderer().getHiresModelManager().posToTile(pos);
					updateBuffer.put(mapType, tile);
				}
			}
		}
	}
	
	public int getUpdateBufferCount() {
		return updateBuffer.size();
	}
	
	public void flushTileBuffer() {
		RenderManager renderManager = SpongePlugin.getInstance().getRenderManager();
		
		synchronized (updateBuffer) {
			for (MapType map : updateBuffer.keySet()) {
				renderManager.createTickets(map, updateBuffer.get(map));
			}
			updateBuffer.clear();
		}
	}
	
}
