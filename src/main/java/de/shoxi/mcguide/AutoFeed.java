package de.shoxi.mcguide;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Auto-Fuettern, rein clientseitig: erwachsene Nutztiere in Reichweite mit passendem Futter aus der
 * Schnellleiste fuettern. Ob ein Tier zuchtbereit ist, weiss nur der Server; ist es nicht bereit,
 * lehnt der Server ab und das Futter bleibt im Inventar. Darum jedes Tier hoechstens alle 30 s versuchen.
 */
final class AutoFeed {
	private static final int RETRY_TICKS = 20 * 30;
	private static final Map<Integer, Integer> lastTry = new HashMap<>();
	private static int ticks;
	private static int cooldown;
	private static boolean warnedFood;

	static void toggle(Minecraft mc) {
		GuideState s = GuideClient.settings;
		s.autoFeed = !s.autoFeed;
		s.save();
		lastTry.clear();
		warnedFood = false;
		if (mc.player != null) mc.player.sendOverlayMessage(Component.literal("Auto-Füttern " + (s.autoFeed ? "an" : "aus"))
			.withStyle(s.autoFeed ? ChatFormatting.GREEN : ChatFormatting.GRAY));
	}

	static void tick(Minecraft mc) {
		ticks++;
		LocalPlayer player = mc.player;
		if (!GuideClient.settings.autoFeed || player == null || mc.level == null || mc.gameMode == null) return;
		if (mc.gui.screen() != null || mc.options.keyAttack.isDown() || mc.options.keyUse.isDown() || player.isUsingItem()) return;
		if (cooldown > 0) {
			cooldown--;
			return;
		}
		if (ticks % 200 == 0) lastTry.values().removeIf(t -> ticks - t > RETRY_TICKS);
		List<Animal> animals = mc.level.getEntitiesOfClass(Animal.class, player.getBoundingBox().inflate(6),
			a -> farmAnimal(a) && !a.isBaby() && a.isAlive() && player.isWithinEntityInteractionRange(a, 0.0));
		Inventory inv = player.getInventory();
		boolean anyFoodNeeded = false;
		for (Animal a : animals) {
			Integer t = lastTry.get(a.getId());
			if (t != null && ticks - t < RETRY_TICKS) continue;
			int slot = foodSlot(inv, a);
			if (slot < 0) {
				anyFoodNeeded = true;
				continue;
			}
			lastTry.put(a.getId(), ticks);
			int old = inv.getSelectedSlot();
			inv.setSelectedSlot(slot);
			EntityHitResult hit = new EntityHitResult(a, a.getBoundingBox().getCenter());
			if (mc.gameMode.interact(player, a, hit, InteractionHand.MAIN_HAND).consumesAction()) {
				player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getAttackAnimation(), false);
			}
			inv.setSelectedSlot(old);
			cooldown = 4;
			return;
		}
		if (anyFoodNeeded && !warnedFood) {
			warnedFood = true;
			player.sendOverlayMessage(Component.literal("Auto-Füttern: passendes Futter in die Schnellleiste legen").withStyle(ChatFormatting.YELLOW));
		}
	}

	/** Nur Nutztiere; Pferde, Woelfe usw. bekommen teures Futter und werden nicht angefasst. */
	private static boolean farmAnimal(Animal a) {
		return a instanceof AbstractCow || a instanceof Sheep || a instanceof Pig || a instanceof Chicken
			|| a instanceof Goat || a instanceof Rabbit;
	}

	private static int foodSlot(Inventory inv, Animal a) {
		if (a.isFood(inv.getSelectedItem())) return inv.getSelectedSlot();
		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			if (!inv.getItem(i).isEmpty() && a.isFood(inv.getItem(i))) return i;
		}
		return -1;
	}

	static void reset() {
		lastTry.clear();
	}

	private AutoFeed() {}
}
