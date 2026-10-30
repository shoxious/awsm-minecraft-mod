package de.shoxi.mcguide;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Sortiert per normaler Klicks (laeuft auch auf Vanilla-Servern):
 * in Kisten/Shulkern die Kiste, sonst das Hauptinventar ohne Schnellleiste.
 */
final class InventorySort {
	private static Map<Item, Integer> creativeOrder;
	private static Object orderLevel;

	/** Reihenfolge wie im Kreativ-Inventar (Baubloecke, Werkzeuge, Essen ...), dann Name, dann Menge. */
	private static Comparator<ItemStack> order(Minecraft mc) {
		if (creativeOrder == null || orderLevel != mc.level) {
			CreativeModeTabs.tryRebuildTabContents(mc.player.connection.enabledFeatures(), false, mc.level.registryAccess());
			creativeOrder = new IdentityHashMap<>();
			for (CreativeModeTab tab : CreativeModeTabs.tabs()) {
				for (ItemStack st : tab.getDisplayItems()) creativeOrder.putIfAbsent(st.getItem(), creativeOrder.size());
			}
			orderLevel = mc.level;
		}
		Map<Item, Integer> pos = creativeOrder;
		return Comparator.<ItemStack>comparingInt(s -> pos.getOrDefault(s.getItem(), pos.size() + BuiltInRegistries.ITEM.getId(s.getItem())))
			.thenComparing(s -> s.getHoverName().getString())
			.thenComparing(Comparator.comparingInt(ItemStack::getCount).reversed());
	}

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
		if (!merge(mc, menu, slots)) return;
		Comparator<ItemStack> order = order(mc);
		for (int i = 0; i < slots.size(); i++) {
			int best = -1;
			for (int j = i; j < slots.size(); j++) {
				ItemStack st = slots.get(j).getItem();
				if (st.isEmpty()) continue;
				if (best < 0 || order.compare(st, slots.get(best).getItem()) < 0) best = j;
			}
			if (best < 0) break;
			if (best == i) continue;
			click(mc, menu, slots.get(best));
			click(mc, menu, slots.get(i));
			if (!menu.getCarried().isEmpty()) click(mc, menu, slots.get(best));
			if (!menu.getCarried().isEmpty()) return; // unerwartet, lieber aufhoeren
		}
	}

	/** Erst angebrochene Stapel zusammenlegen; false wenn dabei etwas schiefging. */
	private static boolean merge(Minecraft mc, AbstractContainerMenu menu, List<Slot> slots) {
		for (int i = 0; i < slots.size(); i++) {
			for (int j = i + 1; j < slots.size(); j++) {
				ItemStack a = slots.get(i).getItem();
				if (a.isEmpty() || a.getCount() >= a.getMaxStackSize()) break;
				ItemStack b = slots.get(j).getItem();
				if (b.isEmpty() || !ItemStack.isSameItemSameComponents(a, b)) continue;
				click(mc, menu, slots.get(j));
				click(mc, menu, slots.get(i));
				if (!menu.getCarried().isEmpty()) click(mc, menu, slots.get(j));
				if (!menu.getCarried().isEmpty()) return false;
			}
		}
		return true;
	}

	private static void click(Minecraft mc, AbstractContainerMenu menu, Slot slot) {
		mc.gameMode.handleContainerInput(menu.containerId, slot.index, 0, ContainerInput.PICKUP, mc.player);
	}

	private InventorySort() {}
}
