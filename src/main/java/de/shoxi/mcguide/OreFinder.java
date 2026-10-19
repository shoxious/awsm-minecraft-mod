package de.shoxi.mcguide;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Erz-Finder: farbige Kaesten um Erze im Umkreis, durch Waende sichtbar.
 * Rein clientseitig, laeuft auf jedem Server (auch ohne Guide-Mod dort).
 */
final class OreFinder {
	record Ore(String name, int color) {}
	private record Hit(BlockPos pos, Ore ore) {}

	private static final Ore DIAMOND = new Ore("Diamant", 0x4AEDD9);
	private static final Ore EMERALD = new Ore("Smaragd", 0x17DD62);
	private static final Ore GOLD = new Ore("Gold", 0xFCEE4B);
	private static final Ore IRON = new Ore("Eisen", 0xD8AF93);
	private static final Ore REDSTONE = new Ore("Redstone", 0xFF2A2A);
	private static final Ore LAPIS = new Ore("Lapis", 0x345EC3);
	private static final Ore DEBRIS = new Ore("Schrott", 0xA0522D);
	private static final Ore QUARTZ = new Ore("Quarz", 0xEFE9E1);
	private static final Ore COPPER = new Ore("Kupfer", 0xE77C56);
	private static final Ore COAL = new Ore("Kohle", 0x3A3A3A);

	/** Reihenfolge = Reihenfolge in der Anzeige. */
	private static final List<Ore> ORDER = List.of(DIAMOND, EMERALD, DEBRIS, GOLD, REDSTONE, LAPIS, IRON, QUARTZ, COPPER, COAL);
	private static final Map<Block, Ore> BLOCKS = new IdentityHashMap<>();

	static {
		put(DIAMOND, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
		put(EMERALD, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
		put(GOLD, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE);
		put(IRON, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
		put(REDSTONE, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
		put(LAPIS, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE);
		put(DEBRIS, Blocks.ANCIENT_DEBRIS);
		put(QUARTZ, Blocks.NETHER_QUARTZ_ORE);
		put(COPPER, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);
		put(COAL, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE);
	}

	/** Erzsorte eines Blocks oder null (fuer den Ader-Abbau). */
	static Ore oreOf(Block b) {
		return BLOCKS.get(b);
	}

	private static void put(Ore ore, Block... blocks) {
		for (Block b : blocks) BLOCKS.put(b, ore);
	}

	private static List<Hit> hits = List.of();
	private static int ticks;

	static void toggle(Minecraft mc) {
		setEnabled(mc, !GuideClient.settings.oreFinder);
	}

	static void setEnabled(Minecraft mc, boolean on) {
		GuideState s = GuideClient.settings;
		s.oreFinder = on;
		s.save();
		hits = List.of();
		ticks = 0;
		mc.player.sendOverlayMessage(Component.literal("Erz-Finder " + (on ? "an (" + s.oreRadius + " Blöcke)" : "aus"))
			.withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY));
	}

	static void setRadius(Minecraft mc, int radius) {
		GuideClient.settings.oreRadius = radius;
		setEnabled(mc, true);
	}

	static void toggleExtras(Minecraft mc) {
		GuideState s = GuideClient.settings;
		s.oreCoal = s.oreCopper = !(s.oreCoal || s.oreCopper);
		s.save();
		ticks = 0;
		mc.player.sendOverlayMessage(Component.literal("Kohle und Kupfer " + (s.oreCoal ? "werden angezeigt" : "ausgeblendet")));
	}

	static boolean active() {
		return GuideClient.settings.oreFinder;
	}

	/** Laeuft im Client-Tick; dort sammelt Minecraft die Gizmos fuer diesen Tick ein. */
	static void tick(Minecraft mc) {
		if (!active() || mc.player == null || mc.level == null) {
			hits = List.of();
			return;
		}
		if (ticks++ % 10 == 0) scan(mc);
		for (Hit h : hits) {
			int c = h.ore().color();
			Gizmos.cuboid(h.pos(), GizmoStyle.strokeAndFill(0xFF000000 | c, 2.5f, 0x40000000 | c)).setAlwaysOnTop();
		}
	}

	private static void scan(Minecraft mc) {
		GuideState s = GuideClient.settings;
		int r = s.oreRadius;
		BlockPos center = mc.player.blockPosition();
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		List<Hit> found = new ArrayList<>();
		for (int dx = -r; dx <= r; dx++) {
			for (int dy = -r; dy <= r; dy++) {
				for (int dz = -r; dz <= r; dz++) {
					if (dx * dx + dy * dy + dz * dz > r * r) continue;
					p.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
					Ore ore = BLOCKS.get(mc.level.getBlockState(p).getBlock());
					if (ore == null) continue;
					if ((ore == COAL && !s.oreCoal) || (ore == COPPER && !s.oreCopper)) continue;
					found.add(new Hit(p.immutable(), ore));
				}
			}
		}
		hits = found;
	}

	/** Kurzfassung fuer die HUD-Zeile, z.B. "Diamant 4 · Eisen 12". */
	static String summary() {
		Map<Ore, Integer> counts = new LinkedHashMap<>();
		for (Ore o : ORDER) counts.put(o, 0);
		for (Hit h : hits) counts.merge(h.ore(), 1, Integer::sum);
		StringBuilder sb = new StringBuilder();
		counts.forEach((o, n) -> {
			if (n == 0) return;
			if (!sb.isEmpty()) sb.append(" · ");
			sb.append(o.name()).append(' ').append(n);
		});
		return sb.isEmpty() ? "keine Erze in der Nähe" : sb.toString();
	}

	private OreFinder() {}
}
