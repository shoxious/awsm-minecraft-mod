package de.shoxi.mcguide;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** /finde: fragt den Server (Guide-Mod nötig) nach der nächsten Struktur und setzt einen Wegpunkt. */
final class StructureFinder {
	static int request(Minecraft mc, String kind) {
		if (mc.player == null) {
			return 0;
		} else if (!ClientPlayNetworking.canSend(GuideNet.StructureRequest.TYPE)) {
			mc.player
				.sendSystemMessage(
					Component.literal(
							"/finde geht nur im Einzelspieler oder mit der Mod auf dem Server. Unterwegs meldet der Außenposten-Finder (Menü J) Außenposten in Sichtweite."
						)
						.withStyle(ChatFormatting.RED)
				);
			return 0;
		} else {
			ClientPlayNetworking.send(new GuideNet.StructureRequest(kind));
			mc.player.sendOverlayMessage(Component.literal("Suche " + StructureLocate.KINDS.get(kind).label() + " …"));
			return 1;
		}
	}

	static void onFound(Minecraft mc, GuideNet.StructureFound found) {
		StructureLocate.Kind kind = StructureLocate.KINDS.get(found.kind());
		if (mc.player != null && kind != null) {
			if (!found.found()) {
				mc.player
					.sendSystemMessage(
						Component.literal("Kein " + kind.label() + " in der Nähe gefunden (gibt es nur in der Oberwelt)").withStyle(ChatFormatting.YELLOW)
					);
			} else {
				BlockPos p = found.pos();
				int y = mc.player.getBlockY();
				int dist = (int)Math.sqrt(mc.player.distanceToSqr(p.getX(), y, p.getZ()));
				String where = " bei " + p.getX() + " " + p.getZ() + " (" + dist + " Blöcke)";
				String name = Waypoints.addAuto(mc, kind.label(), p.getX(), y, p.getZ(), 96);
				if (name == null) {
					mc.player
						.sendSystemMessage(Component.literal(kind.label() + where + " – ist schon als Wegpunkt gespeichert (/wp)").withStyle(ChatFormatting.GOLD));
				} else {
					Waypoints.target(mc, name);
					mc.player.sendSystemMessage(Component.literal("\ud83c\udfda " + name + where + " – Wegpunkt gesetzt").withStyle(ChatFormatting.GOLD));
				}
			}
		}
	}

	private StructureFinder() {
	}
}
