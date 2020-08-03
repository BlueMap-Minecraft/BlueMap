package de.bluecolored.bluemap.fabric.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.flowpowered.math.vector.Vector2i;

import de.bluecolored.bluemap.fabric.events.ChunkFinalizeCallback;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.IWorld;
import net.minecraft.world.gen.chunk.ChunkGenerator;

@Mixin(ChunkGenerator.class)
public class MixinChunkGenerator {

	@Shadow
	@Final
	protected IWorld world;
	
	@Inject(at = @At("RETURN"), method = "generateFeatures")
	public void generateFeatures(ChunkRegion region, CallbackInfo ci) {
		if (world instanceof ServerWorld) {
			ChunkFinalizeCallback.EVENT.invoker().onChunkFinalized((ServerWorld) world, new Vector2i(region.getCenterChunkX(), region.getCenterChunkZ()));
		}
	}
	
}
