package de.shoxi.mcguide;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * End-Karte (Standardtaste H): alle bisher geladenen End-Chunks (Land hell, Leere dunkel), Endstaedte und
 * Endschiffe mit Elytra-Zustand. Mausrad = Zoom, Ziehen = verschieben, Klick auf Markierung = Ziel.
 */
public class EndMapScreen extends Screen {
	private static final int[] ZOOMS = {1, 2, 4, 8, 16, 32, 64}; // Bloecke pro Pixel
	private static final int COLOR_VOID = 0xFF16141E;
	private static final int COLOR_LAND = 0xFF7D7A58;
	private static final int COLOR_CITY = 0xFFB070D8;
	private static final int COLOR_ELYTRA = 0xFF40F070;
	private static final int COLOR_LOOTED = 0xFF707070;
	private static final int COLOR_UNKNOWN = 0xFFF0D040;
	private static final int COLOR_GATEWAY = 0xFF40D8F0;
	private static final int COLOR_PORTAL = 0xFFFF8C30;

	private int zoom = 3;
	private double centerX, centerZ;
	private boolean dragged;

	public EndMapScreen() {
		super(Component.literal("End-Karte"));
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			centerX = mc.player.getX();
			centerZ = mc.player.getZ();
		}
		EndMap.save();
	}

	@Override
	protected void init() {
		addRenderableWidget(Button.builder(Component.literal("🏠 Heimweg"), b -> {
			home(Minecraft.getInstance());
			onClose();
		}).bounds(width - 252, height - 26, 80, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Zu mir"), b -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null) {
				centerX = mc.player.getX();
				centerZ = mc.player.getZ();
			}
		}).bounds(width - 168, height - 26, 80, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Schließen (" + GuideClient.endMapKeyName() + ")"), b -> onClose())
			.bounds(width - 84, height - 26, 80, 20).build());
	}

	private static boolean isExit(EndMap.Marker m) {
		return m.type.equals("gateway") || m.type.equals("portal");
	}

	private static String label(EndMap.Marker m) {
		return switch (m.type) {
			case "portal" -> "Ausgang zur Oberwelt";
			case "gateway" -> Math.abs(m.x) + Math.abs(m.z) > 300 ? "Endtor zur Hauptinsel" : "Endtor zu den Außeninseln";
			case "city" -> m.name + " – Endstadt";
			default -> m.name + " – " + (m.state == EndMap.ELYTRA ? "Elytra da!" : m.state == EndMap.LOOTED ? "geplündert" : "Schiff, Elytra unbekannt");
		};
	}

	/** Marker zum Ziel machen; Ausgaenge bekommen erst jetzt einen Wegpunkt. */
	private static void select(Minecraft mc, EndMap.Marker m) {
		if (isExit(m)) Waypoints.setTarget(mc, m.type.equals("portal") ? "Ausgang" : "Endtor", m.x, m.y, m.z);
		else Waypoints.target(mc, m.name);
	}

	/**
	 * Heimweg: draussen auf den Aussenbereichen zum naechsten Endtor zur Hauptinsel,
	 * sonst zum Ausgangsportal in der Mitte (auch wenn es noch nicht gesehen wurde, es steht immer bei 0/0).
	 */
	private static void home(Minecraft mc) {
		if (mc.player == null) return;
		double px = mc.player.getX(), pz = mc.player.getZ();
		EndMap.Marker best = null;
		double bestD = Double.MAX_VALUE;
		boolean outside = px * px + pz * pz > 500 * 500;
		for (EndMap.Marker m : EndMap.data(mc).markers) {
			boolean wanted = outside ? m.type.equals("gateway") && Math.abs(m.x) + Math.abs(m.z) > 300 : m.type.equals("portal");
			if (!wanted) continue;
			double d = (m.x - px) * (m.x - px) + (m.z - pz) * (m.z - pz);
			if (d < bestD) {
				bestD = d;
				best = m;
			}
		}
		if (best != null) select(mc, best);
		else if (outside) mc.player.sendSystemMessage(Component.literal(
			"Noch kein Endtor zur Hauptinsel bekannt – such eine kleine Bedrock-Säule mit lila Lichtstrahl, oder stirb (dann Spawn in der Oberwelt)."));
		else Waypoints.setTarget(mc, "Ausgang", 0, 64, 0);
	}

	private int scale() {
		return ZOOMS[zoom];
	}

	private int sx(double x) {
		return (int) Math.floor((x - centerX) / scale()) + width / 2;
	}

	private int sz(double z) {
		return (int) Math.floor((z - centerZ) / scale()) + height / 2;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		g.fill(0, 0, width, height, 0xFF050408);
		Minecraft mc = Minecraft.getInstance();
		EndMap.Data d = EndMap.data(mc);

		// Chunks: bei grobem Zoom auf Pixelraster zusammenfassen, damit nicht zehntausende Rechtecke entstehen
		int cell = Math.max(1, 16 / scale());
		Map<Long, Boolean> cells = new HashMap<>();
		for (long key : d.explored) {
			int cx = (int) (key >> 32), cz = (int) key;
			int px = sx(cx * 16), pz = sz(cz * 16);
			if (px < -cell || pz < -cell || px > width || pz > height) continue;
			long pk = (long) px << 32 | (pz & 0xFFFFFFFFL);
			if (d.land.contains(key)) cells.put(pk, true);
			else cells.putIfAbsent(pk, false);
		}
		for (Map.Entry<Long, Boolean> e : cells.entrySet()) {
			int px = (int) (e.getKey() >> 32), pz = (int) (long) e.getKey();
			g.fill(px, pz, px + cell, pz + cell, e.getValue() ? COLOR_LAND : COLOR_VOID);
		}

		// Zentrum der Hauptinsel und 1000-Block-Ring (ab dort beginnen die Aussenbereiche)
		g.fill(sx(0) - 3, sz(0), sx(0) + 4, sz(0) + 1, 0xFFFFFFFF);
		g.fill(sx(0), sz(0) - 3, sx(0) + 1, sz(0) + 4, 0xFFFFFFFF);
		for (int a = 0; a < 360; a += 2) {
			double r = Math.toRadians(a);
			int px = sx(Math.cos(r) * 1000), pz = sz(Math.sin(r) * 1000);
			g.fill(px, pz, px + 1, pz + 1, 0x60FFFFFF);
		}

		EndMap.Marker hover = null;
		int elytraCount = 0;
		for (EndMap.Marker m : d.markers) {
			int px = sx(m.x), pz = sz(m.z);
			boolean ship = m.type.equals("ship");
			int color = m.type.equals("gateway") ? COLOR_GATEWAY : m.type.equals("portal") ? COLOR_PORTAL
				: !ship ? COLOR_CITY : m.state == EndMap.ELYTRA ? COLOR_ELYTRA : m.state == EndMap.LOOTED ? COLOR_LOOTED : COLOR_UNKNOWN;
			int r = ship || m.type.equals("portal") ? 4 : isExit(m) ? 2 : 3;
			g.fill(px - r - 1, pz - r - 1, px + r + 2, pz + r + 2, 0xFF000000);
			g.fill(px - r, pz - r, px + r + 1, pz + r + 1, color);
			if (ship && m.state == EndMap.ELYTRA) elytraCount++;
			if (Math.abs(mouseX - px) <= r + 2 && Math.abs(mouseY - pz) <= r + 2) hover = m;
		}

		if (mc.player != null && mc.level != null) {
			int px = sx(mc.player.getX()), pz = sz(mc.player.getZ());
			g.fill(px - 2, pz - 2, px + 3, pz + 3, 0xFFFFFFFF);
			double yaw = Math.toRadians(mc.player.getYRot());
			for (int i = 3; i <= 8; i++) {
				int lx = px + (int) Math.round(-Math.sin(yaw) * i), lz = pz + (int) Math.round(Math.cos(yaw) * i);
				g.fill(lx, lz, lx + 1, lz + 1, 0xFFFFFFFF);
			}
		}

		g.fill(0, 0, width, 34, 0xC0000000);
		g.text(font, Component.literal("🪽 End-Karte").withStyle(ChatFormatting.BOLD), 6, 6, 0xFFFFFFFF);
		g.text(font, Component.literal("Elytra verfügbar: " + elytraCount), 100, 6, COLOR_ELYTRA);
		g.text(font, Component.literal("■ Elytra da  ").withStyle(s -> s.withColor(COLOR_ELYTRA))
			.append(Component.literal("■ Schiff, noch nicht geprüft  ").withStyle(s -> s.withColor(COLOR_UNKNOWN)))
			.append(Component.literal("■ geplündert  ").withStyle(s -> s.withColor(COLOR_LOOTED)))
			.append(Component.literal("■ Endstadt  ").withStyle(s -> s.withColor(COLOR_CITY)))
			.append(Component.literal("■ Endtor  ").withStyle(s -> s.withColor(COLOR_GATEWAY)))
			.append(Component.literal("■ Ausgang Oberwelt").withStyle(s -> s.withColor(COLOR_PORTAL))), 6, 20, 0xFFFFFFFF);
		String info = "1 Pixel = " + scale() + " Blöcke · Mausrad zoomt · ziehen verschiebt · Klick auf Punkt = Ziel";
		g.text(font, Component.literal(info), 6, height - 20, 0xFFB0B4BC);
		if (d.explored.isEmpty()) {
			Component c = Component.literal("Noch leer – flieg im End herum, die Karte füllt sich von selbst.");
			g.text(font, c, (width - font.width(c)) / 2, height / 2 + 12, 0xFFB0B4BC);
		}
		if (hover != null) {
			Component c = Component.literal(label(hover) + " (" + hover.x + " " + hover.y + " " + hover.z + ")");
			g.fill(mouseX + 8, mouseY - 4, mouseX + 14 + font.width(c), mouseY + 10, 0xE0000000);
			g.text(font, c, mouseX + 11, mouseY - 1, 0xFFFFFFFF);
		}
		super.extractRenderState(g, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		// Punkt unter der Maus bleibt beim Zoomen stehen
		double wx = centerX + (mouseX - width / 2.0) * scale(), wz = centerZ + (mouseY - height / 2.0) * scale();
		zoom = Math.max(0, Math.min(ZOOMS.length - 1, zoom - (int) Math.signum(scrollY)));
		centerX = wx - (mouseX - width / 2.0) * scale();
		centerZ = wz - (mouseY - height / 2.0) * scale();
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		centerX -= dx * scale();
		centerZ -= dy * scale();
		dragged = true;
		return true;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		dragged = false;
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (super.mouseReleased(event) || dragged) return true;
		Minecraft mc = Minecraft.getInstance();
		for (EndMap.Marker m : EndMap.data(mc).markers) {
			if (Math.abs(event.x() - sx(m.x)) <= 6 && Math.abs(event.y() - sz(m.z)) <= 6) {
				select(mc, m);
				onClose();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (GuideClient.endMapKeyMatches(event)) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
