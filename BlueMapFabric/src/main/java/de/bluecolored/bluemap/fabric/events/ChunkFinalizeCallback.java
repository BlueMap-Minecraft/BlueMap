package de.bluecolored.bluemap.fabric.events;

import com.flowpowered.math.vector.Vector2i;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.world.ServerWorld;

public interface ChunkFinalizeCallback {
	Event<ChunkFinalizeCallback> EVENT = EventFactory.createArrayBacked(ChunkFinalizeCallback.class,
			(listeners) -> (world, chunkPos) -> {
				for (ChunkFinalizeCallback event : listeners) {
					event.onChunkFinalized(world, chunkPos);
				}
			}
	);

	void onChunkFinalized(ServerWorld world, Vector2i chunkPos);
}
