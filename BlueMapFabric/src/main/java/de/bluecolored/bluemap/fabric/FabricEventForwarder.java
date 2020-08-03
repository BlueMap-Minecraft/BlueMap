package de.bluecolored.bluemap.fabric;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;

import de.bluecolored.bluemap.common.plugin.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.fabric.events.ChunkFinalizeCallback;
import de.bluecolored.bluemap.fabric.events.WorldSaveCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.entity.player.PlayerEntity;
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
		
		WorldSaveCallback.EVENT.register(this::onWorldSave);
		ChunkFinalizeCallback.EVENT.register(this::onChunkFinalize);
		AttackBlockCallback.EVENT.register(this::onBlockAttack);
		UseBlockCallback.EVENT.register(this::onBlockUse);
	}
	
	public void addEventListener(ServerEventListener listener) {
		this.eventListeners.add(listener);
	}
	
	public void removeAllListeners() {
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
	
	public void onBlockChange(ServerWorld world, BlockPos blockPos) {
		Vector3i position = new Vector3i(blockPos.getX(), blockPos.getY(), blockPos.getZ());
		
		try {
			UUID uuid = mod.getUUIDForWorld(world);
			eventListeners.forEach(e -> e.onBlockChange(uuid, position));
		} catch (IOException e) {
			Logger.global.logError("Failed to get UUID for world: " + world, e);
		}
	}
	
	public void onWorldSave(ServerWorld world) {
		try {
			UUID uuid = mod.getUUIDForWorld(world);
			eventListeners.forEach(e -> e.onWorldSaveToDisk(uuid));
		} catch (IOException e) {
			Logger.global.logError("Failed to get UUID for world: " + world, e);
		}
	}
	
	public void onChunkFinalize(ServerWorld world, Vector2i chunkPos) {
		try {
			UUID uuid = mod.getUUIDForWorld(world);
			eventListeners.forEach(e -> e.onChunkFinishedGeneration(uuid, chunkPos));
		} catch (IOException e) {
			Logger.global.logError("Failed to get UUID for world: " + world, e);
		}
	}
	
}
