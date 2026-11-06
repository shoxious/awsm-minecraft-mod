package de.shoxi.mcguide;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * Lokale Schnittstelle (nur 127.0.0.1, mit Token aus config/mcguide-api.json), damit ein Werkzeug auf
 * demselben Rechner – z. B. Claude Code – sehen kann, was im Spiel los ist, und Bauplaene ausfuehren laesst.
 * Alles laeuft als normale Aktionen der eigenen Spielfigur.
 *
 * GET  /status     Position, Blick, Inventar, laufende Aufgaben, letzte Meldungen
 * GET  /ansehen    Umgebung wie /lager ansehen (macht auch einen Screenshot)
 * GET  /inhalt     zuletzt notierter Kisteninhalt (Bauplan-Schritt "inhalt")
 * POST /plan       Bauplan als JSON ausfuehren (wie config/mcguide-bauplan.json)
 * POST /zuordnung  feste Item-Zuordnung {"minecraft:stone": "Stein", ...} speichern
 * POST /screenshot Screenshot machen
 * POST /stop       alles abbrechen
 */
final class LocalApi {
	private static final int PORT = 47115;
	private static final Path CONFIG = FabricLoader.getInstance().getConfigDir();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final ArrayDeque<String> LOG = new ArrayDeque<>();
	private static String token;

	static void start() {
		try {
			token = HexFormat.of().formatHex(SecureRandom.getInstanceStrong().generateSeed(16));
			HttpServer server;
			try {
				server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), PORT), 0);
			} catch (IOException busy) {
				server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
			}
			server.setExecutor(Executors.newSingleThreadExecutor(r -> {
				Thread t = new Thread(r, "mcguide-api");
				t.setDaemon(true);
				return t;
			}));
			server.createContext("/", LocalApi::handle);
			server.start();
			Files.writeString(CONFIG.resolve("mcguide-api.json"), GSON.toJson(Map.of(
				"url", "http://127.0.0.1:" + server.getAddress().getPort(), "token", token)), StandardCharsets.UTF_8);
			GuideServer.LOG.info("mcguide-Schnittstelle auf 127.0.0.1:{}", server.getAddress().getPort());
		} catch (Exception e) {
			GuideServer.LOG.warn("mcguide-Schnittstelle nicht gestartet", e);
		}
	}

	static synchronized void log(String line) {
		LOG.addLast(java.time.LocalTime.now().withNano(0) + " " + line);
		while (LOG.size() > 50) LOG.removeFirst();
	}

	private static synchronized List<String> logLines() {
		return new ArrayList<>(LOG);
	}

	private static void handle(HttpExchange ex) throws IOException {
		try {
			if (!token.equals(ex.getRequestHeaders().getFirst("X-Token"))) {
				send(ex, 401, "Token fehlt oder falsch");
				return;
			}
			String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			Minecraft mc = Minecraft.getInstance();
			String path = ex.getRequestURI().getPath();
			switch (ex.getRequestMethod() + " " + path) {
				case "GET /status" -> send(ex, 200, GSON.toJson(onClient(mc, () -> status(mc))));
				case "GET /ansehen" -> {
					onClient(mc, () -> {
						StorageView.dump(mc);
						return null;
					});
					send(ex, 200, Files.readString(CONFIG.resolve("mcguide-lager.txt")));
				}
				case "GET /bereich" -> send(ex, 200, onClient(mc, () -> area(mc, ex.getRequestURI().getQuery())));
				case "GET /wesen" -> send(ex, 200, onClient(mc, () -> {
					StringBuilder out = new StringBuilder();
					if (mc.player == null) return "nicht im Spiel";
					for (var e : mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(80))) {
						String type = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
						boolean living = e instanceof net.minecraft.world.entity.LivingEntity && !(e instanceof net.minecraft.world.entity.player.Player);
						if (!(living || type.contains("painting") || type.contains("frame") || type.equals("item") || type.contains("minecart") || type.contains("boat"))) continue;
						out.append(type).append(' ').append(e.getBlockX()).append(' ').append(e.getBlockY()).append(' ').append(e.getBlockZ());
						if (e instanceof net.minecraft.world.entity.decoration.ArmorStand a) {
							for (var slot : net.minecraft.world.entity.EquipmentSlot.values()) {
								if (!a.getItemBySlot(slot).isEmpty()) out.append(' ').append(slot.getName()).append('=').append(BuiltInRegistries.ITEM.getKey(a.getItemBySlot(slot).getItem()).getPath());
							}
						}
						if (e instanceof net.minecraft.world.entity.item.ItemEntity it) out.append(' ').append(BuiltInRegistries.ITEM.getKey(it.getItem().getItem()).getPath()).append(" x").append(it.getItem().getCount());
						out.append('\n');
					}
					return out.toString();
				}));
				case "GET /inhalt" -> {
					Path f = CONFIG.resolve("mcguide-inhalt.txt");
					send(ex, Files.exists(f) ? 200 : 404, Files.exists(f) ? Files.readString(f) : "noch nichts notiert");
				}
				case "POST /plan" -> {
					Blueprint.Plan plan = GSON.fromJson(body, Blueprint.Plan.class);
					boolean ok = onClient(mc, () -> Blueprint.start(mc, plan));
					send(ex, ok ? 200 : 409, ok ? "gestartet" : "nicht gestartet: " + String.join(" | ", logLines().subList(Math.max(0, logLines().size() - 3), logLines().size())));
				}
				case "POST /zuordnung" -> {
					JsonParser.parseString(body).getAsJsonObject(); // nur gueltiges JSON speichern
					Files.writeString(CONFIG.resolve("mcguide-zuordnung.json"), body, StandardCharsets.UTF_8);
					send(ex, 200, "gespeichert");
				}
				case "POST /screenshot" -> {
					onClient(mc, () -> {
						Screenshot.grab(mc, false);
						return null;
					});
					send(ex, 200, mc.gameDirectory.toPath().resolve("screenshots").toString());
				}
				case "POST /weiter" -> {
					onClient(mc, () -> {
						mc.options.pauseOnLostFocus = false;
						if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.PauseScreen) mc.gui.setScreen(null);
						if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.DeathScreen && mc.player != null) {
							mc.player.respawn();
							mc.gui.setScreen(null);
						}
						return null;
					});
					send(ex, 200, "ok");
				}
				case "POST /stop" -> {
					onClient(mc, () -> {
						Blueprint.abort(mc);
						ChestSorter.abort(mc);
						return null;
					});
					send(ex, 200, "gestoppt");
				}
				default -> send(ex, 404, "unbekannt: " + ex.getRequestMethod() + " " + path);
			}
		} catch (Exception e) {
			send(ex, 500, e.toString());
		}
	}

	private static Map<String, Object> status(Minecraft mc) {
		Map<String, Object> m = new LinkedHashMap<>();
		if (mc.player == null || mc.level == null) {
			m.put("imSpiel", false);
			return m;
		}
		m.put("imSpiel", true);
		m.put("server", mc.getCurrentServer() == null ? "Einzelspieler" : mc.getCurrentServer().ip);
		m.put("pos", List.of(Math.round(mc.player.getX() * 100) / 100.0, Math.round(mc.player.getY() * 100) / 100.0, Math.round(mc.player.getZ() * 100) / 100.0));
		m.put("block", List.of(mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ()));
		m.put("yaw", Math.round(mc.player.getYRot()));
		m.put("pitch", Math.round(mc.player.getXRot()));
		m.put("blick", mc.player.getDirection().getName());
		m.put("leben", Math.round(mc.player.getHealth() * 10) / 10.0);
		m.put("hunger", mc.player.getFoodData().getFoodLevel());
		long t = mc.level.getOverworldClockTime() % 24000;
		m.put("zeit", t);
		m.put("nacht", (t >= 12542 && t <= 23459) || mc.level.isThundering());
		m.put("dimension", mc.level.dimension().identifier().toString());
		m.put("fenster", mc.gui.screen() == null ? null : mc.gui.screen().getClass().getSimpleName());
		m.put("schnellleistePlatz", mc.player.getInventory().getSelectedSlot());
		List<String> inv = new ArrayList<>();
		for (int i = 0; i < 41; i++) {
			ItemStack s = mc.player.getInventory().getItem(i);
			if (!s.isEmpty()) inv.add(i + ": " + BuiltInRegistries.ITEM.getKey(s.getItem()).getPath() + " x" + s.getCount()
				+ (s.isDamageableItem() ? " (" + (s.getMaxDamage() - s.getDamageValue()) + "/" + s.getMaxDamage() + ")" : "")
				+ (s.isEnchanted() ? " " + s.getEnchantments().keySet().stream().map(e -> e.unwrapKey().map(k -> k.identifier().getPath()).orElse("?")).toList() : ""));
		}
		m.put("inventar", inv);
		m.put("bauplan", Blueprint.progress());
		m.put("sortieren", ChestSorter.stageName());
		m.put("meldungen", logLines());
		return m;
	}

	/** /bereich?von=x,y,z&bis=x,y,z: alle Bloecke ausser Luft als "x y z id[eigenschaften]" (max. 100.000 Bloecke). */
	private static String area(Minecraft mc, String query) {
		if (mc.level == null || query == null) return "nicht im Spiel oder Parameter fehlen";
		Map<String, String> q = new LinkedHashMap<>();
		for (String kv : query.split("&")) {
			String[] p = kv.split("=", 2);
			if (p.length == 2) q.put(p[0], p[1]);
		}
		int[] a = java.util.Arrays.stream(q.getOrDefault("von", "").split(",")).mapToInt(Integer::parseInt).toArray();
		int[] b = java.util.Arrays.stream(q.getOrDefault("bis", "").split(",")).mapToInt(Integer::parseInt).toArray();
		long vol = (long) (Math.abs(a[0] - b[0]) + 1) * (Math.abs(a[1] - b[1]) + 1) * (Math.abs(a[2] - b[2]) + 1);
		if (vol > 100_000) return "Bereich zu gross: " + vol;
		StringBuilder out = new StringBuilder();
		for (var pos : net.minecraft.core.BlockPos.betweenClosed(new net.minecraft.core.BlockPos(a[0], a[1], a[2]), new net.minecraft.core.BlockPos(b[0], b[1], b[2]))) {
			var st = mc.level.getBlockState(pos);
			if (st.isAir()) continue;
			out.append(pos.getX()).append(' ').append(pos.getY()).append(' ').append(pos.getZ()).append(' ')
				.append(BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath());
			String props = st.getValues().map(v -> v.property().getName() + "=" + v.valueName()).collect(java.util.stream.Collectors.joining(","));
			if (!props.isEmpty()) out.append('[').append(props).append(']');
			out.append('\n');
		}
		return out.toString();
	}

	/** Auf dem Spiel-Thread ausfuehren und auf das Ergebnis warten. */
	private static <T> T onClient(Minecraft mc, Supplier<T> task) throws Exception {
		return mc.submit(task).get(10, TimeUnit.SECONDS);
	}

	private static void send(HttpExchange ex, int code, String text) throws IOException {
		byte[] b = text.getBytes(StandardCharsets.UTF_8);
		ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
		ex.sendResponseHeaders(code, b.length);
		try (OutputStream o = ex.getResponseBody()) {
			o.write(b);
		}
	}

	private LocalApi() {}
}
