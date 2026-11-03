package de.shoxi.mcguide;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * Merkt sich den Inhalt jeder Kiste, jedes Fasses und jeder Shulkerkiste, die der Spieler oeffnet
 * (auch durch Sortieren/Baupläne), pro Server in config/mcguide-kisten.json. Selbst aufgestellte
 * Behaelter werden sofort eingetragen ("noch nicht geöffnet"), abgebaute fallen wieder heraus.
 * Inhalte von Shulkerkisten in Kisten werden mitgezaehlt. Suche: Taste K bzw. /kisten [suchwort].
 */
final class ChestTracker {
	static final class Box {
		String dim;
		int x, y, z;
		String kind;
		String label;
		long seen;
		boolean opened;
		Map<String, Integer> items = new TreeMap<>();
	}

	/** Ein Fundort fuer die Suche. */
	record Hit(Box box, int count) {}

	/** Treffer fuer ein Item: Gesamtzahl und Fundorte, groesster zuerst. */
	record Result(String id, ItemStack icon, int total, List<Hit> where) {}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type TYPE = new TypeToken<Map<String, Map<String, Box>>>() {}.getType();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("mcguide-kisten.json");
	static final String ENDER = "ender";

	private static Map<String, Map<String, Box>> all;
	private static BlockPos lastUse;
	private static long lastUseTick;
	private static BlockPos pendingPlace;
	private static Block pendingBlock;
	private static int pendingTicks;
	private static int openMenu = -1;
	private static int openTicks;
	private static String openKey;
	private static long ticks;
	private static boolean dirty;

	static void register() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (!level.isClientSide()) return InteractionResult.PASS;
			BlockPos pos = hit.getBlockPos();
			if (isContainer(level.getBlockState(pos)) && !player.isSecondaryUseActive()) {
				lastUse = pos.immutable();
				lastUseTick = ticks;
			}
			ItemStack held = player.getItemInHand(hand);
			if (held.getItem() instanceof BlockItem bi && isContainer(bi.getBlock().defaultBlockState())) {
				pendingPlace = (level.getBlockState(pos).canBeReplaced() ? pos : pos.relative(hit.getDirection())).immutable();
				pendingBlock = bi.getBlock();
				pendingTicks = 0;
			}
			return InteractionResult.PASS;
		});
	}

	static boolean isContainer(BlockState st) {
		Block b = st.getBlock();
		return b instanceof ChestBlock || b instanceof BarrelBlock || b instanceof ShulkerBoxBlock || b instanceof EnderChestBlock;
	}

	// ---------------------------------------------------------------- Daten

	private static Map<String, Map<String, Box>> data() {
		if (all == null) {
			all = new HashMap<>();
			if (Files.exists(FILE)) {
				try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
					Map<String, Map<String, Box>> m = GSON.fromJson(r, TYPE);
					if (m != null) all = m;
				} catch (IOException | RuntimeException e) {
					GuideServer.LOG.warn("mcguide-kisten.json konnte nicht gelesen werden", e);
				}
			}
		}
		return all;
	}

	private static void save() {
		dirty = false;
		try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
			GSON.toJson(data(), TYPE, w);
		} catch (IOException e) {
			GuideServer.LOG.warn("mcguide-kisten.json konnte nicht gespeichert werden", e);
		}
	}

	private static String serverKey(Minecraft mc) {
		ServerData sd = mc.getCurrentServer();
		if (sd != null) return sd.ip;
		if (mc.getSingleplayerServer() != null) return "einzelspieler:" + mc.getSingleplayerServer().getWorldData().getLevelName();
		return "unbekannt";
	}

	static Map<String, Box> boxes(Minecraft mc) {
		return data().computeIfAbsent(serverKey(mc), k -> new LinkedHashMap<>());
	}

	private static String dim(Minecraft mc) {
		return mc.level.dimension().identifier().getPath();
	}

	private static String key(String dim, BlockPos p) {
		return dim + " " + p.getX() + " " + p.getY() + " " + p.getZ();
	}

	/** Doppelkisten zaehlen als eine Kiste: Schluessel ist immer die kleinere Haelfte. */
	private static BlockPos main(Minecraft mc, BlockPos pos) {
		BlockState st = mc.level.getBlockState(pos);
		if (st.getBlock() instanceof ChestBlock && st.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
			BlockPos other = ChestBlock.getConnectedBlockPos(pos, st);
			if (other.compareTo(pos) < 0) return other;
		}
		return pos;
	}

	private static List<BlockPos> parts(Minecraft mc, BlockPos pos) {
		List<BlockPos> out = new ArrayList<>();
		out.add(pos);
		BlockState st = mc.level.getBlockState(pos);
		if (st.getBlock() instanceof ChestBlock && st.getValue(ChestBlock.TYPE) != ChestType.SINGLE) out.add(ChestBlock.getConnectedBlockPos(pos, st));
		return out;
	}

	private static String kind(BlockState st, boolean dbl) {
		Block b = st.getBlock();
		if (b instanceof EnderChestBlock) return "Enderkiste";
		if (b instanceof ShulkerBoxBlock) return "Shulkerkiste";
		if (b instanceof BarrelBlock) return "Fass";
		return dbl ? "Doppelkiste" : "Kiste";
	}

	/** Eintrag holen; neu angelegt wird er nur, wenn create (selbst aufgestellt/übernommen) oder Enderkiste. */
	private static Box entry(Minecraft mc, BlockPos pos, boolean create) {
		BlockState st = mc.level.getBlockState(pos);
		boolean ender = st.getBlock() instanceof EnderChestBlock;
		BlockPos m = main(mc, pos);
		String k = ender ? ENDER : key(dim(mc), m);
		Box b = boxes(mc).get(k);
		if (b == null && !ender && !create) {
			// Doppelkiste, deren andere Haelfte schon eingetragen ist
			for (BlockPos p : parts(mc, pos)) {
				Box o = boxes(mc).remove(key(dim(mc), p));
				if (o != null) {
					o.x = m.getX();
					o.y = m.getY();
					o.z = m.getZ();
					boxes(mc).put(k, o);
					b = o;
				}
			}
			if (b == null) return null;
		}
		if (b == null) {
			b = new Box();
			b.dim = ender ? "" : dim(mc);
			b.x = m.getX();
			b.y = m.getY();
			b.z = m.getZ();
			boxes(mc).put(k, b);
		}
		b.kind = kind(st, parts(mc, pos).size() > 1);
		String label = ender ? null : ItemCategories.label(mc.level, parts(mc, m));
		if (label != null) b.label = label;
		return b;
	}

	// ---------------------------------------------------------------- Tick

	static void tick(Minecraft mc) {
		ticks++;
		if (mc.player == null || mc.level == null) {
			openMenu = -1;
			return;
		}
		placed(mc);
		AbstractContainerMenu menu = switch (mc.gui.screen()) {
			case ContainerScreen s when s.getMenu() instanceof ChestMenu m -> m;
			case ShulkerBoxScreen s -> s.getMenu();
			case null, default -> null;
		};
		if (menu == null) {
			openMenu = -1;
			if (dirty && ticks % 40 == 0) save();
			if (ticks % 100 == 0) prune(mc);
			return;
		}
		if (menu.containerId != openMenu) {
			openMenu = menu.containerId;
			openTicks = 0;
			openKey = null;
			BlockPos pos = lastUse != null && ticks - lastUseTick < 100 ? lastUse : null;
			if (pos == null && mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bh && isContainer(mc.level.getBlockState(bh.getBlockPos()))) pos = bh.getBlockPos();
			lastUse = null;
			if (pos != null && isContainer(mc.level.getBlockState(pos))) {
				Box b = entry(mc, pos, false);
				if (b != null) openKey = b.dim.isEmpty() ? ENDER : key(b.dim, new BlockPos(b.x, b.y, b.z));
			}
		}
		// Inhalt kommt vom Server ein paar Ticks nach dem Oeffnen; danach laufend nachfuehren
		if (openKey == null || ++openTicks < 3 || openTicks % 5 != 0) return;
		Box b = boxes(mc).get(openKey);
		if (b == null) return;
		int size = menu instanceof ChestMenu cm ? cm.getRowCount() * 9 : 27;
		Map<String, Integer> items = new TreeMap<>();
		for (int i = 0; i < size && i < menu.slots.size(); i++) count(menu.getSlot(i).getItem(), items);
		if (!items.equals(b.items) || !b.opened) {
			b.items = items;
			b.opened = true;
			dirty = true;
		}
		b.seen = System.currentTimeMillis();
	}

	/** Item zaehlen, Shulkerkisten samt Inhalt. */
	private static void count(ItemStack st, Map<String, Integer> into) {
		if (st.isEmpty()) return;
		into.merge(BuiltInRegistries.ITEM.getKey(st.getItem()).toString(), st.getCount(), Integer::sum);
		ItemContainerContents inner = st.get(DataComponents.CONTAINER);
		if (inner != null) inner.nonEmptyItemCopyStream().forEach(s -> count(s, into));
	}

	/** Gerade selbst aufgestellt? Dann gleich eintragen (Inhalt noch unbekannt bzw. leer). */
	private static void placed(Minecraft mc) {
		if (pendingPlace == null) return;
		if (mc.level.getBlockState(pendingPlace).is(pendingBlock)) {
			Box b = entry(mc, pendingPlace, true);
			if (!b.opened && b.items.isEmpty()) b.seen = System.currentTimeMillis();
			// neu angesetzte Haelfte einer Doppelkiste: alter Eintrag der anderen Haelfte geht im neuen auf
			for (BlockPos p : parts(mc, pendingPlace)) {
				String k = key(dim(mc), p);
				Box old = boxes(mc).get(k);
				if (old != null && old != b) {
					old.items.forEach((id, n) -> b.items.merge(id, n, Integer::sum));
					b.opened |= old.opened;
					boxes(mc).remove(k);
				}
			}
			dirty = true;
			pendingPlace = null;
		} else if (++pendingTicks > 20) {
			pendingPlace = null;
		}
	}

	/** Abgebaut? Eintraege in geladenen Chunks entfernen, an deren Stelle kein Behaelter mehr steht. */
	private static void prune(Minecraft mc) {
		String d = dim(mc);
		Iterator<Map.Entry<String, Box>> it = boxes(mc).entrySet().iterator();
		while (it.hasNext()) {
			Box b = it.next().getValue();
			if (b.dim.isEmpty() || !b.dim.equals(d)) continue;
			BlockPos p = new BlockPos(b.x, b.y, b.z);
			if (mc.player.blockPosition().distSqr(p) > 96 * 96 || !mc.level.isLoaded(p)) continue;
			if (!isContainer(mc.level.getBlockState(p))) {
				it.remove();
				dirty = true;
			}
		}
	}

	static void onBreak(Minecraft mc, BlockPos pos) {
		if (mc.level == null) return;
		if (boxes(mc).remove(key(dim(mc), pos)) != null) dirty = true;
		prune(mc);
	}

	/** Alle Behaelter im Umkreis als eigene übernehmen (für Kisten, die schon vor dem Mod standen). */
	static int claim(Minecraft mc, int radius) {
		if (mc.level == null || mc.player == null) return 0;
		int n = 0;
		BlockPos c = mc.player.blockPosition();
		for (BlockPos p : BlockPos.betweenClosed(c.offset(-radius, -radius / 2, -radius), c.offset(radius, radius / 2, radius))) {
			BlockState st = mc.level.getBlockState(p);
			if (!isContainer(st) || st.getBlock() instanceof EnderChestBlock) continue;
			BlockPos m = main(mc, p);
			if (!m.equals(p)) continue;
			if (!boxes(mc).containsKey(key(dim(mc), m))) n++;
			entry(mc, m, true);
		}
		save();
		return n;
	}

	static void forget(Minecraft mc) {
		boxes(mc).clear();
		save();
	}

	// ---------------------------------------------------------------- Suche

	static String name(String id) {
		var item = BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(id));
		return new ItemStack(item).getHoverName().getString();
	}

	static List<Result> search(Minecraft mc, String query) {
		String q = query.trim().toLowerCase(java.util.Locale.ROOT);
		Map<String, List<Hit>> by = new HashMap<>();
		for (Box b : boxes(mc).values()) {
			for (Map.Entry<String, Integer> e : b.items.entrySet()) by.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(new Hit(b, e.getValue()));
		}
		List<Result> out = new ArrayList<>();
		for (Map.Entry<String, List<Hit>> e : by.entrySet()) {
			String id = e.getKey();
			String n = name(id);
			if (!q.isEmpty() && !n.toLowerCase(java.util.Locale.ROOT).contains(q) && !id.contains(q.replace(' ', '_'))) continue;
			List<Hit> hits = e.getValue();
			hits.sort((a, c) -> Integer.compare(c.count(), a.count()));
			int total = hits.stream().mapToInt(Hit::count).sum();
			var item = BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(id));
			out.add(new Result(id, new ItemStack(item), total, hits));
		}
		out.sort((a, c) -> q.isEmpty() ? Integer.compare(c.total(), a.total()) : name(a.id()).compareToIgnoreCase(name(c.id())));
		return out;
	}

	static String where(Box b) {
		String pos = b.dim.isEmpty() ? "Enderkiste (überall)" : b.x + " " + b.y + " " + b.z + (b.dim.equals("overworld") ? "" : " (" + b.dim + ")");
		return (b.label != null ? "„" + b.label + "“ · " : "") + b.kind + " · " + pos;
	}

	static int count(Minecraft mc) {
		return boxes(mc).size();
	}

	static int unopened(Minecraft mc) {
		return (int) boxes(mc).values().stream().filter(b -> !b.opened).count();
	}

	private ChestTracker() {}
}
