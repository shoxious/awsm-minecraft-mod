package de.shoxi.mcguide;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Gelaende-Werkzeug, rein clientseitig: zwei Ecken markieren, dann fuellen, abtragen, einebnen oder einen Huegel
 * aufschuetten. Gesetzt wird der Block aus der Hand (Nachschub aus der Hotbar), nur in Reichweite und in normalem
 * Tempo; der Server sieht nur gewoehnliche Klicks. Der Spieler laeuft dabei durch das Gebiet.
 */
final class Terrain {
	private static final int MAX_VOLUME = 64 * 64 * 32;
	/** Ticks zwischen zwei gesetzten Bloecken. */
	private static final int PLACE_DELAY = 2;

	private static BlockPos corner1, corner2;
	private static final List<BlockPos> toPlace = new ArrayList<>();
	private static final List<BlockPos> toBreak = new ArrayList<>();
	private static Item block;
	private static BlockPos current;
	private static int cooldown;
	private static String job;

	static boolean hasArea() {
		return corner1 != null && corner2 != null;
	}

	static void corner(Minecraft mc, int n) {
		LocalPlayer p = mc.player;
		if (p == null) return;
		BlockPos pos = mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
			? hit.getBlockPos() : p.blockPosition().below();
		if (n == 1) corner1 = pos;
		else corner2 = pos;
		msg(p, "Ecke " + n + ": " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + sizeInfo(), ChatFormatting.AQUA);
	}

	private static String sizeInfo() {
		if (corner1 == null || corner2 == null) return "";
		return " (" + (Math.abs(corner1.getX() - corner2.getX()) + 1) + "×" + (Math.abs(corner1.getZ() - corner2.getZ()) + 1)
			+ ", Höhe " + (Math.abs(corner1.getY() - corner2.getY()) + 1) + ")";
	}

	private static boolean ready(LocalPlayer p) {
		if (corner1 == null || corner2 == null) {
			msg(p, "Erst zwei Ecken markieren: /gelaende ecke1 und /gelaende ecke2 (auf einen Block schauen)", ChatFormatting.YELLOW);
			return false;
		}
		long vol = (long) (Math.abs(corner1.getX() - corner2.getX()) + 1) * (Math.abs(corner1.getY() - corner2.getY()) + 1)
			* (Math.abs(corner1.getZ() - corner2.getZ()) + 1);
		if (vol > MAX_VOLUME) {
			msg(p, "Bereich zu groß (" + vol + " Blöcke, max. " + MAX_VOLUME + ") – teil ihn auf", ChatFormatting.YELLOW);
			return false;
		}
		return true;
	}

	private static boolean takeBlock(LocalPlayer p) {
		if (!(p.getMainHandItem().getItem() instanceof BlockItem)) {
			msg(p, "Nimm den Block, mit dem gebaut werden soll, in die Hand", ChatFormatting.YELLOW);
			return false;
		}
		block = p.getMainHandItem().getItem();
		return true;
	}

	/** Alles Leere (Luft, Wasser, Gras) im Bereich mit dem Block aus der Hand fuellen. */
	static void fill(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null || !ready(p) || !takeBlock(p)) return;
		start();
		forBox((x, y, z) -> {
			BlockPos pos = new BlockPos(x, y, z);
			if (mc.level.getBlockState(pos).canBeReplaced()) toPlace.add(pos);
		});
		begin(p, "Füllen");
	}

	/** Alles im Bereich abbauen. */
	static void clear(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null || !ready(p)) return;
		start();
		forBox((x, y, z) -> addBreak(mc, new BlockPos(x, y, z)));
		begin(p, "Abtragen");
	}

	/** Einebnen auf Hoehe der unteren Ecke: darueber abtragen, Luecken bis dorthin auffuellen. */
	static void flatten(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null || !ready(p) || !takeBlock(p)) return;
		int level = Math.min(corner1.getY(), corner2.getY());
		int top = Math.max(Math.max(corner1.getY(), corner2.getY()), level + 1);
		start();
		int x0 = Math.min(corner1.getX(), corner2.getX()), x1 = Math.max(corner1.getX(), corner2.getX());
		int z0 = Math.min(corner1.getZ(), corner2.getZ()), z1 = Math.max(corner1.getZ(), corner2.getZ());
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				for (int y = level + 1; y <= top; y++) addBreak(mc, new BlockPos(x, y, z));
				// Loecher unter der Flaeche bis zu 8 Bloecke tief auffuellen
				for (int y = level; y >= level - 8; y--) {
					BlockPos pos = new BlockPos(x, y, z);
					if (!mc.level.getBlockState(pos).canBeReplaced()) break;
					toPlace.add(pos);
				}
			}
		}
		begin(p, "Einebnen auf Y " + level);
	}

	/** Huegel: ab der unteren Ecke aufschuetten, in der Mitte so hoch wie angegeben, zum Rand weich auslaufend. */
	static void hill(Minecraft mc, int height) {
		LocalPlayer p = mc.player;
		if (p == null || !ready(p) || !takeBlock(p)) return;
		int level = Math.min(corner1.getY(), corner2.getY());
		int x0 = Math.min(corner1.getX(), corner2.getX()), x1 = Math.max(corner1.getX(), corner2.getX());
		int z0 = Math.min(corner1.getZ(), corner2.getZ()), z1 = Math.max(corner1.getZ(), corner2.getZ());
		double cx = (x0 + x1) / 2.0, cz = (z0 + z1) / 2.0, rx = (x1 - x0) / 2.0 + 0.5, rz = (z1 - z0) / 2.0 + 0.5;
		start();
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				double d = Math.sqrt(Math.pow((x - cx) / rx, 2) + Math.pow((z - cz) / rz, 2));
				if (d >= 1) continue;
				int h = (int) Math.round(height * (0.5 + 0.5 * Math.cos(Math.PI * d)));
				for (int y = level; y < level + h; y++) {
					BlockPos pos = new BlockPos(x, y, z);
					if (mc.level.getBlockState(pos).canBeReplaced()) toPlace.add(pos);
				}
			}
		}
		begin(p, "Hügel (" + height + " hoch)");
	}

	private interface BoxVisitor {
		void visit(int x, int y, int z);
	}

	private static void forBox(BoxVisitor v) {
		for (int x = Math.min(corner1.getX(), corner2.getX()); x <= Math.max(corner1.getX(), corner2.getX()); x++) {
			for (int y = Math.min(corner1.getY(), corner2.getY()); y <= Math.max(corner1.getY(), corner2.getY()); y++) {
				for (int z = Math.min(corner1.getZ(), corner2.getZ()); z <= Math.max(corner1.getZ(), corner2.getZ()); z++) v.visit(x, y, z);
			}
		}
	}

	private static void addBreak(Minecraft mc, BlockPos pos) {
		if (breakable(mc, pos)) toBreak.add(pos);
	}

	/** Abbaubar: kein Luft-, Fluessigkeits- oder unzerstoerbarer Block. */
	private static boolean breakable(Minecraft mc, BlockPos pos) {
		BlockState s = mc.level.getBlockState(pos);
		return !s.isAir() && !(s.getFluidState().isSource() || !s.getFluidState().isEmpty() && s.canBeReplaced())
			&& s.getDestroySpeed(mc.level, pos) >= 0 && !s.is(Blocks.BEDROCK);
	}

	private static void start() {
		toPlace.clear();
		toBreak.clear();
		current = null;
	}

	private static void begin(LocalPlayer p, String name) {
		job = name;
		if (toPlace.isEmpty() && toBreak.isEmpty()) {
			msg(p, name + ": nichts zu tun", ChatFormatting.GRAY);
			return;
		}
		msg(p, name + ": " + (toBreak.isEmpty() ? "" : toBreak.size() + " abbauen") + (toBreak.isEmpty() || toPlace.isEmpty() ? "" : ", ")
			+ (toPlace.isEmpty() ? "" : toPlace.size() + " setzen") + " – lauf durch den Bereich, /gelaende stop bricht ab", ChatFormatting.GREEN);
	}

	static void cancel() {
		toPlace.clear();
		toBreak.clear();
		current = null;
	}

	static void stop(Minecraft mc) {
		cancel();
		if (mc.player != null) msg(mc.player, "Gelände-Werkzeug gestoppt", ChatFormatting.GRAY);
	}

	static boolean busy(Minecraft mc) {
		return current != null && !mc.options.keyAttack.isDown();
	}

	static String status() {
		if (toPlace.isEmpty() && toBreak.isEmpty()) return null;
		return "⛰ " + job + ": " + (toBreak.size() + toPlace.size()) + " übrig";
	}

	static void tick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null || mc.level == null) return;
		drawBox();
		if (toPlace.isEmpty() && toBreak.isEmpty() || mc.gameMode == null) return;
		if (mc.gui.screen() != null || mc.options.keyAttack.isDown() || mc.options.keyUse.isDown()) {
			current = null;
			return;
		}
		// erst abtragen, dann (von unten nach oben) setzen
		if (!toBreak.isEmpty()) {
			toBreak.removeIf(pos -> !breakable(mc, pos));
			if (current == null || !toBreak.contains(current) || !p.isWithinBlockInteractionRange(current, 0.0)) current = nearest(p, toBreak, true);
			if (current == null) return;
			Direction side = Direction.getApproximateNearest(p.getEyePosition().subtract(Vec3.atCenterOf(current)));
			if (mc.gameMode.continueDestroyBlock(current, side)) p.swing(InteractionHand.MAIN_HAND, p.getMainHandItem().getAttackAnimation(), false);
			if (toBreak.isEmpty()) current = null;
			return;
		}
		current = null;
		if (cooldown > 0) {
			cooldown--;
			return;
		}
		toPlace.removeIf(pos -> !mc.level.getBlockState(pos).canBeReplaced());
		if (toPlace.isEmpty()) {
			msg(p, job + " fertig", ChatFormatting.GREEN);
			return;
		}
		int slot = blockSlot(p);
		if (slot < 0) {
			msg(p, "Keine " + new net.minecraft.world.item.ItemStack(block).getHoverName().getString() + " mehr in der Hotbar – nachfüllen, dann geht es weiter", ChatFormatting.YELLOW);
			cooldown = 100;
			return;
		}
		AABB body = p.getBoundingBox();
		BlockPos best = null;
		Direction bestFace = null;
		double bestScore = Double.MAX_VALUE;
		Vec3 eye = p.getEyePosition();
		for (BlockPos pos : toPlace) {
			if (!p.isWithinBlockInteractionRange(pos, 0.0) || body.intersects(new AABB(pos))) continue;
			Direction face = supportFace(mc, pos);
			if (face == null) continue;
			double score = pos.getY() * 1000 + eye.distanceToSqr(Vec3.atCenterOf(pos)); // unten zuerst
			if (score < bestScore) {
				bestScore = score;
				best = pos;
				bestFace = face;
			}
		}
		if (best == null) return;
		BlockPos support = best.relative(bestFace);
		Direction clicked = bestFace.getOpposite();
		Inventory inv = p.getInventory();
		inv.setSelectedSlot(slot);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(support).add(Vec3.atLowerCornerOf(clicked.getUnitVec3i()).scale(0.5)), clicked, support, false);
		if (mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, hit).consumesAction()) {
			p.swing(InteractionHand.MAIN_HAND, p.getMainHandItem().getAttackAnimation(), false);
		}
		cooldown = PLACE_DELAY;
	}

	/** Richtung zu einem festen Nachbarn, an den der neue Block gesetzt werden kann (unten bevorzugt). */
	private static Direction supportFace(Minecraft mc, BlockPos pos) {
		for (Direction d : new Direction[] {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP}) {
			if (!mc.level.getBlockState(pos.relative(d)).canBeReplaced()) return d;
		}
		return null;
	}

	private static int blockSlot(LocalPlayer p) {
		Inventory inv = p.getInventory();
		if (inv.getSelectedItem().is(block)) return inv.getSelectedSlot();
		for (int i = 0; i < Inventory.getSelectionSize(); i++) if (inv.getItem(i).is(block)) return i;
		return -1;
	}

	private static BlockPos nearest(LocalPlayer p, List<BlockPos> list, boolean topFirst) {
		BlockPos best = null;
		double bestScore = Double.MAX_VALUE;
		Vec3 eye = p.getEyePosition();
		for (BlockPos pos : list) {
			if (!p.isWithinBlockInteractionRange(pos, 0.0)) continue;
			double score = (topFirst ? -pos.getY() * 1000 : 0) + eye.distanceToSqr(Vec3.atCenterOf(pos));
			if (score < bestScore) {
				bestScore = score;
				best = pos;
			}
		}
		return best;
	}

	/** Rahmen um den markierten Bereich. */
	private static void drawBox() {
		if (corner1 == null) return;
		BlockPos c2 = corner2 != null ? corner2 : corner1;
		double x0 = Math.min(corner1.getX(), c2.getX()), x1 = Math.max(corner1.getX(), c2.getX()) + 1;
		double y0 = Math.min(corner1.getY(), c2.getY()), y1 = Math.max(corner1.getY(), c2.getY()) + 1;
		double z0 = Math.min(corner1.getZ(), c2.getZ()), z1 = Math.max(corner1.getZ(), c2.getZ()) + 1;
		int color = 0xFFFFB040;
		double[][] xs = {{x0, x1}}, ys = {{y0, y1}}, zs = {{z0, z1}};
		for (double y : ys[0]) for (double z : zs[0]) Gizmos.line(new Vec3(x0, y, z), new Vec3(x1, y, z), color, 2f);
		for (double x : xs[0]) for (double z : zs[0]) Gizmos.line(new Vec3(x, y0, z), new Vec3(x, y1, z), color, 2f);
		for (double x : xs[0]) for (double y : ys[0]) Gizmos.line(new Vec3(x, y, z0), new Vec3(x, y, z1), color, 2f);
	}

	static void forget(Minecraft mc) {
		cancel();
		corner1 = corner2 = null;
		if (mc.player != null) msg(mc.player, "Markierung gelöscht", ChatFormatting.GRAY);
	}

	private static void msg(LocalPlayer p, String s, ChatFormatting color) {
		p.sendSystemMessage(Component.literal("⛰ " + s).withStyle(color));
	}

	private Terrain() {}
}
