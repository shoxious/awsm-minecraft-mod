package de.shoxi.mcguide;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Gelände-Pinsel: ein gesetzter Block wird zur runden Fläche bzw. zum Hügel (Taste/Menü, /gelaende 1-4). */
final class TerrainBrush {
	static final int MAX_RADIUS = 4;
	private static final int FILL_DOWN = 3;
	private static final int MAX_BLOCKS = 64;
	private static TerrainBrush.Pending pending;
	private static boolean placing;

	static void register() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (level.isClientSide() && !placing && GuideClient.settings.terrainMode && hand == InteractionHand.MAIN_HAND) {
				Block block = brushBlock(player.getMainHandItem());
				if (block == null) {
					return InteractionResult.PASS;
				} else {
					BlockPos pos = level.getBlockState(hit.getBlockPos()).canBeReplaced() ? hit.getBlockPos() : hit.getBlockPos().relative(hit.getDirection());
					pending = new TerrainBrush.Pending(pos, hit.getDirection(), block);
					return InteractionResult.PASS;
				}
			} else {
				return InteractionResult.PASS;
			}
		});
	}

	private static Block brushBlock(ItemStack stack) {
		if (!(stack.getItem() instanceof BlockItem bi)) {
			return null;
		} else {
			Block b = bi.getBlock();
			return !(b instanceof EntityBlock) && b.defaultBlockState().canOcclude() ? b : null;
		}
	}

	static void tick(Minecraft mc) {
		TerrainBrush.Pending p = pending;
		pending = null;
		LocalPlayer player = mc.player;
		if (p != null && player != null && mc.level != null && mc.gameMode != null) {
			if (mc.level.getBlockState(p.pos()).is(p.block())) {
				boolean hill = GuideClient.settings.terrainHill && p.face() == Direction.UP;
				placing = true;

				try {
					int placed = 0;

					for (BlockPos t : hill ? hill(mc, p) : brush(p)) {
						if (placed >= 64 || player.getMainHandItem().isEmpty() || brushBlock(player.getMainHandItem()) != p.block()) {
							break;
						}

						if (place(mc, player, t, hill ? Direction.UP : p.face())) {
							placed++;
						}
					}
				} finally {
					placing = false;
				}
			}
		}
	}

	private static List<BlockPos> brush(TerrainBrush.Pending p) {
		int r = GuideClient.settings.terrainRadius;
		Axis axis = p.face().getAxis();
		List<BlockPos> out = new ArrayList<>();

		for (int a = -r; a <= r; a++) {
			for (int b = -r; b <= r; b++) {
				if ((a != 0 || b != 0) && a * a + b * b <= r * r + r) {
					out.add(switch (axis) {
						case Y -> p.pos().offset(a, 0, b);
						case X -> p.pos().offset(0, a, b);
						case Z -> p.pos().offset(a, b, 0);
					});
				}
			}
		}

		out.sort(Comparator.comparingDouble(t -> t.distSqr(p.pos())));
		return out;
	}

	private static List<BlockPos> hill(Minecraft mc, TerrainBrush.Pending p) {
		int r = GuideClient.settings.terrainRadius;
		int peak = r + 1;
		BlockPos c = p.pos();
		List<BlockPos> out = new ArrayList<>();

		for (int a = -r; a <= r; a++) {
			for (int b = -r; b <= r; b++) {
				double d = Math.sqrt(a * a + b * b) / (r + 0.5);
				if (!(d > 1.0)) {
					int h = (int)Math.round(peak * (1.0 - d * d));
					if (a != 0 || b != 0) {
						h += jitter(c.offset(a, 0, b));
					}

					if (h >= 1) {
						int bottom = c.getY();

						while (bottom > c.getY() - 3 && mc.level.getBlockState(new BlockPos(c.getX() + a, bottom - 1, c.getZ() + b)).canBeReplaced()) {
							bottom--;
						}

						for (int y = bottom; y < c.getY() + h; y++) {
							out.add(new BlockPos(c.getX() + a, y, c.getZ() + b));
						}
					}
				}
			}
		}

		out.sort(Comparator.<BlockPos>comparingInt(t -> t.getY()).thenComparingDouble(t -> t.distSqr(c)));
		return out;
	}

	private static int jitter(BlockPos pos) {
		long h = pos.asLong() * -7046029254386353131L;

		return switch ((int)(h >>> 62)) {
			case 0 -> -1;
			case 3 -> 1;
			default -> 0;
		};
	}

	private static boolean place(Minecraft mc, LocalPlayer player, BlockPos t, Direction face) {
		if (!mc.level.getBlockState(t).canBeReplaced()) {
			return false;
		} else {
			BlockPos support = t.relative(face.getOpposite());
			if (!mc.level.getBlockState(support).isFaceSturdy(mc.level, support, face)) {
				return false;
			} else if (player.isWithinBlockInteractionRange(t, 0.0) && !new AABB(t).intersects(player.getBoundingBox())) {
				Vec3 hitVec = Vec3.atCenterOf(support).add(face.getUnitVec3().scale(0.5));
				return mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, new BlockHitResult(hitVec, face, support, false)).consumesAction();
			} else {
				return false;
			}
		}
	}

	static void toggle(Minecraft mc) {
		setEnabled(mc, !GuideClient.settings.terrainMode);
	}

	static void setEnabled(Minecraft mc, boolean on) {
		GuideState s = GuideClient.settings;
		s.terrainMode = on;
		s.save();
		mc.player
			.sendOverlayMessage(
				Component.literal("Gelände-Modus " + (on ? "an (" + (s.terrainHill ? "Hügel" : "flach") + ", Radius " + s.terrainRadius + ")" : "aus"))
					.withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY)
			);
	}

	static void setHill(Minecraft mc, boolean hill) {
		GuideClient.settings.terrainHill = hill;
		setEnabled(mc, true);
	}

	static void setRadius(Minecraft mc, int r) {
		GuideClient.settings.terrainRadius = r;
		setEnabled(mc, true);
	}

	private TerrainBrush() {
	}

	private record Pending(BlockPos pos, Direction face, Block block) {
	}
}
