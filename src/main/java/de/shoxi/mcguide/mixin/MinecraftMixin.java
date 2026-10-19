package de.shoxi.mcguide.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import de.shoxi.mcguide.GuideClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** Vanilla bricht den Abbau jeden Tick ab, wenn die Maustaste nicht gedrueckt ist; waehrend des Faellens nicht. */
@Mixin(Minecraft.class)
abstract class MinecraftMixin {
	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void mcguide$keepFelling(boolean down, CallbackInfo ci) {
		if (GuideClient.fellingBusy()) ci.cancel();
	}

	/** Mob-Markierung: Leuchtumriss, durch Waende sichtbar. */
	@Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
	private void mcguide$markMobs(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() && GuideClient.markedGlow(entity)) cir.setReturnValue(true);
	}
}
