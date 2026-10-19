package de.shoxi.mcguide;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** Die Phasen und ihre Ziele. Statistiken zaehlen nur den Zuwachs seit dem ersten Start (Baseline). */
public final class Goals {
	public record Goal(String id, String text, Predicate<Progress> check) {}
	public record Phase(String title, List<Goal> goals) {}

	/** Alle Statistiken, die fuer die Baseline gespeichert werden. */
	public static final List<Stat<?>> TRACKED = new ArrayList<>();

	private static Stat<?> track(Stat<?> s) {
		if (!TRACKED.contains(s)) TRACKED.add(s);
		return s;
	}

	private static Predicate<Progress> crafted(Item item, int n) {
		Stat<?> s = track(Stats.ITEM_CRAFTED.get(item));
		return p -> p.delta(s) >= n;
	}

	private static Predicate<Progress> craftedOrHas(Item item) {
		return crafted(item, 1).or(p -> p.has(item, 1));
	}

	private static final Item[][] ARMOR_OK = {
		{Items.IRON_HELMET, Items.DIAMOND_HELMET, Items.NETHERITE_HELMET},
		{Items.IRON_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE},
		{Items.IRON_LEGGINGS, Items.DIAMOND_LEGGINGS, Items.NETHERITE_LEGGINGS},
		{Items.IRON_BOOTS, Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS},
	};
	private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	private static boolean armorAtLeastIron(Progress p) {
		for (int i = 0; i < 4; i++) {
			Item worn = p.worn(ARMOR_SLOTS[i]);
			boolean ok = false;
			for (Item it : ARMOR_OK[i]) ok |= worn == it;
			if (!ok) return false;
		}
		return true;
	}

	public static final List<Phase> PHASES;

	static {
		Stat<?> minedObsidian = track(Stats.BLOCK_MINED.get(Blocks.OBSIDIAN));
		Stat<?> enchanted = track(Stats.CUSTOM.get(Stats.ENCHANT_ITEM));
		Stat<?> bred = track(Stats.CUSTOM.get(Stats.ANIMALS_BRED));
		Stat<?> traded = track(Stats.CUSTOM.get(Stats.TRADED_WITH_VILLAGER));
		Stat<?> usedSeeds = track(Stats.ITEM_USED.get(Items.WHEAT_SEEDS));
		Stat<?> usedCarrot = track(Stats.ITEM_USED.get(Items.CARROT));
		Stat<?> usedPotato = track(Stats.ITEM_USED.get(Items.POTATO));
		Stat<?> usedCane = track(Stats.ITEM_USED.get(Items.SUGAR_CANE));
		Stat<?> killedBlaze = track(Stats.ENTITY_KILLED.get(EntityTypes.BLAZE));
		Stat<?> pickedRod = track(Stats.ITEM_PICKED_UP.get(Items.BLAZE_ROD));
		Stat<?> pickedQuartz = track(Stats.ITEM_PICKED_UP.get(Items.QUARTZ));
		Stat<?> usedEye = track(Stats.ITEM_USED.get(Items.ENDER_EYE));
		Stat<?> killedDragon = track(Stats.ENTITY_KILLED.get(EntityTypes.ENDER_DRAGON));
		Stat<?> minedDebris = track(Stats.BLOCK_MINED.get(Blocks.ANCIENT_DEBRIS));

		PHASES = List.of(
			new Phase("Phase 1 · Ausrüstung", List.of(
				new Goal("p1_pick", "Diamantspitzhacke bauen (3 Diamanten)", craftedOrHas(Items.DIAMOND_PICKAXE)),
				new Goal("p1_sword", "Diamantschwert bauen (2 Diamanten)", craftedOrHas(Items.DIAMOND_SWORD)),
				new Goal("p1_armor", "Volle Eisenrüstung tragen", Goals::armorAtLeastIron),
				new Goal("p1_shield", "Schild dabei", p -> p.has(Items.SHIELD, 1)),
				new Goal("p1_bucket", "Zweiten Eimer bauen", crafted(Items.BUCKET, 1)
					.or(p -> p.count(Items.BUCKET) + p.count(Items.WATER_BUCKET) + p.count(Items.LAVA_BUCKET) >= 2))
			)),
			new Phase("Phase 2 · Verzaubern", List.of(
				new Goal("p2_obs", "Obsidian abbauen (Wasser auf Lava)", p -> p.delta(minedObsidian) >= 1 || p.has(Items.OBSIDIAN, 1)),
				new Goal("p2_obs14", "14 Obsidian sammeln", p -> p.delta(minedObsidian) >= 14 || p.has(Items.OBSIDIAN, 14)),
				new Goal("p2_book", "Bücher herstellen (Zuckerrohr + Leder)", crafted(Items.BOOK, 1)),
				new Goal("p2_table", "Zaubertisch bauen", craftedOrHas(Items.ENCHANTING_TABLE)),
				new Goal("p2_shelves", "15 Bücherregale um den Tisch", crafted(Items.BOOKSHELF, 15)),
				new Goal("p2_lvl30", "Level 30 erreichen", p -> p.level() >= 30),
				new Goal("p2_ench", "Erste Verzauberung", p -> p.delta(enchanted) >= 1)
			)),
			new Phase("Phase 3 · Basis & Essen", List.of(
				new Goal("p3_farm", "Feld anlegen (Samen, Karotten, Kartoffeln)",
					p -> p.delta(usedSeeds) + p.delta(usedCarrot) + p.delta(usedPotato) >= 16),
				new Goal("p3_breed", "Tiere züchten (Kühe für Leder)", p -> p.delta(bred) >= 1),
				new Goal("p3_cane", "Zuckerrohr am Wasser pflanzen", p -> p.delta(usedCane) >= 4),
				new Goal("p3_trade", "Mit Dorfbewohner handeln", p -> p.delta(traded) >= 1)
			)),
			new Phase("Phase 4 · Nether", List.of(
				new Goal("p4_gold", "Goldrüstungsteil bauen (gegen Piglins)", craftedOrHas(Items.GOLDEN_HELMET)
					.or(craftedOrHas(Items.GOLDEN_CHESTPLATE)).or(craftedOrHas(Items.GOLDEN_LEGGINGS)).or(craftedOrHas(Items.GOLDEN_BOOTS))),
				new Goal("p4_portal", "Netherportal bauen und betreten", p -> p.dimension() == Level.NETHER),
				new Goal("p4_fort", "Netherfestung finden (Lohe besiegen)", p -> p.delta(killedBlaze) >= 1),
				new Goal("p4_blaze", "Lohenrute holen", p -> p.has(Items.BLAZE_ROD, 1) || p.delta(pickedRod) >= 1),
				new Goal("p4_quartz", "Netherquarz sammeln", p -> p.delta(pickedQuartz) >= 1)
			)),
			new Phase("Phase 5 · Endspiel", List.of(
				new Goal("p5_eye", "Enderaugen herstellen", crafted(Items.ENDER_EYE, 1)),
				new Goal("p5_strong", "Festung mit Endportal finden (Augen werfen)", p -> p.delta(usedEye) >= 1),
				new Goal("p5_end", "Ins End reisen", p -> p.dimension() == Level.END),
				new Goal("p5_dragon", "Enderdrachen besiegen", p -> p.delta(killedDragon) >= 1),
				new Goal("p5_elytra", "Elytren finden", p -> p.has(Items.ELYTRA, 1)),
				new Goal("p5_debris", "Antiken Schrott finden (Nether, Y 15)", p -> p.delta(minedDebris) >= 1),
				new Goal("p5_neth", "Netheritrüstung tragen", p -> p.worn(EquipmentSlot.HEAD) == Items.NETHERITE_HELMET
					&& p.worn(EquipmentSlot.CHEST) == Items.NETHERITE_CHESTPLATE
					&& p.worn(EquipmentSlot.LEGS) == Items.NETHERITE_LEGGINGS
					&& p.worn(EquipmentSlot.FEET) == Items.NETHERITE_BOOTS)
			))
		);
	}

	/** Kurze Anleitung zu jedem Ziel. */
	private static final Map<String, String> HOWTO = Map.ofEntries(
		Map.entry("p1_pick", "Werkbank: 3 Diamanten oben in eine Reihe, 2 Stöcke darunter in die Mitte. Diamanten liegen am meisten auf Y -58."),
		Map.entry("p1_sword", "Werkbank: 2 Diamanten übereinander, darunter 1 Stock."),
		Map.entry("p1_armor", "Helm 5, Brustpanzer 8, Hose 7, Stiefel 4 Eisenbarren. Alle vier Teile anziehen."),
		Map.entry("p1_shield", "Werkbank: 6 Holzbretter in Y-Form, oben in der Mitte 1 Eisenbarren. In die zweite Hand nehmen."),
		Map.entry("p1_bucket", "3 Eisenbarren in V-Form. Einen Eimer für Wasser, einen für Lava."),
		Map.entry("p2_obs", "Kippe Wasser auf eine Lava-Quelle, sie wird zu Obsidian. Mit der Diamantspitzhacke abbauen (dauert ca. 10 Sekunden)."),
		Map.entry("p2_obs14", "Du brauchst 4 Obsidian für den Zaubertisch und 10 für ein Netherportal."),
		Map.entry("p2_book", "Buch = 3 Papier + 1 Leder. Papier aus 3 Zuckerrohr in einer Reihe. Leder bekommst du von Kühen."),
		Map.entry("p2_table", "Werkbank: oben 1 Buch, in der Mitte links und rechts je 1 Diamant, unten und in der Mitte 4 Obsidian."),
		Map.entry("p2_shelves", "Bücherregal = 6 Bretter + 3 Bücher. 15 Stück mit genau 1 Block Abstand rund um den Zaubertisch stellen."),
		Map.entry("p2_lvl30", "Erfahrung gibt es für Erze (Kohle, Redstone, Lapis), besiegte Monster, Schmelzen im Ofen und Handel."),
		Map.entry("p2_ench", "Gegenstand und Lapis in den Zaubertisch legen. Die unterste Option kostet 3 Level und ist am stärksten."),
		Map.entry("p3_farm", "Mit einer Hacke Ackerland neben Wasser anlegen (bis 4 Blöcke Abstand) und Samen, Karotten oder Kartoffeln pflanzen."),
		Map.entry("p3_breed", "Zwei Kühe mit Weizen füttern (Rechtsklick), dann bekommen sie ein Kalb."),
		Map.entry("p3_cane", "Zuckerrohr direkt neben Wasser auf Gras, Erde oder Sand setzen. Es wächst bis 3 Blöcke hoch."),
		Map.entry("p3_trade", "Geh in ein Dorf und mach Rechtsklick auf einen Dorfbewohner mit Beruf. Bibliothekare verkaufen Verzauberungsbücher."),
		Map.entry("p4_gold", "Goldstiefel aus 4 Goldbarren. Mit mindestens einem Goldteil greifen dich Piglins im Nether nicht an."),
		Map.entry("p4_portal", "Rahmen aus 10 Obsidian (4 breit, 5 hoch, Ecken weglassen), innen mit dem Feuerzeug anzünden. Bruchstein und Essen mitnehmen!"),
		Map.entry("p4_fort", "Netherfestungen sind aus dunklen Netherziegeln und ziehen sich lang in eine Richtung. Dort leben Lohen (fliegende Feuermonster)."),
		Map.entry("p4_blaze", "Lohen lassen Lohenruten fallen. Schild hilft gegen Feuerbälle. Du brauchst etwa 6 bis 8 Ruten."),
		Map.entry("p4_quartz", "Weißes Netherquarz-Erz steckt überall im roten Netherstein. Einfach mit der Spitzhacke abbauen."),
		Map.entry("p5_eye", "Enderauge = Lohenstaub + Enderperle. Enderperlen bekommst du von Endermen (nachts) oder beim Kleriker."),
		Map.entry("p5_strong", "Wirf ein Enderauge (Rechtsklick), es fliegt Richtung Festung. Folge ihm und wirf erneut, bis es nach unten fliegt."),
		Map.entry("p5_end", "Im Portalraum fehlende Augen in den Rahmen setzen (12 insgesamt) und hineinspringen. Gute Rüstung, Bogen und viele Blöcke mitnehmen!"),
		Map.entry("p5_dragon", "Erst die Endkristalle oben auf den Obsidiansäulen zerstören (mit Pfeilen), dann den Drachen angreifen, wenn er in der Mitte landet."),
		Map.entry("p5_elytra", "Nach dem Drachen durch das kleine Portal am Rand zu den Endstädten. Die Elytren hängen im Endschiff."),
		Map.entry("p5_debris", "Antiker Schrott liegt im Nether auf Y 8 bis 22 und ist sehr selten. Nur mit Diamantspitzhacke abbaubar."),
		Map.entry("p5_neth", "4 Antiker Schrott + 4 Gold = 1 Netheritbarren. Am Schmiedetisch mit Netherit-Vorlage die Diamantrüstung aufwerten."));

	public static String howTo(Goal g) {
		return HOWTO.getOrDefault(g.id(), "");
	}

	/** Phase, zu der ein Ziel gehoert. */
	public static Phase phaseOf(Goal g) {
		for (Phase p : PHASES) if (p.goals().contains(g)) return p;
		return null;
	}

	private Goals() {}
}
