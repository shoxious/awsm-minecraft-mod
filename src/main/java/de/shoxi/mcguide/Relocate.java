package de.shoxi.mcguide;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

/**
 * Umzugs-Modus, rein clientseitig ueber normale Shift-Klicks:
 * Kiste/Fass oeffnen -> bis zu 27 Stapel wandern ins Inventar (vorgemerkt).
 * Aufgestellte Shulkerkiste oeffnen -> genau die vorgemerkten Sachen wandern hinein.
 * Shulker abbauen, neue hinstellen, naechste Kiste – usw.
 */
final class Relocate {
	private static final int SHULKER_SLOTS = 27;
	/** Aus Kisten geholt und noch nicht in eine Shulker gepackt. */
	private static final List<ItemStack> pending = new ArrayList<>();
	private static int ticks;
	private static int doneMenu = -1;
	private static int cursor;
	private static int moved;

	static void tick(Minecraft mc) {
		if (!GuideClient.settings.relocate || mc.player == null || mc.gameMode == null) return;
		AbstractContainerMenu menu = switch (mc.gui.screen()) {
			case ShulkerBoxScreen s -> s.getMenu();
			case ContainerScreen s -> s.getMenu();
			case null, default -> null;
		};
		if (menu == null || menu.containerId == doneMenu) {
			if (menu == null) cursor = moved = 0;
			return;
		}
		if (ticks++ % 2 != 0) return;
		if (menu instanceof ChestMenu chest) fromChest(mc, chest);
		else intoShulker(mc, menu);
	}

	/** Ein Stapel pro Schritt aus der Kiste ins Inventar. */
	private static void fromChest(Minecraft mc, ChestMenu menu) {
		int size = menu.getRowCount() * 9;
		while (cursor < size && slotsNeeded() < SHULKER_SLOTS) {
			int i = cursor++;
			ItemStack before = menu.getSlot(i).getItem().copy();
			if (before.isEmpty() || !before.getItem().canFitInsideContainerItems()) continue;
			mc.gameMode.handleContainerInput(menu.containerId, i, 0, ContainerInput.QUICK_MOVE, mc.player);
			int n = before.getCount() - menu.getSlot(i).getItem().getCount();
			if (n <= 0) break; // Inventar voll
			add(before.copyWithCount(n));
			moved++;
			return;
		}
		finish(mc, menu, moved == 0
			? Component.literal("Umzug: nichts mitgenommen – Inventar voll oder Kiste leer").withStyle(ChatFormatting.YELLOW)
			: Component.literal("Umzug: " + slotsNeeded() + " Stapel eingepackt – jetzt eine leere Shulkerkiste öffnen").withStyle(ChatFormatting.GREEN));
	}

	/** Vorgemerkte Sachen aus dem Inventar (Slots nach den 27 Shulker-Plaetzen) in die Shulker. */
	private static void intoShulker(Minecraft mc, AbstractContainerMenu menu) {
		if (pending.isEmpty()) {
			finish(mc, menu, Component.literal("Umzug: erst eine Kiste öffnen, dann die Shulker").withStyle(ChatFormatting.GRAY));
			return;
		}
		for (int i = SHULKER_SLOTS; i < menu.slots.size(); i++) {
			ItemStack before = menu.getSlot(i).getItem().copy();
			ItemStack want = match(before);
			if (want == null) continue;
			mc.gameMode.handleContainerInput(menu.containerId, i, 0, ContainerInput.QUICK_MOVE, mc.player);
			int n = before.getCount() - menu.getSlot(i).getItem().getCount();
			if (n <= 0) {
				finish(mc, menu, Component.literal("Umzug: Shulker voll – abbauen, neue hinstellen und öffnen").withStyle(ChatFormatting.YELLOW));
				return;
			}
			want.shrink(n);
			pending.removeIf(ItemStack::isEmpty);
			return;
		}
		pending.clear(); // Rest nicht mehr im Inventar (z. B. weggeworfen)
		finish(mc, menu, Component.literal("Umzug: Shulker gepackt – abbauen und nächste Kiste öffnen").withStyle(ChatFormatting.GREEN));
	}

	private static void finish(Minecraft mc, AbstractContainerMenu menu, Component msg) {
		doneMenu = menu.containerId;
		mc.player.closeContainer();
		mc.player.sendOverlayMessage(msg);
	}

	private static ItemStack match(ItemStack s) {
		if (s.isEmpty()) return null;
		for (ItemStack p : pending) if (ItemStack.isSameItemSameComponents(p, s)) return p;
		return null;
	}

	private static void add(ItemStack s) {
		ItemStack p = match(s);
		if (p != null) p.grow(s.getCount());
		else pending.add(s);
	}

	/** Wie viele Shulker-Plaetze die vorgemerkten Sachen brauchen. */
	private static int slotsNeeded() {
		int n = 0;
		for (ItemStack p : pending) n += (p.getCount() + p.getMaxStackSize() - 1) / p.getMaxStackSize();
		return n;
	}

	static void toggle(Minecraft mc) {
		GuideState s = GuideClient.settings;
		s.relocate = !s.relocate;
		s.save();
		pending.clear();
		doneMenu = -1;
		if (mc.player != null) mc.player.sendOverlayMessage(Component.literal("Umzugs-Modus " + (s.relocate ? "an" : "aus"))
			.withStyle(s.relocate ? ChatFormatting.GREEN : ChatFormatting.GRAY));
	}

	private Relocate() {}
}
