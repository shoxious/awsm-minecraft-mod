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

/**
 * Gelaende-Modus: wer einen vollen Block (Erde, Stein, Sand ...) setzt, bekommt mehr davon dazu.
 * Oben auf den Boden gesetzt waechst ein runder Huegel (Mitte am hoechsten, leicht unregelmaessig,
 * bei abfallendem Boden bis unten aufgefuellt); an Waenden oder mit "flach" nur eine runde Lage auf
 * derselben Ebene, dort wo dahinter schon etwas steht. Laeuft per normaler Platzier-Klicks, also auch
 * auf Vanilla-Servern.
 */
final class Terrain {
	static final int MAX_RADIUS = 4;
	/** So tief wird unter einem Huegel bis zum Boden aufgefuellt. */
	private static final int FILL_DOWN = 3;
	/** Hoechstens ein Stapel pro gesetztem Block. */
	private static final int MAX_BLOCKS = 64;

	/** Vom Spieler gesetzter Block, wird im naechsten Tick geprueft (erst dann steht er wirklich). */
	private record Pending(BlockPos pos, Direction face, Block block) {}

	private static Pending pending;
	private static boolean placing;

	static void register() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (!level.isClientSide() || placing || !GuideClient.settings.terrainMode || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
			Block block = brushBlock(player.getMainHandItem());
			if (block == null) return InteractionResult.PASS;
			BlockPos pos = level.getBlockState(hit.getBlockPos()).canBeReplaced() ? hit.getBlockPos() : hit.getBlockPos().relative(hit.getDirection());
			pending = new Pending(pos, hit.getDirection(), block);
			return InteractionResult.PASS;
		});
	}

	/** Nur volle Bloecke ohne Inhalt - keine Kisten, Fackeln, Treppen o. ae. */
	private static Block brushBlock(ItemStack stack) {
		if (!(stack.getItem() instanceof BlockItem bi)) return null;
		Block b = bi.getBlock();
		return b instanceof EntityBlock || !b.defaultBlockState().canOcclude() ? null : b;
	}

	static void tick(Minecraft mc) {
		Pending p = pending;
		pending = null;
		LocalPlayer player = mc.player;
		if (p == null || player == null || mc.level == null || mc.gameMode == null) return;
		if (!mc.level.getBlockState(p.pos()).is(p.block())) return; // Setzen hat nicht geklappt
		boolean hill = GuideClient.settings.terrainHill && p.face() == Direction.UP;
		placing = true;
		try {
			int placed = 0;
			for (BlockPos t : hill ? hill(mc, p) : brush(p)) {
				if (placed >= MAX_BLOCKS || player.getMainHandItem().isEmpty() || brushBlock(player.getMainHandItem()) != p.block()) break;
				if (place(mc, player, t, hill ? Direction.UP : p.face())) placed++;
			}
		} finally {
			placing = false;
		}
	}

	/** Kreisflaeche in der Ebene quer zur angeklickten Seite, von innen nach aussen. */
	private static List<BlockPos> brush(Pending p) {
		int r = GuideClient.settings.terrainRadius;
		Direction.Axis axis = p.face().getAxis();
		List<BlockPos> out = new ArrayList<>();
		for (int a = -r; a <= r; a++) {
			for (int b = -r; b <= r; b++) {
				if ((a == 0 && b == 0) || a * a + b * b > r * r + r) continue;
				out.add(switch (axis) {
					case Y -> p.pos().offset(a, 0, b);
					case X -> p.pos().offset(0, a, b);
					case Z -> p.pos().offset(a, b, 0);
				});
			}
		}
		out.sort(Comparator.comparingDouble(t -> t.distSqr(p.pos())));
		return out;
	}

	/**
	 * Runder Huegel um den gesetzten Block: Hoehe faellt zum Rand hin ab (Kuppel), dazu etwas Zufall,
	 * der fest an der Position haengt. Unter jeder Saeule wird bis zum Boden aufgefuellt.
	 * Reihenfolge: Lage fuer Lage von unten, damit jeder Block auf dem vorigen steht.
	 */
	private static List<BlockPos> hill(Minecraft mc, Pending p) {
		int r = GuideClient.settings.terrainRadius;
		int peak = r + 1;
		BlockPos c = p.pos();
		List<BlockPos> out = new ArrayList<>();
		for (int a = -r; a <= r; a++) {
			for (int b = -r; b <= r; b++) {
				double d = Math.sqrt(a * a + b * b) / (r + 0.5);
				if (d > 1) continue;
				int h = (int) Math.round(peak * (1 - d * d));
				if (a != 0 || b != 0) h += jitter(c.offset(a, 0, b));
				if (h < 1) continue;
				int bottom = c.getY();
				while (bottom > c.getY() - FILL_DOWN && mc.level.getBlockState(new BlockPos(c.getX() + a, bottom - 1, c.getZ() + b)).canBeReplaced()) bottom--;
				for (int y = bottom; y < c.getY() + h; y++) out.add(new BlockPos(c.getX() + a, y, c.getZ() + b));
			}
		}
		out.sort(Comparator.comparingInt((BlockPos t) -> t.getY()).thenComparingDouble(t -> t.distSqr(c)));
		return out;
	}

	/** -1, 0 oder +1, immer gleich fuer dieselbe Stelle. */
	private static int jitter(BlockPos pos) {
		long h = pos.asLong() * 0x9E3779B97F4A7C15L;
		return switch ((int) (h >>> 62)) {
			case 0 -> -1;
			case 3 -> 1;
			default -> 0;
		};
	}

	private static boolean place(Minecraft mc, LocalPlayer player, BlockPos t, Direction face) {
		if (!mc.level.getBlockState(t).canBeReplaced()) return false;
		BlockPos support = t.relative(face.getOpposite());
		if (!mc.level.getBlockState(support).isFaceSturdy(mc.level, support, face)) return false;
		if (!player.isWithinBlockInteractionRange(t, 0) || new AABB(t).intersects(player.getBoundingBox())) return false;
		Vec3 hitVec = Vec3.atCenterOf(support).add(face.getUnitVec3().scale(0.5));
		return mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, new BlockHitResult(hitVec, face, support, false)).consumesAction();
	}

	static void toggle(Minecraft mc) {
		setEnabled(mc, !GuideClient.settings.terrainMode);
	}

	static void setEnabled(Minecraft mc, boolean on) {
		GuideState s = GuideClient.settings;
		s.terrainMode = on;
		s.save();
		mc.player.sendOverlayMessage(Component.literal("Gelände-Modus " + (on ? "an (" + (s.terrainHill ? "Hügel" : "flach") + ", Radius " + s.terrainRadius + ")" : "aus"))
			.withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY));
	}

	static void setHill(Minecraft mc, boolean hill) {
		GuideClient.settings.terrainHill = hill;
		setEnabled(mc, true);
	}

	static void setRadius(Minecraft mc, int r) {
		GuideClient.settings.terrainRadius = r;
		setEnabled(mc, true);
	}

	private Terrain() {}
}
