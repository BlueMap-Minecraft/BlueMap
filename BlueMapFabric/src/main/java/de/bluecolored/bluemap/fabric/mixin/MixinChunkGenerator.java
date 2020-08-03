package de.bluecolored.bluemap.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.flowpowered.math.vector.Vector2i;

import de.bluecolored.bluemap.fabric.events.ChunkFinalizeCallback;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

@Mixin(ChunkGenerator.class)
public class MixinChunkGenerator {

	@Inject(at = @At("RETURN"), method = "generateFeatures")
	public void generateFeatures(ChunkRegion region, StructureAccessor accessor, CallbackInfo ci) {
		ChunkFinalizeCallback.EVENT.invoker().onChunkFinalized(region.getWorld(), new Vector2i(region.getCenterChunkX(), region.getCenterChunkZ()));
	}
	
}
