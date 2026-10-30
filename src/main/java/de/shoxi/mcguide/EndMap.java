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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/**
 * Gedaechtnis fuer die End-Karte (pro Server): welche Chunks schon geladen waren, wo Land ist,
 * und die gefundenen Endstaedte/Endschiffe samt Elytra-Zustand. Liegt in config/mcguide-endmap.json.
 */
final class EndMap {
	static final int UNKNOWN = 0, ELYTRA = 1, LOOTED = 2;

	static final class Marker {
		String type; // "city" oder "ship"
		String name; // zugehoeriger Wegpunkt
		int x, y, z;
		int state;

		Marker(String type, String name, int x, int y, int z, int state) {
			this.type = type;
			this.name = name;
			this.x = x;
			this.y = y;
			this.z = z;
			this.state = state;
		}
	}

	static final class Data {
		Set<Long> explored = new HashSet<>();
		Set<Long> land = new HashSet<>();
		List<Marker> markers = new ArrayList<>();
	}

	private static final Gson GSON = new Gson();
	private static final Type TYPE = new TypeToken<Map<String, Data>>() {}.getType();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("mcguide-endmap.json");

	private static Map<String, Data> all;
	private static boolean dirty;
	private static int ticks;

	private static Map<String, Data> all() {
		if (all == null) {
			all = new HashMap<>();
			if (Files.exists(FILE)) {
				try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
					Map<String, Data> m = GSON.fromJson(r, TYPE);
					if (m != null) all = m;
				} catch (IOException | RuntimeException e) {
					GuideServer.LOG.warn("mcguide-endmap.json konnte nicht gelesen werden", e);
				}
			}
		}
		return all;
	}

	static Data data(Minecraft mc) {
		return all().computeIfAbsent(Waypoints.serverKey(mc), k -> new Data());
	}

	static void save() {
		if (!dirty) return;
		dirty = false;
		try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
			GSON.toJson(all(), TYPE, w);
		} catch (IOException e) {
			GuideServer.LOG.warn("mcguide-endmap.json konnte nicht gespeichert werden", e);
		}
	}

	/** Alle 30 Sekunden speichern, falls sich etwas getan hat. */
	static void tick() {
		if (++ticks % 600 == 0) save();
	}

	static void chunk(Minecraft mc, int cx, int cz, boolean hasLand) {
		long key = (long) cx << 32 | (cz & 0xFFFFFFFFL);
		Data d = data(mc);
		if (d.explored.add(key)) dirty = true;
		if (hasLand && d.land.add(key)) dirty = true;
	}

	static Marker near(Minecraft mc, String type, int x, int z, int dist) {
		for (Marker m : data(mc).markers) {
			if (m.type.equals(type) && (long) (m.x - x) * (m.x - x) + (long) (m.z - z) * (m.z - z) < (long) dist * dist) return m;
		}
		return null;
	}

	static void mark(Minecraft mc, String type, String name, int x, int y, int z, int state) {
		data(mc).markers.add(new Marker(type, name, x, y, z, state));
		dirty = true;
	}

	static void setState(Marker m, int state) {
		if (m.state == state) return;
		m.state = state;
		dirty = true;
	}

	private EndMap() {}
}
