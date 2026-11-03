package de.shoxi.mcguide;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Clientseite von /finde: fragt den Server und setzt am Fund einen Wegpunkt als Ziel. */
final class StructureFinder {
	static int request(Minecraft mc, String kind) {
		if (mc.player == null) return 0;
		if (!ClientPlayNetworking.canSend(GuideNet.StructureRequest.TYPE)) {
			mc.player.sendSystemMessage(Component.literal("/finde geht nur im Einzelspieler oder mit der Mod auf dem Server. Unterwegs meldet der Außenposten-Finder (Menü J) Außenposten in Sichtweite.").withStyle(ChatFormatting.RED));
			return 0;
		}
		ClientPlayNetworking.send(new GuideNet.StructureRequest(kind));
		mc.player.sendOverlayMessage(Component.literal("Suche " + StructureLocate.KINDS.get(kind).label() + " …"));
		return 1;
	}

	static void onFound(Minecraft mc, GuideNet.StructureFound found) {
		StructureLocate.Kind kind = StructureLocate.KINDS.get(found.kind());
		if (mc.player == null || kind == null) return;
		if (!found.found()) {
			mc.player.sendSystemMessage(Component.literal("Kein " + kind.label() + " in der Nähe gefunden (gibt es nur in der Oberwelt)")
				.withStyle(ChatFormatting.YELLOW));
			return;
		}
		// die Hoehe kennt der Server nicht, ohne den Chunk zu laden – fuer Richtung und Entfernung reicht die eigene
		BlockPos p = found.pos();
		int y = mc.player.getBlockY();
		int dist = (int) Math.sqrt(mc.player.distanceToSqr(p.getX(), y, p.getZ()));
		String where = " bei " + p.getX() + " " + p.getZ() + " (" + dist + " Blöcke)";
		String name = Waypoints.addAuto(mc, kind.label(), p.getX(), y, p.getZ(), 96);
		if (name == null) {
			mc.player.sendSystemMessage(Component.literal(kind.label() + where + " – ist schon als Wegpunkt gespeichert (/wp)")
				.withStyle(ChatFormatting.GOLD));
			return;
		}
		Waypoints.target(mc, name);
		mc.player.sendSystemMessage(Component.literal("🏚 " + name + where + " – Wegpunkt gesetzt").withStyle(ChatFormatting.GOLD));
	}

	private StructureFinder() {}
}
