package de.shoxi.mcguide;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Licht-Anzeige: markiert Bloecke, auf denen Monster spawnen koennen (Blocklicht 0).
 * Rot = auch tagsueber (kein Himmel), gelb = nur nachts (unter freiem Himmel).
 */
final class LightOverlay {
	private record Spot(BlockPos pos, boolean night) {}

	private static final int RADIUS = 14;
	private static final int HEIGHT = 6;
	private static List<Spot> spots = List.of();
	private static int ticks;

	static void toggle(Minecraft mc) {
		GuideState s = GuideClient.settings;
		s.lightOverlay = !s.lightOverlay;
		s.save();
		spots = List.of();
		ticks = 0;
		mc.player.sendOverlayMessage(Component.literal("Licht-Anzeige " + (s.lightOverlay ? "an (rot = immer, gelb = nachts)" : "aus"))
			.withStyle(s.lightOverlay ? ChatFormatting.GREEN : ChatFormatting.GRAY));
	}

	static void tick(Minecraft mc) {
		if (!GuideClient.settings.lightOverlay || mc.player == null || mc.level == null) {
			spots = List.of();
			return;
		}
		if (ticks++ % 10 == 0) scan(mc);
		for (Spot s : spots) {
			double x = s.pos().getX(), y = s.pos().getY(), z = s.pos().getZ();
			int c = s.night() ? 0xFFFFD000 : 0xFFFF3030;
			Gizmos.cuboid(new AABB(x + 0.3, y + 0.02, z + 0.3, x + 0.7, y + 0.05, z + 0.7), GizmoStyle.strokeAndFill(c, 1.5f, (c & 0x00FFFFFF) | 0x60000000));
		}
	}

	private static void scan(Minecraft mc) {
		BlockPos c = mc.player.blockPosition();
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();
		List<Spot> found = new ArrayList<>();
		for (int dx = -RADIUS; dx <= RADIUS; dx++) {
			for (int dz = -RADIUS; dz <= RADIUS; dz++) {
				if (dx * dx + dz * dz > RADIUS * RADIUS) continue;
				for (int dy = -HEIGHT; dy <= HEIGHT; dy++) {
					p.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
					if (!spawnable(mc, p, below)) continue;
					found.add(new Spot(p.immutable(), mc.level.getBrightness(LightLayer.SKY, p) > 0));
				}
			}
		}
		spots = found;
	}

	/** Grob wie Vanilla: fester Boden, 2 Bloecke frei, kein Blocklicht. */
	static boolean spawnable(Minecraft mc, BlockPos pos, BlockPos.MutableBlockPos below) {
		BlockState here = mc.level.getBlockState(pos);
		if (!here.isAir() && !here.canBeReplaced()) return false;
		if (!here.getFluidState().isEmpty()) return false;
		BlockState head = mc.level.getBlockState(pos.above());
		if (!head.isAir() && !head.canBeReplaced()) return false;
		below.setWithOffset(pos, Direction.DOWN);
		BlockState ground = mc.level.getBlockState(below);
		if (!ground.isFaceSturdy(mc.level, below, Direction.UP) || ground.is(BlockTags.LEAVES)) return false;
		return mc.level.getBrightness(LightLayer.BLOCK, pos) == 0;
	}

	private LightOverlay() {}
}
