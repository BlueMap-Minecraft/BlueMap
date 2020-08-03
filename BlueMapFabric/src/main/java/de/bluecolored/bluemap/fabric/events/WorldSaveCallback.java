package de.bluecolored.bluemap.fabric.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.world.ServerWorld;

public interface WorldSaveCallback {
	Event<WorldSaveCallback> EVENT = EventFactory.createArrayBacked(WorldSaveCallback.class,
			(listeners) -> (world) -> {
				for (WorldSaveCallback event : listeners) {
					event.onWorldSaved(world);
				}
			}
	);

	void onWorldSaved(ServerWorld world);
}
