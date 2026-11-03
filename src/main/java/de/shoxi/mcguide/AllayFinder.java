package de.shoxi.mcguide;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.allay.Allay;

/**
 * Allay-Finder: laesst Allays in Sichtweite tuerkis leuchten (auch durch Waende) und setzt beim ersten
 * Sichten einen Wegpunkt. Allays mit Item in der Hand gehoeren meist schon jemandem und werden ignoriert.
 * Fuer weiter weg: /finde allay (naechster Aussenposten oder naechstes Anwesen).
 */
final class AllayFinder {
	static final int COLOR = 0x40E0FF;
	private static int ticks;

	static void tick(Minecraft mc) {
		if (!GuideClient.settings.allayFinder || mc.player == null || mc.level == null || ticks++ % 20 != 0) return;
		for (Entity e : mc.level.entitiesForRendering()) {
			if (!wild(e)) continue;
			String name = Waypoints.addAuto(mc, "Allay", e.getBlockX(), e.getBlockY(), e.getBlockZ(), 64);
			if (name == null) continue; // hier kennen wir schon einen
			mc.player.sendSystemMessage(Component.literal("🧚 " + name + " gefunden bei " + e.getBlockX() + " " + e.getBlockY() + " "
				+ e.getBlockZ() + " – Wegpunkt gesetzt").withStyle(ChatFormatting.AQUA));
		}
	}

	static boolean glows(Entity e) {
		return GuideClient.settings.allayFinder && wild(e);
	}

	private static boolean wild(Entity e) {
		return e instanceof Allay a && a.getMainHandItem().isEmpty();
	}

	private AllayFinder() {}
}
