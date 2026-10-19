package de.shoxi.mcguide;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Auto-Faeller und Ader-Abbau, rein clientseitig: Schleichen + Axt + Stamm (bzw. Spitzhacke + Erz) abbauen,
 * danach baut der Client die verbundenen Bloecke in Reichweite einzeln ab (normales Abbautempo, der Server sieht nur Klicks).
 * Baeume auf Servern mit Guide-Mod nicht, dort faellt Tree Harvester den Baum.
 */
final class TreeFeller {
	private static final int MAX_LOGS = 512;
	private static final int MAX_SPREAD = 12;
	private static final int MAX_ORES = 64;
	private static final int ORE_SPREAD = 8;
	/** So viele Ticks warten, wenn kein Stamm in Reichweite ist (Spieler kann hinlaufen). */
	private static final int IDLE_LIMIT = 20 * 20;

	private static final List<BlockPos> logs = new ArrayList<>();
	private static Predicate<BlockState> match;
	private static TagKey<Item> tool;
	private static boolean ore;
	private static BlockPos current;
	private static int idle;
	/** Setzling nach dem Faellen an die Schnittstelle (wenn darunter Erde ist). */
	private static BlockPos saplingPos;
	private static Item saplingItem;
	private static int saplingTicks;

	/** Nach einem Abbau durch den Spieler (Fabric ClientPlayerBlockBreakEvents.AFTER). */
	static void onBreak(Minecraft mc, LocalPlayer player, BlockPos pos, BlockState state) {
		if (!GuideClient.settings.treeFeller || !logs.isEmpty() || !player.isShiftKeyDown()) return;
		ItemStack hand = player.getMainHandItem();
		Block block = state.getBlock();
		OreFinder.Ore kind = OreFinder.oreOf(block);
		saplingPos = null;
		if (state.is(BlockTags.LOGS) && hand.is(ItemTags.AXES)) {
			if (GuideClient.synced) return;
			ore = false;
			match = s -> s.is(block);
			tool = ItemTags.AXES;
			Identifier id = BuiltInRegistries.BLOCK.getKey(block);
			saplingItem = BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath().replace("_log", "_sapling"))).orElse(null);
		} else if (kind != null && hand.is(ItemTags.PICKAXES)) {
			// gleiche Erzsorte, auch Tiefenschiefer-Variante
			ore = true;
			match = s -> OreFinder.oreOf(s.getBlock()) == kind;
			tool = ItemTags.PICKAXES;
			saplingItem = null;
		} else {
			return;
		}
		collect(mc, pos);
		if (saplingItem != null) saplingPos = pos;
		if (logs.isEmpty()) return;
		idle = 0;
		current = null;
		player.sendOverlayMessage(Component.literal(ore ? "Ader: " + logs.size() + " Erze" : "Fällen: " + logs.size() + " Stämme").withStyle(ChatFormatting.GREEN));
	}

	/** Verbundene Bloecke (auch diagonal); Staemme nur ab Schnitthoehe aufwaerts, Erze in alle Richtungen. */
	private static void collect(Minecraft mc, BlockPos start) {
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> open = new ArrayDeque<>();
		open.add(start);
		seen.add(start);
		int max = ore ? MAX_ORES : MAX_LOGS;
		int spread = ore ? ORE_SPREAD : MAX_SPREAD;
		while (!open.isEmpty() && logs.size() < max) {
			BlockPos p = open.poll();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = ore ? -1 : 0; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						BlockPos n = p.offset(dx, dy, dz);
						if (Math.abs(n.getX() - start.getX()) > spread || Math.abs(n.getZ() - start.getZ()) > spread
							|| ore && Math.abs(n.getY() - start.getY()) > spread) continue;
						if (!seen.add(n) || !match.test(mc.level.getBlockState(n))) continue;
						logs.add(n);
						open.add(n);
					}
				}
			}
		}
	}

	static boolean busy(Minecraft mc) {
		return current != null && !mc.options.keyAttack.isDown();
	}

	/** HUD-Text, z.B. "🪓 Fällen: 12 Stämme übrig". */
	static String status() {
		return ore ? "⛏ Ader: " + logs.size() + " Erze übrig" : "🪓 Fällen: " + logs.size() + " Stämme übrig";
	}

	static int remaining() {
		return logs.size();
	}

	static void cancel() {
		logs.clear();
		current = null;
	}

	/** Bis zu eine Minute lang versuchen, einen Setzling zu setzen (Spieler muss in Reichweite sein). */
	private static void replant(Minecraft mc) {
		if (saplingPos == null || saplingTicks <= 0) return;
		saplingTicks--;
		LocalPlayer player = mc.player;
		if (!GuideClient.settings.saplings || player == null || mc.level == null || mc.gameMode == null) {
			saplingPos = null;
			return;
		}
		if (mc.gui.screen() != null || mc.options.keyAttack.isDown() || mc.options.keyUse.isDown()) return;
		BlockPos below = saplingPos.below();
		if (!mc.level.getBlockState(saplingPos).isAir()) {
			saplingPos = null;
			return;
		}
		if (!mc.level.getBlockState(below).is(BlockTags.DIRT) || !player.isWithinBlockInteractionRange(below, 0.0)) return;
		Inventory inv = player.getInventory();
		int slot = -1;
		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			if (inv.getItem(i).is(saplingItem)) {
				slot = i;
				break;
			}
		}
		if (slot < 0) return;
		int old = inv.getSelectedSlot();
		inv.setSelectedSlot(slot);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(below).add(0, 0.5, 0), Direction.UP, below, false);
		if (mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit).consumesAction()) {
			player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getAttackAnimation(), false);
		}
		inv.setSelectedSlot(old);
		saplingPos = null;
	}

	static void tick(Minecraft mc) {
		if (logs.isEmpty()) {
			replant(mc);
			return;
		}
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null || mc.gameMode == null) {
			cancel();
			return;
		}
		// Spieler hackt selbst oder hat ein Menue offen: kurz Pause
		if (mc.gui.screen() != null || mc.options.keyAttack.isDown()) {
			current = null;
			return;
		}
		ItemStack axe = player.getMainHandItem();
		if (!axe.is(tool)) {
			stop(player, (ore ? "Abbau" : "Fällen") + " abgebrochen (" + (ore ? "keine Spitzhacke" : "keine Axt") + " in der Hand)");
			return;
		}
		if (axe.isDamageableItem() && axe.getMaxDamage() - axe.getDamageValue() <= 3) {
			stop(player, (ore ? "Abbau" : "Fällen") + " abgebrochen (Werkzeug fast kaputt)");
			return;
		}
		logs.removeIf(p -> !match.test(mc.level.getBlockState(p)));
		if (logs.isEmpty()) {
			current = null;
			saplingTicks = 20 * 60;
			player.sendOverlayMessage(Component.literal(ore ? "Ader abgebaut" : "Baum gefällt").withStyle(ChatFormatting.GREEN));
			return;
		}
		if (current == null || !logs.contains(current) || !player.isWithinBlockInteractionRange(current, 0.0)) {
			current = nearestReachable(player);
		}
		if (current == null) {
			if (++idle > IDLE_LIMIT) stop(player, "Fertig (" + logs.size() + " " + (ore ? "Erze" : "Stämme") + " außer Reichweite)");
			return;
		}
		idle = 0;
		Vec3 eye = player.getEyePosition();
		Direction side = Direction.getApproximateNearest(eye.subtract(Vec3.atCenterOf(current)));
		// continueDestroyBlock beginnt bei neuem Ziel selbst neu und haelt die Abbaupause ein
		if (mc.gameMode.continueDestroyBlock(current, side)) {
			player.swing(InteractionHand.MAIN_HAND, axe.getAttackAnimation(), false);
		}
	}

	private static BlockPos nearestReachable(LocalPlayer player) {
		BlockPos best = null;
		double bestDist = Double.MAX_VALUE;
		Vec3 eye = player.getEyePosition();
		for (BlockPos p : logs) {
			if (!player.isWithinBlockInteractionRange(p, 0.0)) continue;
			double d = eye.distanceToSqr(Vec3.atCenterOf(p));
			if (d < bestDist) {
				bestDist = d;
				best = p;
			}
		}
		return best;
	}

	private static void stop(LocalPlayer player, String msg) {
		cancel();
		saplingTicks = 20 * 60;
		player.sendOverlayMessage(Component.literal(msg).withStyle(ChatFormatting.YELLOW));
	}

	private TreeFeller() {}
}
