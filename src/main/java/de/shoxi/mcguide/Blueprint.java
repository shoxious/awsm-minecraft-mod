package de.shoxi.mcguide;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.gson.Gson;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * /lager bauplan: fuehrt config/mcguide-bauplan.json aus – Bloecke setzen, /lager laufen lassen,
 * leere Shulker abbauen. Der Bauplan wird ausserhalb des Spiels geschrieben (z. B. von Claude Code
 * anhand von /lager ansehen). Mehrfach ausfuehrbar: Erledigtes wird uebersprungen.
 */
final class Blueprint {
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("mcguide-bauplan.json");

	/**
	 * action: place (item, pos, click, face, yaw), inhalt (yaw: Behaelter oeffnen und Inhalt notieren),
	 * schilder (yaw, labels: Schilder anbringen, dann sortieren), lager (yaw: Claude im Spiel plant), break (pos).
	 */
	record Step(String action, String item, int[] pos, int[] click, String face, Float yaw, List<Label> labels, Boolean sortieren, Integer count,
		List<String> grid, double[] hit, Float pitch, String fuel, Boolean schleichen) {}

	record Label(int[] pos, String text) {}

	/** nacht = Schritte zum Bett (bett), morgen = Schritte zurueck an die Arbeit; dazwischen wird geschlafen. */
	record Plan(int[] stand, List<Step> steps, List<Step> nacht, List<Step> morgen, int[] bett) {}

	/** unterbrochener Hauptplan waehrend der Nachtruhe */
	private static Plan paused;
	private static int pausedIndex;
	private static boolean sleptTonight;

	private static Plan plan;
	private static int index;
	private static int phase;
	private static int wait;

	static void start(Minecraft mc) {
		if (mc.player == null) return;
		if (plan != null) {
			finish(mc, "Bauplan abgebrochen", ChatFormatting.YELLOW);
			return;
		}
		Plan p;
		try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
			p = new Gson().fromJson(r, Plan.class);
		} catch (Exception e) {
			msg(mc, "Bauplan: " + FILE.getFileName() + " fehlt oder ist kaputt", ChatFormatting.RED);
			return;
		}
		start(mc, p);
	}

	/** Plan ausfuehren (aus der Datei oder ueber die lokale Schnittstelle). */
	static boolean start(Minecraft mc, Plan p) {
		if (mc.player == null || plan != null || p == null || p.steps() == null) return false;
		if (p.stand() != null && !mc.player.blockPosition().equals(pos(p.stand()))) {
			msg(mc, "Bauplan: stell dich auf " + p.stand()[0] + " " + p.stand()[1] + " " + p.stand()[2], ChatFormatting.YELLOW);
			return false;
		}
		plan = p;
		index = phase = wait = 0;
		msg(mc, "Bauplan: " + p.steps().size() + " Schritte – nicht bewegen (/lager bauplan = abbrechen)", ChatFormatting.GREEN);
		return true;
	}

	static boolean running() {
		return plan != null;
	}

	static String progress() {
		return plan == null ? "aus" : (index + 1) + "/" + plan.steps().size() + " " + plan.steps().get(Math.min(index, plan.steps().size() - 1)).action();
	}

	static void abort(Minecraft mc) {
		paused = null;
		if (plan != null) finish(mc, "Bauplan abgebrochen", ChatFormatting.YELLOW);
		release(mc);
	}

	static void tick(Minecraft mc) {
		if (plan == null) return;
		if (mc.player == null || mc.gameMode == null) {
			plan = null;
			return;
		}
		if (index >= plan.steps().size()) {
			if (paused != null) { // Nachtruhe vorbei: Hauptplan fortsetzen
				plan = paused;
				index = pausedIndex;
				paused = null;
				phase = wait = 0;
				msg(mc, "Bauplan: guten Morgen – weiter geht's", ChatFormatting.GREEN);
				return;
			}
			finish(mc, "Bauplan fertig", ChatFormatting.GREEN);
			return;
		}
		if (!night(mc)) sleptTonight = false;
		// Nachts mit festem Weg ins Bett (nur zwischen zwei Schritten)
		if (phase == 0 && paused == null && plan.nacht() != null && plan.bett() != null && night(mc) && !sleptTonight) {
			sleptTonight = true;
			paused = plan;
			pausedIndex = index;
			List<Step> night = new java.util.ArrayList<>(plan.nacht());
			night.add(new Step("schlafen", null, plan.bett(), null, null, null, null, null, null, null, null, null, null, null));
			if (plan.morgen() != null) night.addAll(plan.morgen());
			plan = new Plan(null, night, null, null, null);
			index = 0;
			msg(mc, "Bauplan: es wird dunkel – gehe ins Haus schlafen", ChatFormatting.GRAY);
			return;
		}
		Step st = plan.steps().get(index);
		// ohne festen Nachtweg: nur ein Bett in der Naehe nutzen
		if (phase == 0 && paused == null && plan.nacht() == null && sleepTick(mc)) return;
		switch (st.action()) {
			case "place" -> place(mc, st);
			case "lager", "inhalt", "schilder", "sortieren" -> lager(mc, st);
			case "holen" -> fetch(mc, st);
			case "craft" -> craft(mc, st);
			case "use" -> use(mc, st);
			case "ofen" -> smelt(mc, st);
			case "leeren", "ablegen" -> transfer(mc, st);
			case "break" -> breakBlock(mc, st);
			case "goto" -> walk(mc, st);
			case "schlafen" -> sleepAt(mc, st);
			case "klettern" -> climb(mc, st);
			case "anziehen" -> dress(mc, st);
			case "saege" -> saw(mc, st);
			case "look" -> {
				mc.player.setYRot(st.yaw() == null ? mc.player.getYRot() : st.yaw());
				next();
			}
			default -> next();
		}
	}

	private static void place(Minecraft mc, Step st) {
		BlockPos pos = pos(st.pos());
		switch (phase) {
			case 0 -> {
				if (!mc.level.getBlockState(pos).canBeReplaced()) { // schon gesetzt oder belegt
					next();
					return;
				}
				int slot = Hotbar.bring(mc, s -> is(s, st.item()));
				if (slot < 0) {
					finish(mc, "Bauplan: kein " + st.item() + " mehr im Inventar", ChatFormatting.RED);
					return;
				}
				// schleichen: an Truhen/Trichter anbauen, ohne sie zu oeffnen
				mc.options.keyShift.setDown(Boolean.TRUE.equals(st.schleichen()));
				mc.player.getInventory().setSelectedSlot(slot);
				look(mc, hit(st).getLocation(), st.yaw());
				if (st.pitch() != null) mc.player.setXRot(st.pitch());
				phase = 1;
				wait = 0;
			}
			case 1 -> {
				if (++wait < 3) return; // Blickrichtung muss beim Server sein, sie bestimmt die Kistenfront
				mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit(st));
				phase = 2;
				wait = 0;
			}
			default -> {
				if (++wait < 4) return;
				mc.options.keyShift.setDown(false);
				if (mc.level.getBlockState(pos).canBeReplaced()) msg(mc, "Bauplan: an " + pos.toShortString() + " ließ sich nichts setzen", ChatFormatting.YELLOW);
				next();
			}
		}
	}

	private static void lager(Minecraft mc, Step st) {
		if (phase == 0) {
			if (st.yaw() != null) {
				mc.player.setYRot(st.yaw());
				mc.player.setXRot(10);
			}
			phase = 1;
			wait = 0;
		} else if (phase == 1) {
			if (++wait < 3) return;
			switch (st.action()) {
				case "inhalt" -> ChestSorter.scanOnly(mc);
				case "schilder" -> {
					java.util.Map<BlockPos, String> map = new java.util.HashMap<>();
					if (st.labels() != null) for (Label l : st.labels()) map.put(pos(l.pos()), l.text());
					ChestSorter.labelAndSort(mc, map, st.sortieren() == null || st.sortieren());
				}
				case "sortieren" -> ChestSorter.start(mc);
				default -> ChestSorter.design(mc);
			}
			phase = 2;
		} else if (!ChestSorter.running()) {
			next();
		}
	}

	private static void breakBlock(Minecraft mc, Step st) {
		BlockPos pos = pos(st.pos());
		boolean shulker = mc.level.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock;
		if (phase == 0) {
			if (mc.level.getBlockState(pos).isAir()) {
				next();
				return;
			}
			// Shulker behalten ihren Inhalt beim Abbauen – nur nachweislich leere abbauen, ausser bewusst zum Tragen (count = 1)
			if (shulker && !ChestSorter.emptied.contains(pos) && (st.count() == null || st.count() != 1)) {
				msg(mc, "Bauplan: Shulker " + pos.toShortString() + " ist nicht leer, bleibt stehen", ChatFormatting.YELLOW);
				next();
				return;
			}
			// schnellstes Werkzeug der Schnellleiste fuer diesen Block
			var state = mc.level.getBlockState(pos);
			int slot = 0;
			float best = -1;
			for (int i = 0; i < 9; i++) {
				float sp = mc.player.getInventory().getItem(i).getDestroySpeed(state);
				if (sp > best) {
					best = sp;
					slot = i;
				}
			}
			mc.player.getInventory().setSelectedSlot(slot);
			look(mc, Vec3.atCenterOf(pos), null);
			phase = 1;
			wait = 0;
			return;
		}
		Direction face = Direction.getApproximateNearest(mc.player.getEyePosition().subtract(Vec3.atCenterOf(pos)));
		if (phase == 1) {
			if (++wait < 2) return;
			mc.gameMode.startDestroyBlock(pos, face);
			phase = 2;
			wait = 0;
			return;
		}
		if (mc.level.getBlockState(pos).isAir() || ++wait > 100) {
			mc.gameMode.stopDestroyBlock();
			next();
			return;
		}
		mc.gameMode.continueDestroyBlock(pos, face);
	}

	/** Gegenstand item in die Hand nehmen und auf click/face anwenden (z. B. Wassereimer in Kessel). */
	private static void use(Minecraft mc, Step st) {
		if (phase == 0) {
			int slot = Hotbar.bring(mc, s -> is(s, st.item()));
			if (slot < 0) {
				msg(mc, "Bauplan: kein " + st.item() + " im Inventar", ChatFormatting.YELLOW);
				next();
				return;
			}
			mc.options.keyShift.setDown(false);
			mc.player.getInventory().setSelectedSlot(slot);
			look(mc, hit(st).getLocation(), st.yaw());
			phase = 1;
			wait = 0;
		} else if (phase == 1) {
			if (++wait < 3) return;
			mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit(st));
			phase = 2;
			wait = 0;
		} else if (++wait >= 4) {
			next();
		}
	}

	private static int taken;

	/** Rüstungsständer nahe pos: die Gegenstände aus grid nacheinander in die Hand nehmen und anwenden. */
	private static void dress(Minecraft mc, Step st) {
		Vec3 c = Vec3.atBottomCenterOf(pos(st.pos()));
		var stands = mc.level.getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class,
			new net.minecraft.world.phys.AABB(c.add(-0.8, -0.5, -0.8), c.add(0.8, 2.5, 0.8)));
		if (stands.isEmpty()) {
			if (++wait > 40) {
				msg(mc, "Bauplan: kein Rüstungsständer bei " + pos(st.pos()).toShortString(), ChatFormatting.YELLOW);
				next();
			}
			return;
		}
		var stand = stands.stream().min(java.util.Comparator.comparingDouble(e -> e.position().distanceToSqr(c))).get();
		if (stand.position().distanceToSqr(c) > 0.3) {
			if (++wait > 40) {
				msg(mc, "Bauplan: kein Rüstungsständer genau bei " + pos(st.pos()).toShortString(), ChatFormatting.YELLOW);
				next();
			}
			return;
		}
		int item = phase / 3; // pro Gegenstand: holen, kurz warten, anwenden
		if (item >= st.grid().size()) {
			next();
			return;
		}
		String id = st.grid().get(item);
		switch (phase % 3) {
			case 0 -> {
				int slot = Hotbar.bring(mc, s -> is(s, id));
				if (slot < 0) {
					msg(mc, "Bauplan: " + id + " fehlt", ChatFormatting.YELLOW);
					phase += 3;
					return;
				}
				mc.player.getInventory().setSelectedSlot(slot);
				look(mc, stand.position().add(0, 1, 0), null);
				phase++;
				wait = 0;
			}
			case 1 -> {
				if (++wait >= 3) phase++;
			}
			default -> {
				mc.gameMode.interact(mc.player, stand, new net.minecraft.world.phys.EntityHitResult(stand, stand.position().add(0, 1, 0)), InteractionHand.MAIN_HAND);
				phase++;
				wait = 0;
			}
		}
	}

	/** An einer Leiter (Blickrichtung yaw) hochklettern, bis die Fuesse auf Hoehe pos.y sind. */
	private static void climb(Minecraft mc, Step st) {
		if (st.yaw() != null) mc.player.setYRot(st.yaw());
		mc.player.setXRot(-30);
		if (mc.player.getY() >= st.pos()[1] - 0.05 || ++wait > 300) {
			mc.options.keyUp.setDown(false);
			if (mc.player.getY() < st.pos()[1] - 0.05) msg(mc, "Bauplan: Klettern hat nicht geklappt", ChatFormatting.YELLOW);
			next();
			return;
		}
		mc.options.keyShift.setDown(false);
		mc.options.keyUp.setDown(true);
	}

	/** Ins Bett an pos legen und schlafen, bis es Tag ist. */
	private static void sleepAt(Minecraft mc, Step st) {
		BlockPos b = pos(st.pos());
		if (phase == 0) {
			look(mc, Vec3.atCenterOf(b), null);
			phase = 1;
			wait = 0;
		} else if (phase == 1) {
			if (++wait < 3) return;
			mc.options.keyShift.setDown(false);
			mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(b).add(0, 0.06, 0), Direction.UP, b, false));
			phase = 2;
			wait = 0;
		} else {
			if (mc.player.isSleeping() || (night(mc) && ++wait < 100)) return;
			if (night(mc)) msg(mc, "Bauplan: Schlafen ging nicht (Monster in der Nähe?) – arbeite weiter", ChatFormatting.YELLOW);
			next();
		}
	}

	private static int sawIndex = -1;
	private static int sawTry;
	private static int sawBefore;

	/**
	 * Steinsaege an pos: aus item (Eingang) grid[0] (Ergebnis) herstellen, bis count Stueck im Inventar dazugekommen sind.
	 * Das Rezept wird durch Ausprobieren der Knoepfe gefunden (die Liste kommt vom Server).
	 */
	private static void saw(Minecraft mc, Step st) {
		BlockPos pos = pos(st.pos());
		String out = st.grid().get(0);
		var menu = mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.StonecutterScreen s ? s.getMenu() : null;
		switch (phase) {
			case 0 -> {
				mc.options.keyShift.setDown(false);
				look(mc, Vec3.atCenterOf(pos), null);
				sawIndex = -1;
				sawLast = -1;
				sawBefore = countInv(mc, out);
				phase = 1;
				wait = 0;
			}
			case 1 -> {
				if (++wait < 3) return;
				mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
				phase = 2;
				wait = 0;
			}
			case 2 -> { // Eingang einlegen
				if (menu == null) {
					if (++wait > 40) {
						msg(mc, "Bauplan: Steinsäge " + pos.toShortString() + " ließ sich nicht öffnen", ChatFormatting.YELLOW);
						next();
					}
					return;
				}
				if (++wait < 3) return;
				if (countInv(mc, out) - sawBefore >= st.count() || menu.getSlot(0).getItem().isEmpty() && countInv(mc, st.item()) == 0) {
					if (menu.getSlot(0).getItem().isEmpty() == false) mc.gameMode.handleContainerInput(menu.containerId, 0, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, mc.player);
					phase = 6;
					wait = 0;
					return;
				}
				if (!menu.getSlot(0).getItem().isEmpty() && sawIndex >= 0 && countInv(mc, out) == sawLast) {
					msg(mc, "Bauplan: Inventar voll – Steinsäge pausiert", ChatFormatting.YELLOW);
					phase = 6;
					wait = 0;
					return;
				}
				sawLast = countInv(mc, out);
				if (menu.getSlot(0).getItem().isEmpty()) {
					int need = st.count() - (countInv(mc, out) - sawBefore);
					int perInput = Math.max(1, sawPer);
					fill(mc, menu, 0, st.item(), Math.min(64, (need + perInput - 1) / perInput));
				}
				phase = 3;
				sawTry = sawIndex >= 0 ? sawIndex : 0;
				wait = 0;
			}
			case 3 -> { // Rezept waehlen
				if (++wait < 3) return;
				mc.gameMode.handleInventoryButtonClick(menu.containerId, sawTry);
				phase = 4;
				wait = 0;
			}
			case 4 -> { // pruefen, ob das Ergebnis passt
				if (++wait < 4) return;
				ItemStack r = menu.getSlot(1).getItem();
				if (is(r, out)) {
					sawIndex = sawTry;
					sawPer = r.getCount();
					mc.gameMode.handleContainerInput(menu.containerId, 1, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, mc.player);
					phase = 2;
					wait = 0;
				} else if (++sawTry < Math.max(1, menu.getNumberOfVisibleRecipes())) {
					phase = 3;
					wait = 0;
				} else {
					msg(mc, "Bauplan: Steinsäge kennt kein Rezept für " + out, ChatFormatting.YELLOW);
					phase = 6;
					wait = 0;
				}
			}
			default -> {
				if (++wait < 3) return;
				mc.player.closeContainer();
				next();
			}
		}
	}

	private static int sawPer = 1;
	private static int sawLast = -1;

	private static int countInv(Minecraft mc, String id) {
		int n = 0;
		for (int i = 0; i < 36; i++) if (is(mc.player.getInventory().getItem(i), id)) n += mc.player.getInventory().getItem(i).getCount();
		return n;
	}
	private static int sleepPhase;
	private static int sleepWait;
	private static BlockPos bed;
	private static boolean warnedNoBed;

	private static boolean night(Minecraft mc) {
		long t = mc.level.getOverworldClockTime() % 24000;
		return (t >= 12542 && t <= 23459) || mc.level.isThundering();
	}

	/** true, solange geschlafen wird bzw. der Weg zum Bett laeuft. */
	private static boolean sleepTick(Minecraft mc) {
		if (sleepPhase == 4) {
			if (night(mc)) return false;
			sleepPhase = 0;
		}
		if (sleepPhase == 0) {
			if (!night(mc) || mc.player.isSleeping()) return false;
			bed = null;
			double best = 1e9;
			for (BlockPos p : BlockPos.betweenClosed(mc.player.blockPosition().offset(-8, -3, -8), mc.player.blockPosition().offset(8, 3, 8))) {
				if (mc.level.getBlockState(p).getBlock() instanceof net.minecraft.world.level.block.BedBlock) {
					double d = p.distSqr(mc.player.blockPosition());
					if (d < best) {
						best = d;
						bed = p.immutable();
					}
				}
			}
			if (bed == null) {
				if (!warnedNoBed) msg(mc, "Bauplan: es ist Nacht, aber kein Bett in der Nähe", ChatFormatting.YELLOW);
				warnedNoBed = true;
				return false;
			}
			warnedNoBed = false;
			sleepPhase = 1;
			sleepWait = 0;
			msg(mc, "Bauplan: Nacht – gehe schlafen", ChatFormatting.GRAY);
		}
		if (sleepPhase == 1) { // neben das Bett laufen
			Vec3 c = Vec3.atBottomCenterOf(bed);
			Vec3 d = c.subtract(mc.player.position());
			double dist = Math.sqrt(d.x * d.x + d.z * d.z);
			if (dist < 2.2 || ++sleepWait > 200) {
				release(mc);
				sleepPhase = 2;
				sleepWait = 0;
				return true;
			}
			mc.player.setYRot((float) Math.toDegrees(Math.atan2(-d.x, d.z)));
			mc.options.keyUp.setDown(true);
			mc.options.keyJump.setDown(mc.player.horizontalCollision && mc.player.onGround());
			return true;
		}
		if (sleepPhase == 2) {
			look(mc, Vec3.atCenterOf(bed), null);
			if (++sleepWait < 3) return true;
			mc.options.keyShift.setDown(false);
			mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(bed).add(0, 0.06, 0), Direction.UP, bed, false));
			sleepPhase = 3;
			sleepWait = 0;
			return true;
		}
		// sleepPhase 3: schlafen, bis es Tag ist (oder es nicht klappt, z. B. Monster in der Naehe)
		if (mc.player.isSleeping() || (night(mc) && ++sleepWait < 100)) return true;
		if (night(mc) && !mc.player.isSleeping()) {
			msg(mc, "Bauplan: Schlafen ging nicht (Monster in der Nähe oder zu weit weg) – arbeite weiter", ChatFormatting.YELLOW);
			sleepPhase = 4; // diese Nacht nicht nochmal versuchen
			return false;
		}
		sleepPhase = 0;
		return false;
	}

	/**
	 * leeren: alles aus dem Behaelter an pos ins Hauptinventar holen (Stapel fuer Stapel, bis voll).
	 * ablegen: alles aus dem Hauptinventar (ohne Schnellleiste) in den Behaelter legen.
	 * grid = Item-IDs, die liegen bleiben (z. B. Erde, Stein bzw. Kisten, Boote).
	 */
	private static void transfer(Minecraft mc, Step st) {
		BlockPos pos = pos(st.pos());
		var menu = mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> s ? s.getMenu() : null;
		if (menu == mc.player.inventoryMenu) menu = null;
		if (phase == 0) {
			mc.options.keyShift.setDown(false);
			look(mc, Vec3.atCenterOf(pos), null);
			phase = 1;
			wait = 0;
		} else if (phase == 1) {
			if (++wait < 3) return;
			mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
			phase = 2;
			wait = 0;
		} else if (phase == 2) {
			if (menu == null) {
				if (++wait > 40) {
					msg(mc, "Bauplan: " + pos.toShortString() + " ließ sich nicht öffnen", ChatFormatting.YELLOW);
					next();
				}
				return;
			}
			if (++wait < 4) return;
			int size = menu instanceof net.minecraft.world.inventory.ChestMenu c ? c.getRowCount() * 9 : 27;
			java.util.Set<String> skip = new java.util.HashSet<>(st.grid() == null ? List.of() : st.grid());
			boolean out = st.action().equals("leeren");
			// ablegen: nur Hauptinventar; mit "sortieren": true auch die Schnellleiste (Werkzeug, Waffen, Essen, Fackeln bleiben)
			boolean bar = !out && Boolean.TRUE.equals(st.sortieren());
			int from = out ? 0 : size, to = out ? size : size + (bar ? 36 : 27);
			for (int i = from; i < to; i++) {
				ItemStack s = menu.getSlot(i).getItem();
				if (s.isEmpty() || skip.contains(BuiltInRegistries.ITEM.getKey(s.getItem()).toString())) continue;
				if (i >= size + 27 && (s.isDamageableItem() || s.has(net.minecraft.core.component.DataComponents.FOOD) || s.is(net.minecraft.world.item.Items.TORCH))) continue;
				int before = s.getCount();
				mc.gameMode.handleContainerInput(menu.containerId, i, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, mc.player);
				if (menu.getSlot(i).getItem().getCount() == before && !menu.getSlot(i).getItem().isEmpty()) break; // voll
				return; // ein Stapel pro Tick
			}
			mc.player.closeContainer();
			next();
		}
	}

	/** Ofen an pos: count Stueck item hinein, Brennstoff fuel dazu, warten, Ergebnis (und Brennstoff-Rest) herausholen. */
	private static void smelt(Minecraft mc, Step st) {
		BlockPos pos = pos(st.pos());
		var menu = mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen<?> s ? s.getMenu() : null;
		int n = st.count() == null ? 1 : st.count();
		if (phase == 0) {
			mc.options.keyShift.setDown(false);
			look(mc, Vec3.atCenterOf(pos), null);
			phase = 1;
			wait = 0;
		} else if (phase == 1) {
			if (++wait < 3) return;
			mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
			phase = 2;
			wait = 0;
		} else if (phase == 2) {
			if (menu == null) {
				if (++wait > 40) {
					msg(mc, "Bauplan: Ofen " + pos.toShortString() + " ließ sich nicht öffnen", ChatFormatting.YELLOW);
					next();
				}
				return;
			}
			if (++wait < 3) return;
			fill(mc, menu, 0, st.item(), n);
			if (st.fuel() != null) fill(mc, menu, 1, st.fuel(), Math.max(1, (n + 7) / 8));
			phase = 3;
			wait = 0;
		} else if (phase == 3) {
			if (menu == null) {
				next();
				return;
			}
			++wait;
			boolean done = menu.getSlot(0).getItem().isEmpty() && wait > 40;
			if (!done && wait < n * 220 + 400) return; // ~10 s pro Stueck
			mc.gameMode.handleContainerInput(menu.containerId, 2, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, mc.player);
			mc.gameMode.handleContainerInput(menu.containerId, 1, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, mc.player);
			if (!menu.getSlot(0).getItem().isEmpty()) {
				mc.gameMode.handleContainerInput(menu.containerId, 0, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, mc.player);
				msg(mc, "Bauplan: Ofen wurde nicht ganz fertig", ChatFormatting.YELLOW);
			}
			phase = 4;
			wait = 0;
		} else {
			if (++wait < 3) return;
			mc.player.closeContainer();
			next();
		}
	}

	/** count Stueck item aus dem Inventar (Slots ab 3) per Rechtsklick in das Fenster-Feld target legen. */
	private static void fill(Minecraft mc, net.minecraft.world.inventory.AbstractContainerMenu menu, int target, String item, int count) {
		int need = count;
		while (need > 0) {
			int from = -1;
			for (int i = 3; i < menu.slots.size() && from < 0; i++) if (is(menu.getSlot(i).getItem(), item)) from = i;
			if (from < 0) {
				msg(mc, "Bauplan: für den Ofen fehlt " + item, ChatFormatting.YELLOW);
				return;
			}
			mc.gameMode.handleContainerInput(menu.containerId, from, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, mc.player);
			int k = Math.min(need, menu.getCarried().getCount());
			for (int j = 0; j < k; j++) mc.gameMode.handleContainerInput(menu.containerId, target, 1, net.minecraft.world.inventory.ContainerInput.PICKUP, mc.player);
			need -= k;
			if (!menu.getCarried().isEmpty()) mc.gameMode.handleContainerInput(menu.containerId, from, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, mc.player);
		}
	}

	/**
	 * An der Werkbank (pos) craften: grid = 9 Eintraege (Item-ID oder ""), count = wie oft.
	 * Legt je Feld count Stueck per Rechtsklick hinein und holt das Ergebnis per Shift-Klick.
	 */
	private static void craft(Minecraft mc, Step st) {
		BlockPos pos = pos(st.pos());
		var menu = mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.CraftingScreen s ? s.getMenu() : null;
		if (phase == 0) {
			mc.options.keyShift.setDown(false);
			look(mc, Vec3.atCenterOf(pos), null);
			phase = 1;
			wait = 0;
		} else if (phase == 1) {
			if (++wait < 3) return;
			mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
			phase = 2;
			wait = 0;
		} else if (phase == 2) {
			if (menu == null) {
				if (++wait > 40) {
					msg(mc, "Bauplan: Werkbank " + pos.toShortString() + " ließ sich nicht öffnen", ChatFormatting.YELLOW);
					next();
				}
				return;
			}
			if (++wait < 3) return;
			int n = st.count() == null ? 1 : st.count();
			boolean ok = true;
			for (int g = 0; g < 9 && ok; g++) {
				String id = st.grid().get(g);
				if (id == null || id.isEmpty()) continue;
				int need = n;
				while (need > 0) {
					int from = -1;
					for (int i = 10; i < 46 && from < 0; i++) if (is(menu.getSlot(i).getItem(), id)) from = i;
					if (from < 0) {
						msg(mc, "Bauplan: zum Craften fehlt " + id, ChatFormatting.YELLOW);
						ok = false;
						break;
					}
					mc.gameMode.handleContainerInput(menu.containerId, from, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, mc.player);
					int k = Math.min(need, menu.getCarried().getCount());
					for (int j = 0; j < k; j++) mc.gameMode.handleContainerInput(menu.containerId, g + 1, 1, net.minecraft.world.inventory.ContainerInput.PICKUP, mc.player);
					need -= k;
					if (!menu.getCarried().isEmpty()) mc.gameMode.handleContainerInput(menu.containerId, from, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, mc.player);
				}
			}
			phase = ok ? 3 : 4;
			wait = 0;
		} else if (phase == 3) {
			// das Ergebnis berechnet der Server und schickt es kurz danach
			if (menu == null) {
				next();
				return;
			}
			if (menu.getSlot(0).getItem().isEmpty()) {
				if (++wait > 20) {
					msg(mc, "Bauplan: Rezept ergibt nichts (" + st.item() + ")", ChatFormatting.YELLOW);
					phase = 4;
					wait = 0;
				}
				return;
			}
			mc.gameMode.handleContainerInput(menu.containerId, 0, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, mc.player);
			phase = 4;
			wait = 0;
		} else {
			if (++wait < 3) return;
			mc.player.closeContainer(); // Reste im Raster wandern beim Schliessen zurueck ins Inventar
			next();
		}
	}

	/** Behaelter an pos oeffnen und bis zu count Stueck von item ins Inventar holen. */
	private static void fetch(Minecraft mc, Step st) {
		BlockPos pos = pos(st.pos());
		var menu = mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> s ? s.getMenu() : null;
		if (phase == 0) {
			mc.options.keyShift.setDown(false);
			look(mc, Vec3.atCenterOf(pos), null);
			taken = 0;
			phase = 1;
			wait = 0;
		} else if (phase == 1) {
			if (++wait < 3) return;
			mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
			phase = 2;
			wait = 0;
		} else if (phase == 2) {
			if (menu == null || menu == mc.player.inventoryMenu) {
				if (++wait > 40) {
					msg(mc, "Bauplan: " + pos.toShortString() + " ließ sich nicht öffnen", ChatFormatting.YELLOW);
					next();
				}
				return;
			}
			if (++wait < 4) return;
			int size = menu instanceof net.minecraft.world.inventory.ChestMenu c ? c.getRowCount() * 9 : 27;
			int want = st.count() == null ? 64 : st.count();
			for (int i = 0; i < size && taken < want; i++) {
				ItemStack s = menu.getSlot(i).getItem();
				if (!is(s, st.item())) continue;
				int before = s.getCount();
				mc.gameMode.handleContainerInput(menu.containerId, i, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, mc.player);
				int n = before - menu.getSlot(i).getItem().getCount();
				if (n <= 0) break;
				taken += n;
				return; // ein Stapel pro Tick
			}
			mc.player.closeContainer();
			next();
		}
	}

	/** Zur Blockmitte von pos laufen (gerade Linie, springt bei Hindernissen, schleicht kurz vor dem Ziel). */
	private static void walk(Minecraft mc, Step st) {
		Vec3 target = Vec3.atBottomCenterOf(pos(st.pos()));
		Vec3 d = target.subtract(mc.player.position());
		double dist = Math.sqrt(d.x * d.x + d.z * d.z);
		boolean above = mc.player.getY() > target.y + 0.5;
		// ueber einer Leiter/Luke angekommen: loslassen und hinunterrutschen lassen
		if (dist < 0.3 && above && wait < 400) {
			release(mc);
			++wait;
			return;
		}
		if (dist < 0.2 || ++wait > 400) {
			release(mc);
			if (dist >= 0.2) msg(mc, "Bauplan: " + pos(st.pos()).toShortString() + " nicht erreicht", ChatFormatting.YELLOW);
			next();
			return;
		}
		mc.player.setYRot((float) Math.toDegrees(Math.atan2(-d.x, d.z)));
		mc.options.keyUp.setDown(true);
		mc.options.keyShift.setDown(dist < 1.2 && !above); // nie schleichen, wenn es abwaerts geht (Leiter)
		mc.options.keyJump.setDown(mc.player.horizontalCollision && mc.player.onGround());
	}

	private static void release(Minecraft mc) {
		mc.options.keyUp.setDown(false);
		mc.options.keyJump.setDown(false);
		mc.options.keyShift.setDown(false);
	}

	/** Trefferpunkt: Mitte der angeklickten Flaeche, oder hit = Punkt innerhalb des Blocks (0..1), z. B. oben fuer obere Stufen. */
	private static BlockHitResult hit(Step st) {
		BlockPos click = pos(st.click());
		Direction face = Direction.byName(st.face());
		Vec3 at = st.hit() != null
			? new Vec3(click.getX() + st.hit()[0], click.getY() + st.hit()[1], click.getZ() + st.hit()[2])
			: Vec3.atCenterOf(click).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
		return new BlockHitResult(at, face, click, false);
	}

	/** Zum Punkt schauen; mit yaw fest in diese Richtung (die Kistenfront zeigt dann zum Spieler zurueck). */
	private static void look(Minecraft mc, Vec3 target, Float yaw) {
		Vec3 d = target.subtract(mc.player.getEyePosition());
		float y = yaw != null ? yaw : (float) Math.toDegrees(Math.atan2(-d.x, d.z));
		float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
		mc.player.setYRot(y);
		mc.player.setXRot(pitch);
	}

	private static boolean is(ItemStack s, String id) {
		return !s.isEmpty() && BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(id);
	}

	private static BlockPos pos(int[] p) {
		return new BlockPos(p[0], p[1], p[2]);
	}

	private static void next() {
		index++;
		phase = 0;
		wait = 0;
	}

	private static void finish(Minecraft mc, String text, ChatFormatting color) {
		plan = null;
		release(mc);
		if (mc.gameMode != null) mc.gameMode.stopDestroyBlock();
		msg(mc, text, color);
	}

	private static void msg(Minecraft mc, String text, ChatFormatting color) {
		LocalApi.log(text);
		if (mc.player != null) mc.player.sendSystemMessage(Component.literal(text).withStyle(color));
	}

	private Blueprint() {}
}
