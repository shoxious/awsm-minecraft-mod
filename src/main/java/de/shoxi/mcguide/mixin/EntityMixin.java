package de.shoxi.mcguide.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import de.shoxi.mcguide.GuideClient;
import net.minecraft.world.entity.Entity;

/** Umrissfarbe fuer die Mob-Markierung (nur clientseitig geladen). */
@Mixin(Entity.class)
abstract class EntityMixin {
	@Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
	private void mcguide$glowColor(CallbackInfoReturnable<Integer> cir) {
		Entity self = (Entity) (Object) this;
		if (self.level().isClientSide() && GuideClient.markedGlow(self)) cir.setReturnValue(GuideClient.glowColor(self));
	}
}
