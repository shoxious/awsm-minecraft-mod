package de.shoxi.mcguide;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.allay.Allay;

/** Allay-Finder: wilde Allays (leere Hand) leuchten türkis und bekommen einen Wegpunkt. */
final class AllayFinder {
	static final int COLOR = 4251903;
	private static int ticks;

	static void tick(Minecraft mc) {
		if (GuideClient.settings.allayFinder && mc.player != null && mc.level != null && ticks++ % 20 == 0) {
			for (Entity e : mc.level.entitiesForRendering()) {
				if (wild(e)) {
					String name = Waypoints.addAuto(mc, "Allay", e.getBlockX(), e.getBlockY(), e.getBlockZ(), 64);
					if (name != null) {
						mc.player
							.sendSystemMessage(
								Component.literal(
										"\ud83e\uddda " + name + " gefunden bei " + e.getBlockX() + " " + e.getBlockY() + " " + e.getBlockZ() + " – Wegpunkt gesetzt"
									)
									.withStyle(ChatFormatting.AQUA)
							);
					}
				}
			}
		}
	}

	static boolean glows(Entity e) {
		return GuideClient.settings.allayFinder && wild(e);
	}

	private static boolean wild(Entity e) {
		return e instanceof Allay a && a.getMainHandItem().isEmpty();
	}

	private AllayFinder() {
	}
}
