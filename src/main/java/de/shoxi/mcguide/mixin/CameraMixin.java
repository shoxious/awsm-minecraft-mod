package de.shoxi.mcguide.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import de.shoxi.mcguide.GuideClient;
import net.minecraft.client.Camera;

/** Zoom: Sichtfeld verkleinern, solange die Zoom-Taste gehalten wird. */
@Mixin(Camera.class)
abstract class CameraMixin {
	@Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
	private void mcguide$zoom(float partialTick, CallbackInfoReturnable<Float> cir) {
		if (GuideClient.zooming()) cir.setReturnValue(cir.getReturnValueF() * 0.25f);
	}
}
