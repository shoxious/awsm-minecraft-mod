package de.shoxi.mcguide;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Sortiert per normaler Klicks (laeuft auch auf Vanilla-Servern):
 * in Kisten/Shulkern die Kiste, sonst das Hauptinventar ohne Schnellleiste.
 */
final class InventorySort {
	private static final Comparator<ItemStack> ORDER = Comparator
		.comparing((ItemStack s) -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString())
		.thenComparing(Comparator.comparingInt(ItemStack::getCount).reversed());

	static void sort(Minecraft mc, AbstractContainerScreen<?> screen) {
		if (mc.player == null || mc.gameMode == null) return;
		AbstractContainerMenu menu = screen.getMenu();
		if (!menu.getCarried().isEmpty()) return;
		Inventory inv = mc.player.getInventory();
		boolean chest = menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu;
		List<Slot> slots = new ArrayList<>();
		for (Slot s : menu.slots) {
			boolean playerInv = s.container == inv;
			if (chest ? !playerInv : playerInv && s.getContainerSlot() >= 9 && s.getContainerSlot() < 36) slots.add(s);
		}
		for (int i = 0; i < slots.size(); i++) {
			int best = -1;
			for (int j = i; j < slots.size(); j++) {
				ItemStack st = slots.get(j).getItem();
				if (st.isEmpty()) continue;
				if (best < 0 || ORDER.compare(st, slots.get(best).getItem()) < 0) best = j;
			}
			if (best < 0) break;
			if (best == i) continue;
			click(mc, menu, slots.get(best));
			click(mc, menu, slots.get(i));
			if (!menu.getCarried().isEmpty()) click(mc, menu, slots.get(best));
			if (!menu.getCarried().isEmpty()) return; // unerwartet, lieber aufhoeren
		}
	}

	private static void click(Minecraft mc, AbstractContainerMenu menu, Slot slot) {
		mc.gameMode.handleContainerInput(menu.containerId, slot.index, 0, ContainerInput.PICKUP, mc.player);
	}

	private InventorySort() {}
}
