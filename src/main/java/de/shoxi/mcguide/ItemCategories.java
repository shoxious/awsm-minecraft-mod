package de.shoxi.mcguide;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Kategorien fuer beschriftete Kisten: Schild an der Kiste ("Redstone Zeug") -> welche Items gehoeren hinein.
 * Ist Claude Code installiert (Befehl claude, angemeldet), entscheidet Claude, sonst eingebaute Regeln.
 * Ergebnisse werden je Satz an Beschriftungen in config/mcguide-kategorien.json gemerkt,
 * damit nur neue Items nachgefragt werden.
 */
final class ItemCategories {
	private static final Path CONFIG = FabricLoader.getInstance().getConfigDir();
	private static final Path CACHE = CONFIG.resolve("mcguide-kategorien.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	/** Kein passendes Schild: Item bleibt, wo es ist. */
	static final String NONE = "";

	public record Assignment(String item, String label) {}

	public record Result(List<Assignment> assignments) {}

	/** Beschriftung einer Kiste: Wandschild daran oder Schild obendrauf, bei Doppelkisten an beiden Haelften. */
	static String label(Level level, List<BlockPos> parts) {
		List<String> texts = new ArrayList<>();
		for (BlockPos pos : parts) {
			for (Direction d : Direction.Plane.HORIZONTAL) {
				BlockPos n = pos.relative(d);
				BlockState st = level.getBlockState(n);
				if (st.getBlock() instanceof WallSignBlock && st.getValue(WallSignBlock.FACING) == d) read(level, n, texts);
			}
			if (level.getBlockState(pos.above()).getBlock() instanceof StandingSignBlock) read(level, pos.above(), texts);
		}
		String text = String.join(" ", texts).replaceAll("\\s+", " ").trim();
		return text.isEmpty() ? null : text;
	}

	private static void read(Level level, BlockPos pos, List<String> out) {
		if (!(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) return;
		for (SignTextSlot slot : List.of(SignTextSlot.FRONT, SignTextSlot.BACK)) {
			for (Component line : sign.getText(slot).getMessages(false)) {
				String s = line.getString().trim();
				if (!s.isEmpty()) out.add(s);
			}
		}
	}

	static String norm(String label) {
		return label.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
	}

	/**
	 * Ordnet jedem Item eine der Beschriftungen zu (oder NONE). Laeuft im Hintergrund;
	 * das Ergebnis ist nach Item aufgeschluesselt, die Beschriftungen genau wie uebergeben.
	 */
	static CompletableFuture<Map<Item, String>> classify(Minecraft mc, List<String> labels, List<ItemStack> items) {
		String signature = String.join("|", new TreeSet<>(labels.stream().map(ItemCategories::norm).toList()));
		Map<String, Map<String, String>> cache = loadCache();
		Map<String, String> known = cache.computeIfAbsent(signature, k -> new HashMap<>());
		Map<String, ItemStack> unknown = new LinkedHashMap<>();
		for (ItemStack s : items) {
			String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
			if (!known.containsKey(id)) unknown.putIfAbsent(id, s);
		}
		// Regeln brauchen die Kreativ-Reiter und Namen: auf dem Client-Thread vorbereiten
		// feste Zuordnung von aussen (z. B. von Claude Code geplant) geht immer vor – auch vor Gemerktem.
		// Steht ihre Kiste nicht in Reichweite, bleibt das Label leer (der Sortierer nimmt das Item dann mit).
		Map<String, String> fixed = loadFixed();
		Map<String, String> byNorm = new HashMap<>();
		for (String l : labels) byNorm.put(norm(l), l);
		Map<String, String> fixedNow = new HashMap<>();
		for (ItemStack st : items) {
			String id = BuiltInRegistries.ITEM.getKey(st.getItem()).toString();
			String l = fixed.get(id);
			if (l != null) fixedNow.put(id, l.isBlank() ? NONE : byNorm.getOrDefault(norm(l), NONE));
		}
		unknown.keySet().removeAll(fixedNow.keySet());
		Map<String, String> offline = new HashMap<>();
		for (Map.Entry<String, ItemStack> e : unknown.entrySet()) offline.put(e.getKey(), byRules(mc, labels, e.getValue()));
		Map<String, String> names = new LinkedHashMap<>();
		unknown.forEach((id, s) -> names.put(id, s.getHoverName().getString()));

		return CompletableFuture.supplyAsync(() -> {
			Map<String, String> fresh = new HashMap<>(offline);
			boolean remember = true;
			if (!names.isEmpty() && hasClaude()) {
				try {
					fresh.putAll(askClaude(labels, names));
				} catch (RuntimeException ex) {
					// Notloesung nur fuer diesmal, beim naechsten Mal wieder Claude fragen
					GuideServer.LOG.warn("Claude-Zuordnung fehlgeschlagen, nehme eingebaute Regeln", ex);
					remember = false;
				}
			}
			if (remember) {
				known.putAll(fresh);
				saveCache(cache);
			}
			Map<Item, String> out = new HashMap<>();
			for (ItemStack s : items) {
				String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
				String label = fixedNow.containsKey(id) ? fixedNow.get(id) : known.getOrDefault(id, fresh.getOrDefault(id, NONE));
				out.put(s.getItem(), label);
			}
			return out;
		});
	}

	private static final String SYSTEM = """
		Du sortierst Gegenstände aus Minecraft in beschriftete Kisten. Jede Beschriftung stammt von einem Schild \
		an einer Kiste und beschreibt, was hinein soll, oft umgangssprachlich (z. B. "Redstone Zeug", "Holz", "Essen", "Erze").
		Ordne jeden Gegenstand genau der Beschriftung zu, deren Inhalt er am ehesten ist, und denke dabei an den Spielzweck: \
		"Redstone Zeug" meint alles, was man für Redstone-Schaltungen baut (Redstone, Verstärker, Komparatoren, Kolben, \
		Beobachter, Trichter, Schienen, Druckplatten ...); "Holz" meint Stämme, Bretter und Holzblöcke aller Holzarten.
		Schreibe die Beschriftung exakt so, wie sie in der Liste steht. Ist eine Beschriftung ein Sammelbecken wie \
		"Sonstiges", "Rest" oder "Kram", bekommt sie alles, was sonst nirgends passt. Gibt es kein Sammelbecken und \
		passt keine Beschriftung, gib einen leeren String als label zurück – lieber leer als eine falsche Kiste.
		Gib für jeden Gegenstand der Liste genau einen Eintrag zurück, mit seiner ID als item.""";

	private static final String SCHEMA = """
		{"type":"object","properties":{"assignments":{"type":"array","items":{"type":"object",\
		"properties":{"item":{"type":"string"},"label":{"type":"string"}},"required":["item","label"],\
		"additionalProperties":false}}},"required":["assignments"],"additionalProperties":false}""";

	private static final long TIMEOUT_SECONDS = 300;

	/** Claude Code (Befehl claude) – Prism startet Minecraft ohne Shell-PATH, also die ueblichen Orte absuchen. */
	static Path claudeBinary() {
		String home = System.getProperty("user.home");
		for (String p : List.of(home + "/.local/bin/claude", "/opt/homebrew/bin/claude", "/usr/local/bin/claude",
			home + "/.claude/local/claude", home + "/.npm-global/bin/claude")) {
			Path path = Path.of(p);
			if (Files.isExecutable(path)) return path;
		}
		return null;
	}

	static boolean hasClaude() {
		return claudeBinary() != null;
	}

	/** Fragt Claude Code im Druckmodus (claude -p) ohne Werkzeuge, mit festem JSON-Schema fuer die Antwort. */
	private static Map<String, String> askClaude(List<String> labels, Map<String, String> names) {
		StringBuilder user = new StringBuilder("Beschriftungen der Kisten:\n");
		for (String l : labels) user.append("- ").append(l).append('\n');
		user.append("\nGegenstände (ID = Name im Spiel):\n");
		names.forEach((id, name) -> user.append(id).append(" = ").append(name).append('\n'));

		Result result = GSON.fromJson(runClaude(SYSTEM, SCHEMA, user.toString()), Result.class);
		Map<String, String> byNorm = new HashMap<>();
		for (String l : labels) byNorm.put(norm(l), l);
		Map<String, String> map = new HashMap<>();
		if (result == null || result.assignments() == null) return map;
		for (Assignment a : result.assignments()) {
			if (a.item() == null || !names.containsKey(a.item())) continue;
			map.put(a.item(), a.label() == null ? NONE : byNorm.getOrDefault(norm(a.label()), NONE));
		}
		return map;
	}

	/** claude -p ohne Werkzeuge, MCP und eigene Einstellungen; liefert das zum Schema passende JSON. */
	private static com.google.gson.JsonElement runClaude(String system, String schema, String userText) {
		ProcessBuilder pb = new ProcessBuilder(claudeBinary().toString(), "-p",
			"--setting-sources", "", "--strict-mcp-config", "--tools", "", "--no-session-persistence",
			"--output-format", "json", "--json-schema", schema, "--system-prompt", system);
		pb.directory(CONFIG.toFile());
		pb.redirectError(ProcessBuilder.Redirect.DISCARD);
		try {
			Process proc = pb.start();
			try (Writer w = new java.io.OutputStreamWriter(proc.getOutputStream(), StandardCharsets.UTF_8)) {
				w.write(userText);
			}
			CompletableFuture<String> out = CompletableFuture.supplyAsync(() -> {
				try {
					return new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
				} catch (java.io.IOException e) {
					throw new java.io.UncheckedIOException(e);
				}
			});
			if (!proc.waitFor(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)) {
				proc.destroyForcibly();
				throw new IllegalStateException("Claude Code antwortet nicht");
			}
			com.google.gson.JsonObject res = com.google.gson.JsonParser.parseString(out.join()).getAsJsonObject();
			if (res.has("is_error") && res.get("is_error").getAsBoolean() || !res.has("structured_output")) {
				throw new IllegalStateException("Claude Code: " + (res.has("result") ? res.get("result").getAsString() : "keine Antwort"));
			}
			return res.get("structured_output");
		} catch (java.io.IOException e) {
			throw new java.io.UncheckedIOException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	// ---- Lager planen (/lager) ----

	/** Eine Kiste fuer die Planung; reihe 1 = oben, spalte 1 = links aus Sicht des Spielers. */
	record ChestInfo(int id, int reihe, int spalte, int plaetze, int belegt, String schild) {}

	public record ChestLabel(int id, String label) {}

	public record Plan(List<ChestLabel> chests, String summary) {}

	private static final String PLAN_SYSTEM = """
		Du planst das Lager eines Minecraft-Spielers. Du bekommst seine Kisten (mit Lage an der Wand: reihe 1 = oben, \
		spalte 1 = links, Plätze = Fassungsvermögen, belegt = derzeit belegte Plätze, schild = vorhandene Beschriftung) \
		und alles, was er besitzt, mit der Anzahl Plätze, die es braucht.
		Entwirf ein übersichtliches Lagersystem und vergib für jede Kiste OHNE Schild eine Beschriftung:
		- Kategorien nach Spielzweck und Alltag, kurz und deutsch (max. 2 Wörter, z. B. "Stein", "Holz", "Redstone", \
		"Erze & Barren", "Essen", "Farmen", "Mob-Drops", "Werkzeug", "Deko", "Sonstiges").
		- Jede Kategorie bekommt so viele Kisten, dass ihr Bedarf plus etwa 25 % Reserve hineinpasst; große Mengen \
		(z. B. Stein) bekommen mehrere Kisten mit derselben Beschriftung.
		- Zusammengehöriges nebeneinander, Häufiges in Augenhöhe (mittlere Reihen), Kisten derselben Kategorie zusammenhängend.
		- Vorhandene Schilder bleiben, wie sie sind; nutze ihre Kategorien weiter statt Doppelungen mit anderem Namen.
		- Genau eine Kiste "Sonstiges" für alles Übrige, falls nicht schon vorhanden.
		- Bleiben Kisten übrig, beschrifte sie als Reserve für die Kategorien mit dem meisten Bedarf oder Wachstum.
		Gib in chests nur Kisten ohne Schild zurück (id + label). In summary erklärst du den Plan in 2 bis 5 kurzen Sätzen \
		auf Deutsch, z. B. welche Wand wofür ist.""";

	private static final String PLAN_SCHEMA = """
		{"type":"object","properties":{"chests":{"type":"array","items":{"type":"object",\
		"properties":{"id":{"type":"integer"},"label":{"type":"string"}},"required":["id","label"],\
		"additionalProperties":false}},"summary":{"type":"string"}},"required":["chests","summary"],\
		"additionalProperties":false}""";

	/** Laesst Claude das Lager planen: Beschriftungen fuer die Kisten ohne Schild. */
	static CompletableFuture<Plan> plan(List<ChestInfo> chests, List<ItemStack> items) {
		Map<String, int[]> need = new LinkedHashMap<>(); // id -> {Anzahl, Plaetze}
		Map<String, String> names = new HashMap<>();
		Map<String, Integer> perStack = new HashMap<>();
		for (ItemStack s : items) {
			String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
			names.putIfAbsent(id, s.getHoverName().getString());
			perStack.putIfAbsent(id, s.getMaxStackSize());
			need.computeIfAbsent(id, k -> new int[2])[0] += s.getCount();
		}
		StringBuilder user = new StringBuilder("Kisten:\n");
		for (ChestInfo c : chests) {
			user.append("id ").append(c.id()).append(": reihe ").append(c.reihe()).append(", spalte ").append(c.spalte())
				.append(", ").append(c.plaetze()).append(" Plätze, belegt ").append(c.belegt())
				.append(", schild: ").append(c.schild() == null ? "-" : '"' + c.schild() + '"').append('\n');
		}
		user.append("\nBesitz (ID = Name: Anzahl, Plätze):\n");
		need.forEach((id, n) -> {
			int slots = (n[0] + perStack.get(id) - 1) / perStack.get(id);
			user.append(id).append(" = ").append(names.get(id)).append(": ").append(n[0]).append(", ").append(slots).append('\n');
		});
		return CompletableFuture.supplyAsync(() -> GSON.fromJson(runClaude(PLAN_SYSTEM, PLAN_SCHEMA, user.toString()), Plan.class));
	}

	// ---- eingebaute Regeln (ohne Claude) ----

	private record Rule(String[] words, String tab, String[] ids) {}

	private static final List<Rule> RULES = List.of(
		new Rule(new String[] {"redstone"}, "redstone_blocks", new String[] {"redstone", "repeater", "comparator", "piston", "observer", "hopper", "rail", "lever", "button", "pressure_plate", "dropper", "dispenser", "tripwire", "daylight", "target", "sculk_sensor"}),
		new Rule(new String[] {"holz", "wood"}, null, new String[] {"_log", "_planks", "_wood", "_stem", "_hyphae", "stick", "bamboo_block"}),
		new Rule(new String[] {"stein", "stone", "fels"}, null, new String[] {"stone", "cobble", "deepslate", "granite", "diorite", "andesite", "tuff", "calcite", "blackstone", "basalt", "brick"}),
		new Rule(new String[] {"erz", "ore", "metall", "barren", "mineral"}, null, new String[] {"_ore", "ingot", "raw_", "nugget", "diamond", "emerald", "lapis", "coal", "quartz", "netherite", "amethyst"}),
		new Rule(new String[] {"erde", "dreck", "sand", "kies", "dirt"}, null, new String[] {"dirt", "grass_block", "mud", "gravel", "sand", "clay", "podzol", "mycelium"}),
		new Rule(new String[] {"glas", "glass"}, null, new String[] {"glass"}),
		new Rule(new String[] {"essen", "nahrung", "food", "futter"}, "food_and_drinks", new String[0]),
		new Rule(new String[] {"werkzeug", "tool"}, "tools_and_utilities", new String[0]),
		new Rule(new String[] {"kampf", "waffe", "rüstung", "ruestung", "combat", "armor"}, "combat", new String[0]),
		new Rule(new String[] {"pflanze", "natur", "blume", "samen", "garten", "farm"}, "natural_blocks", new String[0]),
		new Rule(new String[] {"farbe", "bunt", "wolle", "teppich", "beton", "terrakotta", "colou", "color"}, "colored_blocks", new String[0]),
		new Rule(new String[] {"mob", "drop", "beute", "monster"}, null, new String[] {"rotten_flesh", "bone", "string", "spider_eye", "gunpowder", "ender_pearl", "slime_ball", "blaze_rod", "phantom_membrane", "leather", "feather", "arrow", "ghast_tear", "magma_cream"}),
		new Rule(new String[] {"trank", "brau", "potion"}, null, new String[] {"potion", "nether_wart", "blaze_powder", "glass_bottle", "fermented", "glistering", "brewing"}),
		new Rule(new String[] {"zutat", "material", "ingredient"}, "ingredients", new String[0]),
		new Rule(new String[] {"bau", "building", "block"}, "building_blocks", new String[0]),
		new Rule(new String[] {"deko", "funktion"}, "functional_blocks", new String[0]));

	private static final String[] CATCH_ALL = {"sonstig", "rest", "kram", "misc", "allerlei", "diverses"};

	/** Zuordnung ohne Claude: Itemname auf dem Schild > Stichwort mit ID-Teilen > Kreativ-Reiter > Sammelbecken. */
	private static String byRules(Minecraft mc, List<String> labels, ItemStack stack) {
		String name = norm(stack.getHoverName().getString());
		String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
		for (String l : labels) if ((" " + norm(l) + " ").contains(" " + name + " ")) return l;
		for (Rule r : RULES) {
			if (r.ids().length == 0) continue;
			for (String l : labels) {
				if (!mentions(l, r.words())) continue;
				for (String part : r.ids()) if (id.contains(part)) return l;
			}
		}
		for (Rule r : RULES) {
			if (r.tab() == null) continue;
			for (String l : labels) if (mentions(l, r.words()) && inTab(r.tab(), stack)) return l;
		}
		for (String l : labels) if (mentions(l, CATCH_ALL)) return l;
		return NONE;
	}

	private static boolean mentions(String label, String[] words) {
		String n = norm(label);
		for (String w : words) if (n.contains(w)) return true;
		return false;
	}

	private static boolean inTab(String path, ItemStack stack) {
		for (CreativeModeTab tab : CreativeModeTabs.tabs()) {
			Identifier key = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
			if (key != null && key.getPath().equals(path) && tab.contains(stack)) return true;
		}
		return false;
	}

	// ---- Merkliste ----

	private static Map<String, Map<String, String>> loadCache() {
		if (Files.exists(CACHE)) {
			try (Reader r = Files.newBufferedReader(CACHE, StandardCharsets.UTF_8)) {
				Map<String, Map<String, String>> m = GSON.fromJson(r, new TypeToken<Map<String, Map<String, String>>>() {}.getType());
				if (m != null) return m;
			} catch (Exception e) {
				GuideServer.LOG.warn("Kategorien-Merkliste unlesbar, fange neu an", e);
			}
		}
		return new HashMap<>();
	}

	private static synchronized void saveCache(Map<String, Map<String, String>> cache) {
		try (Writer w = Files.newBufferedWriter(CACHE, StandardCharsets.UTF_8)) {
			GSON.toJson(cache, w);
		} catch (Exception e) {
			GuideServer.LOG.warn("Kategorien-Merkliste nicht gespeichert", e);
		}
	}

	private static final Path FIXED = CONFIG.resolve("mcguide-zuordnung.json");

	/** config/mcguide-zuordnung.json: {"minecraft:cobblestone": "Stein Hell Rohr", ...} */
	private static Map<String, String> loadFixed() {
		if (!Files.exists(FIXED)) return Map.of();
		try (Reader r = Files.newBufferedReader(FIXED, StandardCharsets.UTF_8)) {
			Map<String, String> m = GSON.fromJson(r, new TypeToken<Map<String, String>>() {}.getType());
			return m == null ? Map.of() : m;
		} catch (Exception e) {
			GuideServer.LOG.warn("Zuordnung unlesbar", e);
			return Map.of();
		}
	}

	/** Feste Zuordnung eines Items (aus config/mcguide-zuordnung.json), sonst null. */
	static String fixedLabel(Item item) {
		return loadFixed().get(BuiltInRegistries.ITEM.getKey(item).toString());
	}

	/** Merkliste leeren, z. B. wenn Claude etwas falsch einsortiert hat. */
	static void forget() {
		try {
			Files.deleteIfExists(CACHE);
		} catch (Exception e) {
			GuideServer.LOG.warn("Kategorien-Merkliste nicht geloescht", e);
		}
	}

	private ItemCategories() {}
}
