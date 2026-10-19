package de.shoxi.mcguide;

import java.util.Map;

import net.minecraft.resources.ResourceKey;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Momentaufnahme eines Spielers fuer die Ziel-Pruefung (laeuft auf dem Server). */
public final class Progress {
	private final Player player;
	private final StatsCounter stats;
	private final Map<String, Integer> baseline;

	Progress(Player player, StatsCounter stats, Map<String, Integer> baseline) {
		this.player = player;
		this.stats = stats;
		this.baseline = baseline;
	}

	public int count(Item item) {
		Inventory inv = player.getInventory();
		int n = 0;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (s.is(item)) n += s.getCount();
		}
		return n;
	}

	public boolean has(Item item, int n) {
		int c = count(item);
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (player.getItemBySlot(slot).is(item)) c++;
		}
		return c >= n;
	}

	public Item worn(EquipmentSlot slot) {
		return player.getItemBySlot(slot).getItem();
	}

	public int level() {
		return player.experienceLevel;
	}

	public ResourceKey<Level> dimension() {
		return player.level().dimension();
	}

	public int delta(Stat<?> stat) {
		return stats.getValue(stat) - baseline.getOrDefault(GuideData.key(stat), 0);
	}
}
