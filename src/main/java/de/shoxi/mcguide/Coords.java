package de.shoxi.mcguide;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LightLayer;

/** Koordinaten-Zeilen fuers HUD: Position, Richtung, Biom, Licht, Nether-Umrechnung. */
final class Coords {
	static List<String> lines(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null || mc.level == null) return List.of();
		BlockPos pos = p.blockPosition();
		String dir = switch (p.getDirection()) {
			case NORTH -> "Norden";
			case SOUTH -> "Süden";
			case EAST -> "Osten";
			case WEST -> "Westen";
			default -> "";
		};
		String biome = mc.level.getBiome(pos).unwrapKey()
			.map(k -> Component.translatable("biome." + k.identifier().getNamespace() + "." + k.identifier().getPath()).getString())
			.orElse("?");
		int light = mc.level.getBrightness(LightLayer.BLOCK, pos);
		String dim = mc.level.dimension().identifier().getPath();
		String other = switch (dim) {
			case "overworld" -> " · Nether " + Math.floorDiv(pos.getX(), 8) + " " + Math.floorDiv(pos.getZ(), 8);
			case "the_nether" -> " · Oberwelt " + pos.getX() * 8 + " " + pos.getZ() * 8;
			default -> "";
		};
		return List.of(
			"📍 " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " · " + dir,
			biome + " · Licht " + light + (light == 0 ? " (Mobs!)" : "") + other);
	}

	private Coords() {}
}
