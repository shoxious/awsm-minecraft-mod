package de.shoxi.mcguide;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import de.shoxi.mcguide.mixin.SignEditScreenAccessor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * /sortieren: ordnet alle Kisten und Faesser in Reichweite komplett neu, nach Material in Kreativ-Reihenfolge
 * (Baubloecke, Natur, Werkzeuge, ...), oben links beginnend. Rein clientseitig mit normalen Klicks:
 * 1. Runde: jede Kiste einmal oeffnen und Inhalt merken, daraus den Plan berechnen (welche Sorte wie viele Plaetze in welcher Kiste).
 * Danach Runden, bis nichts mehr passiert: Kiste oeffnen, Passendes aus dem Inventar hinein,
 * Unpassendes ins Inventar (ist das voll, wird getauscht). Das Hauptinventar ist der Zwischenspeicher, die Schnellleiste bleibt unberuehrt.
 */
final class ChestSorter {
	private static final int OPEN_TIMEOUT = 40;
	private static final int MAX_ROUNDS = 30;

	private static final class Box {
		final BlockPos pos;
		int size;
		final List<ItemStack> scanned = new ArrayList<>();
		/** Sorte -> erlaubte Plaetze in dieser Kiste. */
		final Map<Item, Integer> quota = new IdentityHashMap<>();
		/** Text vom Schild an der Kiste, sonst null. */
		final String label;
		/** Beide Haelften einer Doppelkiste (sonst nur pos). */
		final List<BlockPos> parts;
		/** /lager: neu anzubringende Beschriftung. */
		String newLabel;
		boolean broken;

		/** Aufgestellte Shulkerkiste: wird nur ausgeleert, nie befuellt oder beschriftet. */
		final boolean source;

		Box(BlockPos pos, List<BlockPos> parts, String label, boolean source) {
			this.pos = pos;
			this.parts = parts;
			this.label = label;
			this.source = source;
		}
	}

	private enum Stage { SCAN, CLASSIFY, MOVE, DESIGN, SIGNS, RESTART }

	private static List<Box> boxes;
	private static Stage stage;
	private static int index;
	private static int round;
	private static boolean progress;
	private static boolean open;
	private static int wait;
	private static int lastMenu = -1;
	/** Fenster der gerade bearbeiteten Kiste, -1 solange es noch nicht offen ist. */
	private static int activeMenu = -1;
	/** Beschriftungs-Modus: Items mit Schild-Kategorie, ohne Kategorie bleiben sie liegen. */
	private static boolean labelMode;
	private static CompletableFuture<Map<Item, String>> categories;
	/** Items ohne passende Kiste – werden nie herausgenommen. */
	private static final Set<Item> keep = new HashSet<>();
	/** /lager: erst planen und beschriften, danach normal sortieren. */
	private static boolean designMode;
	private static CompletableFuture<ItemCategories.Plan> design;
	private static int signPhase;
	/** Shulker, die beim letzten Lauf nachweislich leer waren – nur die darf der Bauplan abbauen. */
	static final Set<BlockPos> emptied = new HashSet<>();

	/** Nur ansehen: alle Behaelter oeffnen und den Inhalt nach config/mcguide-inhalt.txt schreiben. */
	private static boolean scanOnly;

	static boolean running() {
		return boxes != null;
	}

	static void abort(Minecraft mc) {
		if (boxes != null) stop(mc, "Sortieren abgebrochen");
	}

	static String stageName() {
		return boxes == null ? "aus" : stage + " " + (index + 1) + "/" + boxes.size();
	}

	static void scanOnly(Minecraft mc) {
		if (mc.player == null || mc.level == null || boxes != null) return;
		List<Box> found = find(mc);
		if (found.isEmpty()) {
			msg(mc, "Lager: keine Behälter in Reichweite", ChatFormatting.GRAY);
			return;
		}
		boxes = found;
		keep.clear();
		emptied.clear();
		categories = null;
		designMode = false;
		labelMode = true;
		scanOnly = true;
		stage = Stage.SCAN;
		index = round = 0;
		open = false;
		msg(mc, "Lager: sehe mir " + found.size() + " Behälter an …", ChatFormatting.GREEN);
	}

	/** Schilder mit vorgegebenem Text an Kisten ohne Schild (Schluessel = eine Position der Kiste), danach sortieren. */
	/** Nach dem Anbringen der Schilder sortieren (sonst nur beschriften). */
	private static boolean sortAfterSigns = true;

	static void labelAndSort(Minecraft mc, Map<BlockPos, String> labels, boolean sort) {
		sortAfterSigns = sort;
		if (mc.player == null || mc.level == null || boxes != null) return;
		List<Box> found = find(mc);
		int n = 0;
		for (Box b : found) {
			if (b.source || b.label != null) continue;
			for (BlockPos p : b.parts) {
				if (labels.containsKey(p)) {
					b.newLabel = labels.get(p);
					n++;
					break;
				}
			}
		}
		boxes = found;
		keep.clear();
		categories = null;
		designMode = false;
		scanOnly = false;
		stage = Stage.SIGNS;
		index = 0;
		signPhase = 0;
		wait = 0;
		if (Hotbar.count(mc, ChestSorter::isSign) < n) msg(mc, "Lager: " + n + " Schilder nötig, nicht genug dabei", ChatFormatting.YELLOW);
		msg(mc, "Lager: bringe " + n + " Schilder an …", ChatFormatting.GREEN);
	}

	private static void writeInventory(Minecraft mc) {
		StringBuilder out = new StringBuilder("Behälter (Position | Art | Plätze | Schild | Inhalt id=Anzahl):\n");
		for (Box b : boxes) {
			if (b.broken) {
				out.append(b.pos.toShortString()).append(" | ließ sich nicht öffnen\n");
				continue;
			}
			Map<String, Integer> items = new java.util.TreeMap<>();
			for (ItemStack it : b.scanned) items.merge(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(it.getItem()).getPath(), it.getCount(), Integer::sum);
			out.append(b.parts.stream().map(BlockPos::toShortString).toList()).append(" | ")
				.append(b.source ? "shulker" : mc.level.getBlockState(b.pos).getBlock() instanceof BarrelBlock ? "fass" : "kiste")
				.append(" | ").append(b.size).append(" | ").append(b.label == null ? "-" : b.label)
				.append(" | belegt ").append(b.scanned.size()).append(" | ").append(items).append('\n');
		}
		try {
			java.nio.file.Files.writeString(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("mcguide-inhalt.txt"),
				out.toString(), java.nio.charset.StandardCharsets.UTF_8);
		} catch (Exception e) {
			GuideServer.LOG.warn("Inhalt nicht gespeichert", e);
		}
	}

	static void start(Minecraft mc) {
		start(mc, false);
	}

	/** /lager: Claude plant aus allem, was da ist, Kategorien fuer die unbeschrifteten Kisten; Schilder anbringen, dann sortieren. */
	static void design(Minecraft mc) {
		start(mc, true);
	}

	private static void start(Minecraft mc, boolean planFirst) {
		if (mc.player == null || mc.level == null) return;
		if (boxes != null) {
			stop(mc, "Sortieren abgebrochen");
			return;
		}
		List<Box> found = find(mc);
		if (found.isEmpty()) {
			msg(mc, "Sortieren: keine Kisten oder Fässer in Reichweite", ChatFormatting.GRAY);
			return;
		}
		designMode = planFirst;
		if (planFirst) {
			if (!ItemCategories.hasClaude()) {
				msg(mc, "Lager: Claude Code (Befehl claude) nicht gefunden", ChatFormatting.RED);
				return;
			}
			if (found.stream().allMatch(b -> b.label != null || b.source)) {
				msg(mc, "Lager: alle Kisten haben schon ein Schild – stell leere Kisten dazu", ChatFormatting.YELLOW);
				return;
			}
			if (Hotbar.count(mc, ChestSorter::isSign) == 0) {
				msg(mc, "Lager: du brauchst Schilder im Inventar", ChatFormatting.YELLOW);
				return;
			}
		}
		labelMode = planFirst || found.stream().anyMatch(b -> b.label != null);
		if (!labelMode) found.removeIf(b -> b.source); // Neuordnen ohne Schilder: Shulker nicht anfassen
		if (found.stream().allMatch(b -> b.source)) {
			msg(mc, "Sortieren: nur Shulker in Reichweite, keine Kisten", ChatFormatting.GRAY);
			return;
		}
		// ohne Schilder wird alles neu verteilt, dafuer muss das Inventar als Zwischenlager frei sein;
		// mit Schildern wird der Inhalt des Hauptinventars gleich mit eingelagert
		if (!labelMode) {
			for (int i = 9; i < 36; i++) {
				if (!mc.player.getInventory().getItem(i).isEmpty()) {
					msg(mc, "Sortieren: erst das Inventar leeren (die Schnellleiste darf bleiben)", ChatFormatting.YELLOW);
					return;
				}
			}
		}
		boxes = found;
		keep.clear();
		emptied.clear();
		categories = null;
		stage = Stage.SCAN;
		index = round = 0;
		open = false;
		msg(mc, "Sortieren: " + boxes.size() + " Kisten – erst alles ansehen, nicht bewegen (/sortieren = abbrechen)", ChatFormatting.GREEN);
	}

	/** Kisten (Doppelkiste nur einmal) und Faesser in Reichweite, oben links beginnend aus Spielersicht. */
	private static List<Box> find(Minecraft mc) {
		List<Box> list = new ArrayList<>();
		int r = (int) Math.ceil(mc.player.blockInteractionRange()) + 1;
		BlockPos c = mc.player.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(c.offset(-r, -r, -r), c.offset(r, r + 2, r))) {
			BlockState st = mc.level.getBlockState(pos);
			boolean chest = st.getBlock() instanceof ChestBlock && st.getValue(ChestBlock.TYPE) != ChestType.LEFT;
			if ((chest || st.getBlock() instanceof BarrelBlock) && mc.player.isWithinBlockInteractionRange(pos, 0)) {
				List<BlockPos> parts = new ArrayList<>(List.of(pos.immutable()));
				if (chest && st.getValue(ChestBlock.TYPE) != ChestType.SINGLE) parts.add(ChestBlock.getConnectedBlockPos(pos, st));
				String label = ItemCategories.label(mc.level, parts);
				// Einwurf-Kiste: wird nur ausgeleert, ist keine Kategorie
				boolean intake = label != null && ItemCategories.norm(label).matches("einwurf|eingang|input");
				list.add(new Box(pos.immutable(), parts, intake ? null : label, intake));
			} else if (st.getBlock() instanceof ShulkerBoxBlock && mc.player.isWithinBlockInteractionRange(pos, 0)) {
				list.add(new Box(pos.immutable(), List.of(pos.immutable()), null, true));
			}
		}
		Direction right = mc.player.getDirection().getClockWise();
		list.sort(Comparator.comparingInt((Box b) -> -b.pos.getY())
			.thenComparingInt(b -> b.pos.getX() * right.getStepX() + b.pos.getZ() * right.getStepZ())
			.thenComparingDouble(b -> b.pos.distSqr(c)));
		return list;
	}

	static void tick(Minecraft mc) {
		if (boxes == null) return;
		if (mc.player == null || mc.gameMode == null) {
			boxes = null;
			return;
		}
		if (stage == Stage.CLASSIFY) {
			if (categories.isDone()) applyCategories(mc);
			return;
		}
		if (stage == Stage.DESIGN) {
			if (design.isDone()) applyDesign(mc);
			return;
		}
		if (stage == Stage.SIGNS) {
			signTick(mc);
			return;
		}
		if (stage == Stage.RESTART) {
			if (++wait < 20) return; // Schildtexte vom Server abwarten
			boxes = null;
			start(mc, false);
			return;
		}
		AbstractContainerMenu menu = switch (mc.gui.screen()) {
			case ContainerScreen s when s.getMenu() instanceof ChestMenu m -> m;
			case ShulkerBoxScreen s -> s.getMenu();
			case null, default -> null;
		};
		Box box = boxes.get(index);
		if (!open) {
			if (menu != null) return; // altes Fenster schliesst noch
			if (mc.player.isShiftKeyDown()) return;
			mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(box.pos), Direction.UP, box.pos, false));
			open = true;
			wait = 0;
			return;
		}
		if (activeMenu != -1 && (menu == null || menu.containerId != activeMenu)) {
			stop(mc, "Sortieren abgebrochen (Fenster geschlossen)");
			return;
		}
		if (menu == null || menu.containerId == lastMenu) {
			if (++wait > OPEN_TIMEOUT) {
				box.broken = true; // laesst sich nicht oeffnen (Block drueber, Katze ...)
				next(mc);
			}
			return;
		}
		if (activeMenu == -1) {
			activeMenu = menu.containerId;
			wait = 0;
		}
		if (++wait < 3) return; // Inhalt kommt kurz nach dem Fenster
		if (stage == Stage.SCAN) {
			box.size = sizeOf(menu);
			for (int i = 0; i < box.size; i++) if (!menu.getSlot(i).getItem().isEmpty()) box.scanned.add(menu.getSlot(i).getItem().copy());
			if (box.source && box.scanned.isEmpty()) emptied.add(box.pos);
			close(mc, menu);
			next(mc);
			return;
		}
		if (!step(mc, menu, box)) {
			if (box.source) {
				boolean empty = true;
				for (int i = 0; i < sizeOf(menu); i++) empty &= menu.getSlot(i).getItem().isEmpty();
				if (empty) emptied.add(box.pos);
			}
			close(mc, menu);
			next(mc);
		}
	}

	private static void next(Minecraft mc) {
		open = false;
		activeMenu = -1;
		if (++index < boxes.size()) return;
		index = 0;
		if (stage == Stage.SCAN && scanOnly) {
			writeInventory(mc);
			stop(mc, "Lager: Inhalt notiert (config/mcguide-inhalt.txt)");
			return;
		}
		if (stage == Stage.SCAN && designMode) {
			askDesign(mc);
			return;
		}
		if (stage == Stage.SCAN && labelMode) {
			classify(mc);
			return;
		}
		if (stage == Stage.SCAN) {
			if (!plan(mc)) return;
			stage = Stage.MOVE;
			round = 0;
			progress = false;
			msg(mc, "Sortieren: Plan steht, räume um …", ChatFormatting.GREEN);
			return;
		}
		if (!progress || ++round >= MAX_ROUNDS) {
			int left = 0;
			for (int i = 9; i < 36; i++) if (!mc.player.getInventory().getItem(i).isEmpty()) left++;
			stop(mc, left == 0 ? "Sortieren fertig" : "Sortieren fertig – " + left + " Stapel haben keine passende oder keine freie Kiste und sind im Inventar");
			return;
		}
		progress = false;
	}

	// ---- /lager: planen und Schilder anbringen ----

	private static void askDesign(Minecraft mc) {
		boxes.removeIf(b -> b.broken);
		if (boxes.stream().allMatch(b -> b.label != null || b.source)) {
			stop(mc, "Lager: keine unbeschriftete Kiste ließ sich öffnen");
			return;
		}
		// Reihe/Spalte aus Spielersicht, damit Claude Nachbarschaften planen kann
		Direction right = mc.player.getDirection().getClockWise();
		List<Integer> ys = boxes.stream().map(b -> -b.pos.getY()).distinct().sorted().toList();
		List<Integer> xs = boxes.stream().map(b -> b.pos.getX() * right.getStepX() + b.pos.getZ() * right.getStepZ()).distinct().sorted().toList();
		List<ItemCategories.ChestInfo> infos = new ArrayList<>();
		List<ItemStack> items = new ArrayList<>();
		for (int i = 0; i < boxes.size(); i++) {
			Box b = boxes.get(i);
			if (b.source) {
				items.addAll(b.scanned);
				continue;
			}
			int col = xs.indexOf(b.pos.getX() * right.getStepX() + b.pos.getZ() * right.getStepZ()) + 1;
			infos.add(new ItemCategories.ChestInfo(i, ys.indexOf(-b.pos.getY()) + 1, col, b.size, b.scanned.size(), b.label));
			items.addAll(b.scanned);
		}
		for (int i = 9; i < 36; i++) if (!mc.player.getInventory().getItem(i).isEmpty()) items.add(mc.player.getInventory().getItem(i).copy());
		stage = Stage.DESIGN;
		design = ItemCategories.plan(infos, items);
		msg(mc, "Lager: Claude plant dein Lager …", ChatFormatting.GREEN);
	}

	private static void applyDesign(Minecraft mc) {
		ItemCategories.Plan plan;
		try {
			plan = design.join();
		} catch (RuntimeException e) {
			GuideServer.LOG.warn("Lagerplanung fehlgeschlagen", e);
			stop(mc, "Lager: Planung fehlgeschlagen (siehe Log)");
			return;
		}
		int n = 0;
		if (plan != null && plan.chests() != null) {
			for (ItemCategories.ChestLabel c : plan.chests()) {
				if (c.id() < 0 || c.id() >= boxes.size() || c.label() == null || c.label().isBlank()) continue;
				Box b = boxes.get(c.id());
				if (b.label != null || b.source) continue;
				b.newLabel = c.label().trim();
				n++;
			}
		}
		if (n == 0) {
			stop(mc, "Lager: Claude hat keine Beschriftungen vorgeschlagen");
			return;
		}
		mc.player.sendSystemMessage(Component.literal("📦 Lagerplan: ").withStyle(ChatFormatting.GOLD)
			.append(Component.literal(plan.summary() == null ? "" : plan.summary()).withStyle(ChatFormatting.WHITE)));
		int signs = Hotbar.count(mc, ChestSorter::isSign);
		if (signs < n) msg(mc, "Lager: " + n + " Schilder nötig, " + signs + " dabei – der Rest bleibt unbeschriftet", ChatFormatting.YELLOW);
		stage = Stage.SIGNS;
		index = 0;
		signPhase = 0;
		wait = 0;
	}

	/** Ein Schild nach dem anderen: Schild waehlen, schleichen, an die Kiste klicken, Text eintragen. */
	private static void signTick(Minecraft mc) {
		while (index < boxes.size() && boxes.get(index).newLabel == null) index++;
		if (index >= boxes.size()) {
			release(mc);
			if (!sortAfterSigns) {
				sortAfterSigns = true;
				stop(mc, "Lager: Schilder hängen");
				return;
			}
			msg(mc, "Lager: Schilder hängen – jetzt wird einsortiert …", ChatFormatting.GREEN);
			stage = Stage.RESTART;
			wait = 0;
			return;
		}
		Box box = boxes.get(index);
		switch (signPhase) {
			case 0 -> {
				int slot = Hotbar.bring(mc, ChestSorter::isSign);
				if (slot < 0) {
					release(mc);
					msg(mc, "Lager: keine Schilder mehr – einsortiert wird trotzdem", ChatFormatting.YELLOW);
					stage = Stage.RESTART;
					wait = 0;
					return;
				}
				mc.player.getInventory().setSelectedSlot(slot);
				mc.options.keyShift.setDown(true); // schleichend klicken: Schild anbringen statt Kiste oeffnen
				wait = 0;
				signPhase = 1;
			}
			case 1 -> {
				if (++wait < 3) return;
				BlockHitResult hit = signSpot(mc, box);
				if (hit == null) {
					msg(mc, "Lager: kein Platz für ein Schild an einer Kiste", ChatFormatting.YELLOW);
					finishSign(mc, box);
					return;
				}
				mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
				wait = 0;
				signPhase = 2;
			}
			default -> {
				if (mc.gui.screen() instanceof AbstractSignEditScreen screen) {
					String[] lines = ((SignEditScreenAccessor) screen).mcguide$messages();
					List<String> text = signLines(box.newLabel);
					for (int i = 0; i < lines.length; i++) lines[i] = i < text.size() ? text.get(i) : "";
					screen.onClose(); // schickt den Text an den Server
					finishSign(mc, box);
				} else if (++wait > OPEN_TIMEOUT) {
					finishSign(mc, box);
				}
			}
		}
	}

	private static void finishSign(Minecraft mc, Box box) {
		release(mc);
		box.newLabel = null;
		index++;
		signPhase = 0;
	}

	private static void release(Minecraft mc) {
		mc.options.keyShift.setDown(false);
	}

	/** Freie Seite der Kiste, moeglichst zum Spieler hin; sonst obendrauf. */
	private static BlockHitResult signSpot(Minecraft mc, Box box) {
		Vec3 eye = mc.player.getEyePosition();
		List<Direction> sides = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
		sides.sort(Comparator.comparingDouble((Direction d) -> -(d.getStepX() * (eye.x - box.pos.getX() - 0.5) + d.getStepZ() * (eye.z - box.pos.getZ() - 0.5))));
		for (Direction d : sides) {
			for (BlockPos part : box.parts) {
				if (mc.level.getBlockState(part.relative(d)).canBeReplaced()) {
					return new BlockHitResult(Vec3.atCenterOf(part).add(d.getStepX() * 0.5, 0, d.getStepZ() * 0.5), d, part, false);
				}
			}
		}
		if (mc.level.getBlockState(box.pos.above()).canBeReplaced()) {
			return new BlockHitResult(Vec3.atCenterOf(box.pos).add(0, 0.5, 0), Direction.UP, box.pos, false);
		}
		return null;
	}

	static boolean isSign(ItemStack s) {
		return !s.isEmpty() && Block.byItem(s.getItem()) instanceof StandingSignBlock;
	}

	/** Beschriftung auf bis zu 4 Zeilen zu je 15 Zeichen umbrechen. */
	static List<String> signLines(String label) {
		List<String> lines = new ArrayList<>();
		StringBuilder cur = new StringBuilder();
		for (String w : label.trim().split("\\s+")) {
			if (w.length() > 15) w = w.substring(0, 15);
			if (cur.length() > 0 && cur.length() + 1 + w.length() > 15) {
				lines.add(cur.toString());
				cur.setLength(0);
			}
			if (cur.length() > 0) cur.append(' ');
			cur.append(w);
		}
		if (cur.length() > 0) lines.add(cur.toString());
		return lines.size() > 4 ? lines.subList(0, 4) : lines;
	}

	/** Beschriftungs-Modus: alle gesehenen Items (Kisten + Hauptinventar) den Schildern zuordnen lassen. */
	private static void classify(Minecraft mc) {
		boxes.removeIf(b -> b.broken);
		List<String> labels = new ArrayList<>();
		for (Box b : boxes) if (b.label != null && labels.stream().noneMatch(l -> ItemCategories.norm(l).equals(ItemCategories.norm(b.label)))) labels.add(b.label);
		if (boxes.isEmpty() || labels.isEmpty()) {
			stop(mc, "Sortieren: keine beschriftete Kiste ließ sich öffnen");
			return;
		}
		List<ItemStack> items = new ArrayList<>();
		for (Box b : boxes) items.addAll(b.scanned);
		for (int i = 9; i < 36; i++) if (!mc.player.getInventory().getItem(i).isEmpty()) items.add(mc.player.getInventory().getItem(i).copy());
		stage = Stage.CLASSIFY;
		categories = ItemCategories.classify(mc, labels, items);
		msg(mc, ItemCategories.hasClaude() ? "Sortieren: Claude ordnet " + labels.size() + " Schildern zu …" : "Sortieren: ordne " + labels.size() + " Schildern zu …", ChatFormatting.GREEN);
	}

	private static void applyCategories(Minecraft mc) {
		Map<Item, String> cat;
		try {
			cat = categories.join();
		} catch (RuntimeException e) {
			GuideServer.LOG.warn("Zuordnung fehlgeschlagen", e);
			stop(mc, "Sortieren: Zuordnung fehlgeschlagen (siehe Log)");
			return;
		}
		int homeless = 0;
		for (Map.Entry<Item, String> e : cat.entrySet()) {
			if (e.getValue().isEmpty()) {
				// hat das Item eine feste Kiste ausserhalb der Reichweite, wird es mitgenommen statt liegengelassen
				String elsewhere = ItemCategories.fixedLabel(e.getKey());
				if (elsewhere == null || elsewhere.isBlank()) {
					keep.add(e.getKey());
					homeless++;
				}
				continue;
			}
			for (Box b : boxes) {
				if (b.label != null && ItemCategories.norm(b.label).equals(ItemCategories.norm(e.getValue()))) b.quota.put(e.getKey(), Integer.MAX_VALUE);
			}
		}
		stage = Stage.MOVE;
		index = round = 0;
		progress = false;
		msg(mc, "Sortieren: räume ein …" + (homeless > 0 ? " (" + homeless + " Sorten ohne passendes Schild bleiben liegen)" : ""), ChatFormatting.GREEN);
	}

	/** Berechnet, welche Sorte wie viele Plaetze in welcher Kiste bekommt. */
	private static boolean plan(Minecraft mc) {
		boxes.removeIf(b -> b.broken);
		if (boxes.isEmpty()) {
			stop(mc, "Sortieren: keine Kiste ließ sich öffnen");
			return false;
		}
		// Plaetze pro Sorte: gleiche Stapel (gleiche Komponenten) zusammengelegt
		List<ItemStack> merged = new ArrayList<>();
		for (Box b : boxes) {
			for (ItemStack s : b.scanned) {
				ItemStack same = null;
				for (ItemStack m : merged) if (ItemStack.isSameItemSameComponents(m, s)) same = m;
				if (same != null) same.grow(s.getCount());
				else merged.add(s.copy());
			}
		}
		merged.sort(InventorySort.order(mc));
		Map<Item, Integer> need = new LinkedHashMap<>();
		for (ItemStack m : merged) need.merge(m.getItem(), (m.getCount() + m.getMaxStackSize() - 1) / m.getMaxStackSize(), Integer::sum);

		if (!pack(need, true) && !pack(need, false)) {
			stop(mc, "Sortieren: die Kisten sind zu voll, um umzuordnen – eine leere Kiste dazustellen");
			return false;
		}
		return true;
	}

	/** Verteilt die Sorten der Reihe nach; ordentlich = kleine Sorten nicht ueber zwei Kisten teilen. */
	private static boolean pack(Map<Item, Integer> need, boolean tidy) {
		for (Box b : boxes) b.quota.clear();
		int cur = 0;
		int free = boxes.get(0).size;
		for (Map.Entry<Item, Integer> e : need.entrySet()) {
			int n = e.getValue();
			if (tidy && n > free && free < boxes.get(cur).size && cur + 1 < boxes.size() && n <= boxes.get(cur + 1).size) {
				cur++;
				free = boxes.get(cur).size;
			}
			while (n > 0) {
				if (free == 0) {
					if (++cur >= boxes.size()) return false;
					free = boxes.get(cur).size;
				}
				int take = Math.min(n, free);
				boxes.get(cur).quota.merge(e.getKey(), take, Integer::sum);
				free -= take;
				n -= take;
			}
		}
		return true;
	}

	/** Ein Schritt in der offenen Kiste; false = hier ist nichts mehr zu tun. */
	private static int sizeOf(AbstractContainerMenu menu) {
		return menu instanceof ChestMenu c ? c.getRowCount() * 9 : 27;
	}

	private static boolean step(Minecraft mc, AbstractContainerMenu menu, Box box) {
		int size = sizeOf(menu);
		int inv = size; // Hauptinventar (ohne Schnellleiste) im Fenster: size .. size+26
		Map<Item, Integer> used = new HashMap<>();
		int emptyChest = -1;
		for (int i = 0; i < size; i++) {
			ItemStack s = menu.getSlot(i).getItem();
			if (s.isEmpty()) {
				if (emptyChest < 0) emptyChest = i;
			} else used.merge(s.getItem(), 1, Integer::sum);
		}
		int emptyInv = -1;
		for (int i = inv; i < inv + 27; i++) if (menu.getSlot(i).getItem().isEmpty()) emptyInv = i;

		// 1. Passendes aus dem Inventar hinein
		for (int i = inv; i < inv + 27; i++) {
			ItemStack s = menu.getSlot(i).getItem();
			if (s.isEmpty()) continue;
			int room = box.quota.getOrDefault(s.getItem(), 0) - used.getOrDefault(s.getItem(), 0);
			if (box.quota.getOrDefault(s.getItem(), 0) == 0) continue;
			boolean fits = mergeRoom(menu, size, s) >= s.getCount();
			if (room < 1 && !fits) continue;
			if (emptyChest < 0 && !fits) {
				int wrong = wrongSlot(menu, size, box, used);
				if (wrong < 0 || room < 1) continue;
				click(mc, menu, i); // tauschen: Stapel aufnehmen, gegen Falsches tauschen, Falsches ablegen
				click(mc, menu, wrong);
				click(mc, menu, i);
				return progress = true;
			}
			int before = s.getCount();
			mc.gameMode.handleContainerInput(menu.containerId, i, 0, ContainerInput.QUICK_MOVE, mc.player);
			if (menu.getSlot(i).getItem().getCount() != before || menu.getSlot(i).getItem().isEmpty()) return progress = true;
		}
		// 2. Unpassendes heraus
		int wrong = wrongSlot(menu, size, box, used);
		if (wrong >= 0 && emptyInv >= 0) {
			click(mc, menu, wrong);
			click(mc, menu, emptyInv);
			return progress = true;
		}
		return false;
	}

	/** Wie viel von s noch auf angefangene gleiche Stapel in der Kiste passt. */
	private static int mergeRoom(AbstractContainerMenu menu, int size, ItemStack s) {
		int n = 0;
		for (int i = 0; i < size; i++) {
			ItemStack c = menu.getSlot(i).getItem();
			if (ItemStack.isSameItemSameComponents(c, s)) n += c.getMaxStackSize() - c.getCount();
		}
		return n;
	}

	/** Ein Platz mit einer Sorte, die hier nicht (oder nicht so oft) hingehoert; bei zu vielen der kleinste Stapel. */
	private static int wrongSlot(AbstractContainerMenu menu, int size, Box box, Map<Item, Integer> used) {
		int best = -1;
		for (int i = 0; i < size; i++) {
			ItemStack s = menu.getSlot(i).getItem();
			if (s.isEmpty()) continue;
			if (keep.contains(s.getItem())) continue;
			int quota = box.quota.getOrDefault(s.getItem(), 0);
			if (quota == 0) return i;
			if (used.get(s.getItem()) > quota && (best < 0 || s.getCount() < menu.getSlot(best).getItem().getCount())) best = i;
		}
		return best;
	}

	private static void click(Minecraft mc, AbstractContainerMenu menu, int slot) {
		mc.gameMode.handleContainerInput(menu.containerId, slot, 0, ContainerInput.PICKUP, mc.player);
	}

	private static void close(Minecraft mc, AbstractContainerMenu menu) {
		lastMenu = menu.containerId;
		mc.player.closeContainer();
	}

	private static void stop(Minecraft mc, String text) {
		boxes = null;
		open = false;
		activeMenu = -1;
		if (categories != null) categories.cancel(false);
		categories = null;
		if (design != null) design.cancel(false);
		design = null;
		designMode = false;
		scanOnly = false;
		mc.options.keyShift.setDown(false);
		if (mc.player != null && mc.player.containerMenu != mc.player.inventoryMenu) mc.player.closeContainer();
		msg(mc, text, ChatFormatting.GREEN);
	}

	private static void msg(Minecraft mc, String text, ChatFormatting color) {
		LocalApi.log(text);
		if (mc.player != null) mc.player.sendOverlayMessage(Component.literal(text).withStyle(color));
	}

	private ChestSorter() {}
}
