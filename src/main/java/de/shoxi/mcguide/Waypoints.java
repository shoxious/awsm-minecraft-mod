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
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Wegpunkte pro Server (config/mcguide-waypoints.json), Todespunkt automatisch, Pfeil im HUD. */
final class Waypoints {
	static final class Wp {
		String name;
		int x, y, z;
		String dim;

		Wp(String name, int x, int y, int z, String dim) {
			this.name = name;
			this.x = x;
			this.y = y;
			this.z = z;
			this.dim = dim;
		}
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type TYPE = new TypeToken<Map<String, List<Wp>>>() {}.getType();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("mcguide-waypoints.json");
	private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

	private static Map<String, List<Wp>> all;
	private static String target;
	private static boolean wasDead;

	private static Map<String, List<Wp>> data() {
		if (all == null) {
			all = new HashMap<>();
			if (Files.exists(FILE)) {
				try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
					Map<String, List<Wp>> m = GSON.fromJson(r, TYPE);
					if (m != null) all = m;
				} catch (IOException | RuntimeException e) {
					GuideServer.LOG.warn("mcguide-waypoints.json konnte nicht gelesen werden", e);
				}
			}
		}
		return all;
	}

	private static void save() {
		try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
			GSON.toJson(data(), TYPE, w);
		} catch (IOException e) {
			GuideServer.LOG.warn("mcguide-waypoints.json konnte nicht gespeichert werden", e);
		}
	}

	private static String serverKey(Minecraft mc) {
		ServerData sd = mc.getCurrentServer();
		if (sd != null) return sd.ip;
		if (mc.getSingleplayerServer() != null) return "einzelspieler:" + mc.getSingleplayerServer().getWorldData().getLevelName();
		return "unbekannt";
	}

	static List<Wp> list(Minecraft mc) {
		return data().computeIfAbsent(serverKey(mc), k -> new ArrayList<>());
	}

	private static String dim(Minecraft mc) {
		return mc.level.dimension().identifier().getPath();
	}

	private static Wp find(Minecraft mc, String name) {
		for (Wp w : list(mc)) if (w.name.equalsIgnoreCase(name)) return w;
		return null;
	}

	static void set(Minecraft mc, String name) {
		LocalPlayer p = mc.player;
		if (p == null) return;
		if (name == null) {
			int n = 1;
			while (find(mc, "Punkt " + n) != null) n++;
			name = "Punkt " + n;
		}
		Wp old = find(mc, name);
		if (old != null) list(mc).remove(old);
		list(mc).add(new Wp(name, p.getBlockX(), p.getBlockY(), p.getBlockZ(), dim(mc)));
		save();
		target = name;
		p.sendOverlayMessage(Component.literal("Wegpunkt „" + name + "“ gesetzt").withStyle(ChatFormatting.GREEN));
	}

	/**
	 * Automatischer Wegpunkt ("Bastion 1", ...), ausser es gibt schon einen mit diesem Praefix
	 * im Umkreis minDist. Wird zum Ziel, wenn noch keins gesetzt ist. Gibt den Namen oder null zurueck.
	 */
	static String addAuto(Minecraft mc, String prefix, int x, int y, int z, int minDist) {
		String d = dim(mc);
		for (Wp w : list(mc)) {
			if (w.name.startsWith(prefix) && w.dim.equals(d) && (long) (w.x - x) * (w.x - x) + (long) (w.z - z) * (w.z - z) < (long) minDist * minDist) return null;
		}
		int n = 1;
		while (find(mc, prefix + " " + n) != null) n++;
		String name = prefix + " " + n;
		list(mc).add(new Wp(name, x, y, z, d));
		save();
		if (target == null) target = name;
		return name;
	}

	static void target(Minecraft mc, String name) {
		Wp w = find(mc, name);
		if (w == null) {
			msg(mc, "Kein Wegpunkt „" + name + "“ (/wp zeigt alle)");
			return;
		}
		target = w.name;
		msg(mc, "Ziel: " + w.name);
	}

	static void remove(Minecraft mc, String name) {
		Wp w = find(mc, name);
		if (w == null) {
			msg(mc, "Kein Wegpunkt „" + name + "“");
			return;
		}
		list(mc).remove(w);
		save();
		if (w.name.equals(target)) target = null;
		msg(mc, "Wegpunkt „" + w.name + "“ gelöscht");
	}

	static void clearTarget(Minecraft mc) {
		target = null;
		msg(mc, "Kein Ziel mehr");
	}

	static void print(Minecraft mc) {
		List<Wp> l = list(mc);
		if (l.isEmpty()) {
			msg(mc, "Keine Wegpunkte. /wp setzen <Name> oder Taste N");
			return;
		}
		for (Wp w : l) {
			mc.player.sendSystemMessage(Component.literal("• " + w.name + ": " + w.x + " " + w.y + " " + w.z + " (" + w.dim + ")")
				.withStyle(w.name.equals(target) ? ChatFormatting.GREEN : ChatFormatting.GRAY));
		}
	}

	private static void msg(Minecraft mc, String s) {
		if (mc.player != null) mc.player.sendSystemMessage(Component.literal(s));
	}

	/** Todespunkt merken und Linie zum Ziel zeichnen. */
	static void tick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null || mc.level == null) {
			wasDead = false;
			return;
		}
		boolean dead = p.isDeadOrDying();
		if (dead && !wasDead) {
			Wp old = find(mc, "Tod");
			if (old != null) list(mc).remove(old);
			list(mc).add(new Wp("Tod", p.getBlockX(), p.getBlockY(), p.getBlockZ(), dim(mc)));
			save();
			target = "Tod";
		}
		wasDead = dead;
		Vec3 t = targetPos(mc);
		if (t != null && t.distanceToSqr(p.position()) < 192 * 192) {
			Gizmos.line(t, t.add(0, 64, 0), 0xFF40E0FF, 3f).setAlwaysOnTop();
		}
	}

	/** Zielposition in der aktuellen Dimension (Overworld/Nether umgerechnet), sonst null. */
	private static Vec3 targetPos(Minecraft mc) {
		if (target == null) return null;
		Wp w = find(mc, target);
		if (w == null) return null;
		String here = dim(mc);
		double f;
		if (w.dim.equals(here)) f = 1;
		else if (w.dim.equals("overworld") && here.equals("the_nether")) f = 1 / 8.0;
		else if (w.dim.equals("the_nether") && here.equals("overworld")) f = 8;
		else return null;
		return new Vec3(w.x * f + 0.5, w.y, w.z * f + 0.5);
	}

	/** HUD-Zeile: "➤ Tod 123 m ↗ (Y +5)" oder null. */
	static String hudLine(Minecraft mc) {
		if (target == null || mc.player == null) return null;
		Vec3 t = targetPos(mc);
		if (t == null) return "➤ " + target + " (andere Dimension)";
		LocalPlayer p = mc.player;
		double dx = t.x - p.getX(), dz = t.z - p.getZ();
		int dist = (int) Math.sqrt(dx * dx + dz * dz);
		float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
		float rel = Mth.wrapDegrees(yaw - p.getYRot());
		String arrow = ARROWS[Math.floorMod(Math.round(rel / 45f), 8)];
		int dy = (int) t.y - p.getBlockY();
		String sameDim = find(mc, target).dim.equals(dim(mc)) ? "" : " (umgerechnet)";
		return "➤ " + target + " " + dist + " m " + arrow + " (Y " + (dy >= 0 ? "+" : "") + dy + ")" + sameDim;
	}

	private Waypoints() {}
}
