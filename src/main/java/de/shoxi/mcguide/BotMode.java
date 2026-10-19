package de.shoxi.mcguide;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.world.InteractionHand;

/**
 * AFK-Bot fuer den Headless-Client (-Dmcguide.bot=host:port): verbindet sich beim Start automatisch,
 * bewegt sich alle paar Minuten minimal (gegen Idle-Kick), steht nach dem Tod wieder auf und
 * beendet das Spiel bei jeder Trennung. Ob und wann er neu startet, entscheidet der Supervisor auf dem Server.
 */
final class BotMode {
	private static final int AFK_TICKS = 20 * 60 * 4;
	private static boolean connecting;
	private static int ticks;

	static void register() {
		String addr = System.getProperty("mcguide.bot");
		if (addr == null || addr.isBlank()) return;
		GuideServer.LOG.info("Bot-Modus aktiv, Ziel {}", addr);
		ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
			if (screen instanceof TitleScreen && !connecting) {
				connecting = true;
				mc.execute(() -> ConnectScreen.startConnecting(screen, mc, ServerAddress.parseString(addr),
					new ServerData("Bot", addr, ServerData.Type.OTHER), false, null));
			} else if (screen instanceof DisconnectedScreen) {
				GuideServer.LOG.info("Bot getrennt: {}", screen.getTitle().getString());
				mc.execute(mc::stop);
			} else if (screen instanceof DeathScreen && mc.player != null) {
				mc.player.respawn();
				mc.execute(() -> mc.gui.setScreen(null));
			}
		});
		ClientTickEvents.END_CLIENT_TICK.register(BotMode::tick);
	}

	private static void tick(Minecraft mc) {
		if (mc.player == null) return;
		if (++ticks % AFK_TICKS != 0) return;
		// minimale Drehung hin und zurueck plus Armschwung: zaehlt fuer den Server als Aktivitaet
		float yaw = mc.player.getYRot();
		mc.player.setYRot(yaw + ((ticks / AFK_TICKS) % 2 == 0 ? 3f : -3f));
		mc.player.swing(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().getAttackAnimation(), false);
	}

	private BotMode() {}
}
