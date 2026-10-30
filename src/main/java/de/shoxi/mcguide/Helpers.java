package de.shoxi.mcguide;

import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Kleine Automatiken: Werkzeug waehlen, Werkzeug-Warnung, Essen, Fackeln, Fullbright. */
final class Helpers {
	private static final Set<Item> BAD_FOOD = Set.of(Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.POISONOUS_POTATO,
		Items.PUFFERFISH, Items.CHORUS_FRUIT, Items.SUSPICIOUS_STEW);

	private static int eatSlot = -1;
	private static int eatOldSlot;
	private static int eatCooldown;
	private static int torchCooldown;
	private static int torchWarnTick = -100000;
	private static int warnTicks;
	private static boolean nightVisionOurs;

	static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null || mc.gameMode == null) {
			eatSlot = -1;
			return;
		}
		GuideState s = GuideClient.settings;
		fullbright(player, s.fullbright);
		if (eatSlot >= 0) {
			continueEating(mc, player);
			return;
		}
		if (mc.gui.screen() != null || player.isSpectator()) return;
		if (s.toolWarn) toolWarning(player);
		if (s.autoTool) autoTool(mc, player);
		if (s.autoEat && !mc.options.keyAttack.isDown() && !mc.options.keyUse.isDown() && TreeFeller.remaining() == 0) startEating(mc, player);
		if (s.autoTorch) autoTorch(mc, player);
	}

	// ---------- Werkzeug ----------

	static boolean nearlyBroken(ItemStack st) {
		return st.isDamageableItem() && st.getMaxDamage() - st.getDamageValue() <= 3;
	}

	private static void autoTool(Minecraft mc, LocalPlayer player) {
		if (!mc.options.keyAttack.isDown() || player.getAbilities().instabuild) return;
		if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
		BlockState state = mc.level.getBlockState(hit.getBlockPos());
		if (state.isAir() || state.getDestroySpeed(mc.level, hit.getBlockPos()) == 0) return;
		Inventory inv = player.getInventory();
		int best = inv.getSelectedSlot();
		float bestScore = score(inv.getSelectedItem(), state);
		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			ItemStack st = inv.getItem(i);
			if (st.isEmpty() || nearlyBroken(st)) continue;
			float sc = score(st, state);
			if (sc > bestScore) {
				bestScore = sc;
				best = i;
			}
		}
		if (best != inv.getSelectedSlot()) inv.setSelectedSlot(best);
	}

	/** Abbau-Tempo, plus Bonus wenn das Werkzeug den Drop ermoeglicht. */
	private static float score(ItemStack st, BlockState state) {
		if (nearlyBroken(st)) return 0;
		float speed = st.getDestroySpeed(state);
		if (state.requiresCorrectToolForDrops() && st.isCorrectToolForDrops(state)) speed += 1000;
		return speed;
	}

	private static void toolWarning(LocalPlayer player) {
		ItemStack st = player.getMainHandItem();
		if (!st.isDamageableItem()) return;
		int left = st.getMaxDamage() - st.getDamageValue();
		if (left > Math.max(5, st.getMaxDamage() / 20)) return;
		if (warnTicks++ % 40 == 0) {
			player.sendOverlayMessage(Component.literal("⚠ " + st.getHoverName().getString() + " fast kaputt (" + left + ")")
				.withStyle(ChatFormatting.RED));
		}
	}

	// ---------- Essen ----------

	private static void startEating(Minecraft mc, LocalPlayer player) {
		if (eatCooldown > 0) {
			eatCooldown--;
			return;
		}
		if (player.getAbilities().instabuild || player.getFoodData().getFoodLevel() > 14 || player.isUsingItem()) return;
		Inventory inv = player.getInventory();
		int slot = -1;
		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			ItemStack st = inv.getItem(i);
			if (st.has(DataComponents.FOOD) && !BAD_FOOD.contains(st.getItem())) {
				slot = i;
				break;
			}
		}
		if (slot < 0) return;
		eatOldSlot = inv.getSelectedSlot();
		inv.setSelectedSlot(slot);
		if (!mc.gameMode.useItem(player, InteractionHand.MAIN_HAND).consumesAction() || !player.isUsingItem()) {
			inv.setSelectedSlot(eatOldSlot);
			eatCooldown = 100;
			return;
		}
		eatSlot = slot;
		// Rechtsklick "gedrueckt halten", sonst bricht Vanilla das Essen sofort ab
		mc.options.keyUse.setDown(true);
	}

	private static void continueEating(Minecraft mc, LocalPlayer player) {
		Inventory inv = player.getInventory();
		if (player.isUsingItem() && inv.getSelectedSlot() == eatSlot) return;
		mc.options.keyUse.setDown(false);
		if (inv.getSelectedSlot() == eatSlot) inv.setSelectedSlot(eatOldSlot);
		eatSlot = -1;
		eatCooldown = 20;
	}

	// ---------- Fackeln ----------

	private static void autoTorch(Minecraft mc, LocalPlayer player) {
		if (torchCooldown > 0) {
			torchCooldown--;
			return;
		}
		if (!player.onGround() || mc.options.keyUse.isDown()) return;
		BlockPos pos = player.blockPosition();
		BlockPos below = pos.below();
		if (!mc.level.getBlockState(pos).isAir() || !mc.level.getBlockState(below).isFaceSturdy(mc.level, below, Direction.UP)) return;
		// nur wo Mobs spawnen koennen: kein Blocklicht, kein (nennenswertes) Himmelslicht
		// auch vorausschauen, damit die Fackel steht, bevor man im Dunkeln ist
		if (Math.min(mc.level.getBrightness(LightLayer.BLOCK, pos), lightAhead(mc, player, pos)) > 2) return;
		if (!GuideClient.settings.torchOutdoor && mc.level.getBrightness(LightLayer.SKY, pos) >= 8) return;
		if (mc.level.getBlockState(below).is(BlockTags.LEAVES)) return;
		if (torchNearby(mc, pos)) return;
		Inventory inv = player.getInventory();
		InteractionHand hand = InteractionHand.MAIN_HAND;
		int slot = -1;
		if (isTorch(player.getOffhandItem())) {
			hand = InteractionHand.OFF_HAND;
		} else {
			for (int i = 0; i < Inventory.getSelectionSize(); i++) {
				if (isTorch(inv.getItem(i))) {
					slot = i;
					break;
				}
			}
			if (slot < 0) {
				torchWarning(player, "Auto-Fackel: keine Fackeln in Schnellleiste oder Zweithand");
				torchCooldown = 20;
				return;
			}
		}
		int old = inv.getSelectedSlot();
		if (slot >= 0) inv.setSelectedSlot(slot);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(below).add(0, 0.5, 0), Direction.UP, below, false);
		if (mc.gameMode.useItemOn(player, hand, hit).consumesAction()) {
			player.swing(hand, player.getItemInHand(hand).getAttackAnimation(), false);
		} else {
			torchWarning(player, "Auto-Fackel: Fackel ließ sich hier nicht setzen");
		}
		if (slot >= 0) inv.setSelectedSlot(old);
		torchCooldown = 10;
	}

	/** Dunkelstes Blocklicht 2-5 Bloecke in Blickrichtung (bis zur naechsten Wand). */
	private static int lightAhead(Minecraft mc, LocalPlayer player, BlockPos pos) {
		Vec3 look = player.getLookAngle();
		Vec3 dir = new Vec3(look.x, 0, look.z);
		if (dir.lengthSqr() < 1.0E-4) return 15;
		dir = dir.normalize();
		Vec3 start = Vec3.atCenterOf(pos);
		int min = 15;
		for (int step = 2; step <= 5; step++) {
			BlockPos p = BlockPos.containing(start.add(dir.scale(step)));
			if (!mc.level.getBlockState(p).isAir() && !mc.level.getBlockState(p.above()).isAir()) break;
			if (mc.level.getBlockState(p).isAir()) min = Math.min(min, mc.level.getBrightness(LightLayer.BLOCK, p));
		}
		return min;
	}

		private static boolean isTorch(ItemStack st) {
		return st.is(Items.TORCH) || st.is(Items.SOUL_TORCH) || st.is(Items.COPPER_TORCH);
	}

	/** Hoechstens alle 30 Sekunden, damit es nicht nervt. */
	private static void torchWarning(LocalPlayer player, String msg) {
		if (player.tickCount - torchWarnTick < 20 * 30) return;
		torchWarnTick = player.tickCount;
		player.sendOverlayMessage(Component.literal(msg).withStyle(ChatFormatting.YELLOW));
	}

	/** Licht wird verzoegert berechnet: keine zweite Fackel direkt neben der letzten. */
	private static boolean torchNearby(Minecraft mc, BlockPos pos) {
		for (BlockPos p : BlockPos.betweenClosed(pos.offset(-5, -2, -5), pos.offset(5, 2, 5))) {
			BlockState st = mc.level.getBlockState(p);
			if (st.getBlock() instanceof BaseTorchBlock) return true;
		}
		return false;
	}

	// ---------- Fullbright ----------

	private static void fullbright(LocalPlayer player, boolean on) {
		if (on) {
			if (!player.hasEffect(MobEffects.NIGHT_VISION)) {
				player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, 0, false, false, false));
				nightVisionOurs = true;
			}
		} else if (nightVisionOurs) {
			player.removeEffect(MobEffects.NIGHT_VISION);
			nightVisionOurs = false;
		}
	}

	static void toggleFullbright(Minecraft mc) {
		GuideState s = GuideClient.settings;
		s.fullbright = !s.fullbright;
		s.save();
		mc.player.sendOverlayMessage(Component.literal("Fullbright " + (s.fullbright ? "an" : "aus"))
			.withStyle(s.fullbright ? ChatFormatting.GREEN : ChatFormatting.GRAY));
	}

	static void reset() {
		eatSlot = -1;
		nightVisionOurs = false;
	}

	private Helpers() {}
}
