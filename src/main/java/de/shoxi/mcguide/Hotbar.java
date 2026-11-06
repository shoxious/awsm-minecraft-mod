package de.shoxi.mcguide;

import java.util.function.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

/** Holt einen Gegenstand in die Schnellleiste (per Tausch-Klick im Inventar, geht auch auf Vanilla-Servern). */
final class Hotbar {
	/** Schnellleisten-Platz mit passendem Gegenstand; liegt er nur im Inventar, wird er auf einen freien Platz getauscht. -1 = keiner. */
	static int bring(Minecraft mc, Predicate<ItemStack> want) {
		var inv = mc.player.getInventory();
		for (int i = 0; i < 9; i++) if (want.test(inv.getItem(i))) return i;
		if (mc.player.containerMenu != mc.player.inventoryMenu) return -1;
		int from = -1;
		for (int i = 9; i < 36 && from < 0; i++) if (want.test(inv.getItem(i))) from = i;
		if (from < 0) return -1;
		int to = -1;
		for (int i = 0; i < 9 && to < 0; i++) if (inv.getItem(i).isEmpty()) to = i;
		// kein Platz frei: einen Platz ohne Werkzeug, Essen oder Fackeln freimachen (der wandert ins Inventar)
		for (int i = 8; i >= 0 && to < 0; i--) {
			ItemStack s = inv.getItem(i);
			if (!s.isDamageableItem() && !s.has(net.minecraft.core.component.DataComponents.FOOD) && !s.is(net.minecraft.world.item.Items.TORCH)) to = i;
		}
		if (to < 0) to = 8;
		// Im Spielerinventar-Fenster liegen die Inventarplaetze 9..35 auf denselben Nummern
		mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId, from, to, ContainerInput.SWAP, mc.player);
		return to;
	}

	static int count(Minecraft mc, Predicate<ItemStack> want) {
		int n = 0;
		for (int i = 0; i < 36; i++) {
			ItemStack s = mc.player.getInventory().getItem(i);
			if (want.test(s)) n += s.getCount();
		}
		return n;
	}

	private Hotbar() {}
}
