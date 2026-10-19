package de.shoxi.mcguide;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Auto-Ernte, rein clientseitig: reife Feldfruechte in Reichweite abbauen und
 * mit Saatgut aus der Schnellleiste neu pflanzen.
 */
final class AutoCrops {
	private record Replant(BlockPos pos, Item seed, Block ground, int until) {}

	private static final Map<Block, Item> SEEDS = new IdentityHashMap<>();

	static {
		SEEDS.put(Blocks.WHEAT, Items.WHEAT_SEEDS);
		SEEDS.put(Blocks.CARROTS, Items.CARROT);
		SEEDS.put(Blocks.POTATOES, Items.POTATO);
		SEEDS.put(Blocks.BEETROOTS, Items.BEETROOT_SEEDS);
		SEEDS.put(Blocks.NETHER_WART, Items.NETHER_WART);
	}

	private static final List<Replant> replants = new ArrayList<>();
	private static int ticks;
	private static int cooldown;
	private static boolean warnedSeeds;
	private static boolean warnedBone;
	/** Leisten-Platz, in dem zuletzt Knochenmehl lag (dorthin wird nachgefuellt). */
	private static int boneSlot = -1;

	static void toggle(Minecraft mc) {
		setEnabled(mc, !GuideClient.settings.autoCrops);
	}

	static void setEnabled(Minecraft mc, boolean on) {
		GuideClient.settings.autoCrops = on;
		GuideClient.settings.save();
		replants.clear();
		warnedSeeds = false;
		warnedBone = false;
		if (mc.player != null) mc.player.sendOverlayMessage(Component.literal("Auto-Ernte " + (on ? "an" : "aus"))
			.withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY));
	}

	static void tick(Minecraft mc) {
		ticks++;
		LocalPlayer player = mc.player;
		if (!GuideClient.settings.autoCrops || player == null || mc.level == null || mc.gameMode == null) {
			replants.clear();
			return;
		}
		if (mc.gui.screen() != null || mc.options.keyAttack.isDown() || mc.options.keyUse.isDown()
			|| TreeFeller.remaining() > 0 || player.isUsingItem()) return;
		if (replant(mc, player)) return;
		if (cooldown > 0) {
			cooldown--;
			return;
		}
		if (!harvest(mc, player)) boneMeal(mc, player);
	}

	/** Pflanzt hoechstens eine Stelle pro Tick neu. */
	private static boolean replant(Minecraft mc, LocalPlayer player) {
		while (!replants.isEmpty()) {
			Replant r = replants.getFirst();
			BlockPos below = r.pos().below();
			boolean ready = mc.level.getBlockState(r.pos()).isAir() && mc.level.getBlockState(below).is(r.ground());
			if (!ready) {
				// Server hat den Abbau noch nicht bestaetigt: kurz warten, sonst verwerfen
				if (ticks > r.until() || !mc.level.getBlockState(r.pos()).isAir() && !SEEDS.containsKey(mc.level.getBlockState(r.pos()).getBlock())) {
					replants.removeFirst();
					continue;
				}
				return false;
			}
			replants.removeFirst();
			if (!player.isWithinBlockInteractionRange(below, 0.0)) continue;
			int slot = hotbarSlot(player.getInventory(), r.seed());
			if (slot < 0) {
				if (!warnedSeeds) {
					warnedSeeds = true;
					player.sendOverlayMessage(Component.literal("Auto-Ernte: Saatgut in die Schnellleiste legen").withStyle(ChatFormatting.YELLOW));
				}
				continue;
			}
			Inventory inv = player.getInventory();
			int old = inv.getSelectedSlot();
			inv.setSelectedSlot(slot);
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(below).add(0, 0.5, 0), Direction.UP, below, false);
			if (mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit).consumesAction()) {
				player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getAttackAnimation(), false);
			}
			inv.setSelectedSlot(old);
			return true;
		}
		return false;
	}

	/** Baut die naechstgelegene reife Pflanze ab (Feldfruechte brechen sofort). */
	private static boolean harvest(Minecraft mc, LocalPlayer player) {
		BlockPos center = player.blockPosition();
		Vec3 eye = player.getEyePosition();
		BlockPos best = null;
		BlockState bestState = null;
		double bestDist = Double.MAX_VALUE;
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int dx = -5; dx <= 5; dx++) {
			for (int dy = -2; dy <= 3; dy++) {
				for (int dz = -5; dz <= 5; dz++) {
					p.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
					BlockState st = mc.level.getBlockState(p);
					if (!ripe(st) || !player.isWithinBlockInteractionRange(p, 0.0)) continue;
					double d = eye.distanceToSqr(Vec3.atCenterOf(p));
					if (d < bestDist) {
						bestDist = d;
						best = p.immutable();
						bestState = st;
					}
				}
			}
		}
		if (best == null) return false;
		Block ground = mc.level.getBlockState(best.below()).getBlock();
		if (mc.gameMode.startDestroyBlock(best, Direction.UP)) {
			player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getAttackAnimation(), false);
		}
		replants.add(new Replant(best, SEEDS.get(bestState.getBlock()), ground, ticks + 40));
		cooldown = 2;
		return true;
	}

	/** Knochenmehl auf die naechste unreife Feldfrucht; leere Leiste wird aus dem Inventar nachgefuellt. */
	private static void boneMeal(Minecraft mc, LocalPlayer player) {
		if (!GuideClient.settings.boneMeal) return;
		Vec3 eye = player.getEyePosition();
		BlockPos center = player.blockPosition();
		BlockPos best = null;
		double bestDist = Double.MAX_VALUE;
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int dx = -5; dx <= 5; dx++) {
			for (int dy = -2; dy <= 3; dy++) {
				for (int dz = -5; dz <= 5; dz++) {
					p.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
					BlockState st = mc.level.getBlockState(p);
					if (!(st.getBlock() instanceof CropBlock crop) || crop.isMaxAge(st) || !SEEDS.containsKey(st.getBlock())) continue;
					if (!player.isWithinBlockInteractionRange(p, 0.0)) continue;
					double d = eye.distanceToSqr(Vec3.atCenterOf(p));
					if (d < bestDist) {
						bestDist = d;
						best = p.immutable();
					}
				}
			}
		}
		if (best == null) return;
		Inventory inv = player.getInventory();
		int slot = hotbarSlot(inv, Items.BONE_MEAL);
		if (slot < 0) {
			refillBoneMeal(mc, player);
			cooldown = 4;
			return;
		}
		boneSlot = slot;
		int old = inv.getSelectedSlot();
		inv.setSelectedSlot(slot);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(best).add(0, 0.4, 0), Direction.UP, best, false);
		if (mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit).consumesAction()) {
			player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getAttackAnimation(), false);
		}
		inv.setSelectedSlot(old);
		cooldown = 3;
	}

	/** Knochenmehl aus dem Hauptinventar per Tausch-Klick in die Leiste holen (nur ohne offenes Menue). */
	private static void refillBoneMeal(Minecraft mc, LocalPlayer player) {
		if (player.containerMenu != player.inventoryMenu) return;
		Inventory inv = player.getInventory();
		int from = -1;
		for (int i = Inventory.getSelectionSize(); i < 36; i++) {
			if (inv.getItem(i).is(Items.BONE_MEAL)) {
				from = i;
				break;
			}
		}
		if (from < 0) {
			if (!warnedBone) {
				warnedBone = true;
				player.sendOverlayMessage(Component.literal("Auto-Ernte: kein Knochenmehl mehr").withStyle(ChatFormatting.YELLOW));
			}
			return;
		}
		int to = boneSlot >= 0 && inv.getItem(boneSlot).isEmpty() ? boneSlot : -1;
		for (int i = 0; to < 0 && i < Inventory.getSelectionSize(); i++) {
			if (inv.getItem(i).isEmpty()) to = i;
		}
		if (to < 0) {
			if (!warnedBone) {
				warnedBone = true;
				player.sendOverlayMessage(Component.literal("Auto-Ernte: Schnellleiste voll, kein Platz für Knochenmehl").withStyle(ChatFormatting.YELLOW));
			}
			return;
		}
		// Im Inventar-Menue haben die Hauptinventar-Plaetze 9..35 dieselben Nummern
		mc.gameMode.handleContainerInput(player.inventoryMenu.containerId, from, to, ContainerInput.SWAP, player);
		boneSlot = to;
		warnedBone = false;
	}

	private static boolean ripe(BlockState st) {
		if (!SEEDS.containsKey(st.getBlock())) return false;
		if (st.getBlock() instanceof CropBlock crop) return crop.isMaxAge(st);
		return st.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
	}

	private static int hotbarSlot(Inventory inv, Item item) {
		if (inv.getSelectedItem().is(item)) return inv.getSelectedSlot();
		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			if (inv.getItem(i).is(item)) return i;
		}
		return -1;
	}

	private AutoCrops() {}
}
