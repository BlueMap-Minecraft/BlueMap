package de.bluecolored.bluemap.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.bluecolored.bluemap.fabric.events.WorldSaveCallback;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ProgressListener;
import net.minecraft.world.SessionLockException;

@Mixin(ServerWorld.class)
public abstract class MixinServerWorld {

	@Inject(at = @At("RETURN"), method = "save")
	public void save(ProgressListener progressListener, boolean flush, boolean bl, CallbackInfo ci) throws SessionLockException {		
		WorldSaveCallback.EVENT.invoker().onWorldSaved((ServerWorld) (Object) this);
	}
	
}
