package de.shoxi.mcguide;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Kleine Zeile oben links: das naechste offene Ziel. */
final class GuideHud {
	static void register() {
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(GuideServer.MOD_ID, "next"), (g, tick) -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player == null || mc.gui.hud.isHidden() || mc.gui.screen() != null
				|| mc.getDebugOverlay().showDebugScreen()) return;
			int y = 2;
			Goals.Goal next = GuideClient.next();
			if (GuideClient.synced && GuideClient.settings.hud && next != null) {
				y = line(g, mc, "➜ " + next.text(), y, 0xFF9FE69F);
			}
			if (OreFinder.active()) {
				y = line(g, mc, "⛏ " + OreFinder.summary(), y, 0xFF8FD8FF);
			}
			if (GuideClient.settings.coords) {
				for (String s : Coords.lines(mc)) y = line(g, mc, s, y, 0xFFE0E0E0);
			}
			String wp = Waypoints.hudLine(mc);
			if (wp != null) y = line(g, mc, wp, y, 0xFF40E0FF);
			if (GuideClient.settings.chestFinder) {
				y = line(g, mc, "📦 Kisten/Spawner: " + Finders.chestCount(), y, 0xFFFFC060);
			}
			if (TreeFeller.remaining() > 0) {
				y = line(g, mc, TreeFeller.status(), y, 0xFFE6C88F);
			}
			if (GuideClient.settings.autoFeed) {
				y = line(g, mc, "🐄 Auto-Füttern an (U)", y, 0xFFE6C8A0);
			}
			if (GuideClient.settings.autoCrops) {
				line(g, mc, "🌾 Auto-Ernte an (F7)", y, 0xFFE6E68F);
			}
		});
	}

	private static int line(GuiGraphicsExtractor g, Minecraft mc, String s, int y, int color) {
		Component text = Component.literal(s);
		g.fill(2, y, mc.font.width(text) + 8, y + 13, 0x80000000);
		g.text(mc.font, text, 5, y + 3, color);
		return y + 14;
	}

	private GuideHud() {}
}
