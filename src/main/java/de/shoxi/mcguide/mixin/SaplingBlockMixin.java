package de.shoxi.mcguide.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.shoxi.mcguide.GiantTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;

/** Faengt das Wachsen eines Setzlings ab, wenn er Teil eines 4x4-, 8x8- oder 16x16-Felds ist. */
@Mixin(SaplingBlock.class)
public abstract class SaplingBlockMixin {
	@Shadow
	@Final
	protected TreeGrower treeGrower;

	@Inject(method = "advanceTree", at = @At("HEAD"), cancellable = true)
	private void mcguide$giantTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random, CallbackInfo ci) {
		// Stufe 0 schaltet nur auf Stufe 1, erst danach waechst der Baum
		if (state.getValue(SaplingBlock.STAGE) == 0) return;
		if (GiantTrees.tryGrow(level, pos, state, treeGrower, random)) ci.cancel();
	}
}
