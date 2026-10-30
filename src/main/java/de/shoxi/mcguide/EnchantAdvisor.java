package de.shoxi.mcguide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.shoxi.mcguide.mixin.ContainerScreenAccessor;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Infobox neben dem Inventar: beste Verzauberungen fuer das Teil unter der Maus,
 * mit Haken fuer das, was schon drauf ist.
 */
final class EnchantAdvisor {
	/** Eine Empfehlung; mehrere Optionen = "eins davon" (schliessen sich aus). */
	private record Opt(String id, int level) {}

	private record Rec(List<Opt> options) {}

	private static List<Rec> recs(String spec) {
		List<Rec> out = new ArrayList<>();
		for (String part : spec.split(",")) {
			List<Opt> opts = new ArrayList<>();
			for (String o : part.trim().split("\\|")) {
				String[] kv = o.trim().split(" ");
				opts.add(new Opt(kv[0], kv.length > 1 ? Integer.parseInt(kv[1]) : 1));
			}
			out.add(new Rec(opts));
		}
		return out;
	}

	private static final String KEEP = "unbreaking 3, mending";
	private static final List<Rec> HELMET = recs("protection 4, respiration 3, aqua_affinity, " + KEEP);
	private static final List<Rec> CHEST = recs("protection 4, " + KEEP);
	private static final List<Rec> LEGS = recs("protection 4, swift_sneak 3, " + KEEP);
	private static final List<Rec> BOOTS = recs("protection 4, feather_falling 4, depth_strider 3, soul_speed 3, " + KEEP);
	private static final List<Rec> SWORD = recs("sharpness 5, looting 3, fire_aspect 2, sweeping_edge 3, " + KEEP);
	private static final List<Rec> SPEAR = recs("sharpness 5, lunge 3, looting 3, fire_aspect 2, " + KEEP);
	private static final List<Rec> AXE = recs("efficiency 5, sharpness 5, fortune 3|silk_touch, " + KEEP);
	private static final List<Rec> MINING = recs("efficiency 5, fortune 3|silk_touch, " + KEEP);
	private static final List<Rec> HOE = recs("efficiency 5, fortune 3, " + KEEP);
	private static final List<Rec> BOW = recs("power 5, punch 2, flame, infinity|mending, unbreaking 3");
	private static final List<Rec> CROSSBOW = recs("quick_charge 3, multishot|piercing 4, " + KEEP);
	private static final List<Rec> TRIDENT = recs("impaling 5, loyalty 3|riptide 3, channeling, " + KEEP);
	private static final List<Rec> MACE = recs("density 5|breach 4, wind_burst 3, fire_aspect 2, " + KEEP);
	private static final List<Rec> ROD = recs("luck_of_the_sea 3, lure 3, " + KEEP);
	private static final List<Rec> SHEARS = recs("efficiency 5, " + KEEP);
	private static final List<Rec> BASIC = recs(KEEP);

	private static List<Rec> forItem(ItemStack st) {
		if (st.is(ItemTags.HEAD_ARMOR)) return HELMET;
		if (st.is(ItemTags.CHEST_ARMOR)) return CHEST;
		if (st.is(ItemTags.LEG_ARMOR)) return LEGS;
		if (st.is(ItemTags.FOOT_ARMOR)) return BOOTS;
		if (st.is(ItemTags.SWORDS)) return SWORD;
		if (st.is(ItemTags.SPEARS)) return SPEAR;
		if (st.is(ItemTags.AXES)) return AXE;
		if (st.is(ItemTags.PICKAXES) || st.is(ItemTags.SHOVELS)) return MINING;
		if (st.is(ItemTags.HOES)) return HOE;
		if (st.is(Items.BOW)) return BOW;
		if (st.is(Items.CROSSBOW)) return CROSSBOW;
		if (st.is(Items.TRIDENT)) return TRIDENT;
		if (st.is(Items.MACE)) return MACE;
		if (st.is(Items.FISHING_ROD)) return ROD;
		if (st.is(Items.SHEARS)) return SHEARS;
		if (st.is(Items.ELYTRA) || st.is(Items.SHIELD) || st.is(Items.FLINT_AND_STEEL) || st.is(Items.BRUSH)) return BASIC;
		return null;
	}

	/** Ab ScreenEvents.afterExtract jedes Container-Screens. */
	static void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g) {
		if (!GuideClient.settings.enchantHelp) return;
		ContainerScreenAccessor acc = (ContainerScreenAccessor) screen;
		Slot slot = acc.mcguide$hoveredSlot();
		if (slot == null || !slot.hasItem()) return;
		ItemStack st = slot.getItem();
		List<Rec> recs = forItem(st);
		if (recs == null) return;

		Map<String, Integer> have = new HashMap<>();
		for (Object2IntMap.Entry<Holder<Enchantment>> e : st.getEnchantments().entrySet()) {
			e.getKey().unwrapKey().ifPresent(k -> have.put(k.identifier().getPath(), e.getIntValue()));
		}

		Font font = Minecraft.getInstance().font;
		List<Component> lines = new ArrayList<>();
		List<Integer> colors = new ArrayList<>();
		lines.add(Component.literal("✦ Beste Verzauberungen"));
		colors.add(0xFFFFD866);
		int done = 0;
		for (Rec r : recs) {
			int state = 0; // 0 fehlt, 1 zu niedrig, 2 fertig
			String upgrade = null;
			String owned = null; // bei Alternativen: welche man schon hat
			for (Opt o : r.options()) {
				int lvl = have.getOrDefault(o.id(), 0);
				if (lvl >= o.level()) {
					if (state < 2) owned = name(o.id(), lvl).getString();
					state = 2;
				} else if (lvl > 0 && state < 2) {
					state = 1;
					upgrade = name(o.id(), lvl).getString() + " → " + roman(o.level());
				}
			}
			StringBuilder text = new StringBuilder();
			if (state == 2) text.append(owned);
			else if (state == 1) text.append(upgrade);
			else {
				for (int i = 0; i < r.options().size(); i++) {
					if (i > 0) text.append(" / ");
					Opt o = r.options().get(i);
					text.append(name(o.id(), o.level()).getString());
				}
			}
			lines.add(Component.literal((state == 2 ? "✔ " : state == 1 ? "▲ " : "✘ ") + text));
			colors.add(state == 2 ? 0xFF6BE06B : state == 1 ? 0xFFFFD040 : 0xFFB8B8B8);
			if (state == 2) done++;
		}
		lines.add(Component.literal(done + "/" + recs.size() + " erledigt"));
		colors.add(0xFF8A8F98);

		int w = 0;
		for (Component c : lines) w = Math.max(w, font.width(c));
		int boxW = w + 10;
		int boxH = lines.size() * 11 + 6;
		int x = acc.mcguide$leftPos() - boxW - 6;
		if (x < 2) x = 2;
		int y = Math.max(2, acc.mcguide$topPos());
		g.fill(x, y, x + boxW, y + boxH, 0xE0101215);
		g.fill(x, y, x + boxW, y + 1, 0xFF6040A0);
		for (int i = 0; i < lines.size(); i++) {
			g.text(font, lines.get(i), x + 5, y + 4 + i * 11, colors.get(i));
		}
	}

	private static Component name(String id, int level) {
		Component n = Component.translatable("enchantment.minecraft." + id);
		return level <= 1 && maxOne(id) ? n : Component.empty().append(n).append(" " + roman(level));
	}

	private static boolean maxOne(String id) {
		return switch (id) {
			case "mending", "silk_touch", "aqua_affinity", "flame", "infinity", "multishot", "channeling" -> true;
			default -> false;
		};
	}

	private static String roman(int n) {
		return switch (n) {
			case 1 -> "I";
			case 2 -> "II";
			case 3 -> "III";
			case 4 -> "IV";
			case 5 -> "V";
			default -> String.valueOf(n);
		};
	}

	private EnchantAdvisor() {}
}
