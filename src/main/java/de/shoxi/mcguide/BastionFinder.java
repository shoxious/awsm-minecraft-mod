package de.shoxi.mcguide;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Bastion-Finder: sucht in geladenen Nether-Chunks nach vergoldetem Schwarzstein (gibt es nur in Bastionen)
 * und setzt dort automatisch einen Wegpunkt. Jeder Chunk wird nur einmal durchsucht.
 */
final class BastionFinder {
	private static final int CHUNK_RADIUS = 16;
	private static final Set<Long> scanned = new HashSet<>();
	private static Level lastLevel;
	private static int ticks;

	static void tick(Minecraft mc) {
		if (!GuideClient.settings.bastionFinder || mc.player == null || mc.level == null) return;
		if (mc.level != lastLevel) {
			lastLevel = mc.level;
			scanned.clear();
		}
		if (mc.level.dimension() != Level.NETHER || ticks++ % 40 != 0) return;
		int cx = mc.player.blockPosition().getX() >> 4;
		int cz = mc.player.blockPosition().getZ() >> 4;
		for (int x = cx - CHUNK_RADIUS; x <= cx + CHUNK_RADIUS; x++) {
			for (int z = cz - CHUNK_RADIUS; z <= cz + CHUNK_RADIUS; z++) {
				long key = (long) x << 32 | (z & 0xFFFFFFFFL);
				if (scanned.contains(key)) continue;
				LevelChunk chunk = mc.level.getChunkSource().getChunkNow(x, z);
				if (chunk == null) continue;
				scanned.add(key);
				scan(mc, chunk);
			}
		}
	}

	private static void scan(Minecraft mc, LevelChunk chunk) {
		LevelChunkSection[] sections = chunk.getSections();
		int minSection = mc.level.getMinY() >> 4;
		for (int i = 0; i < sections.length; i++) {
			LevelChunkSection sec = sections[i];
			if (sec.hasOnlyAir() || !sec.getStates().maybeHas(s -> s.is(Blocks.GILDED_BLACKSTONE))) continue;
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					for (int x = 0; x < 16; x++) {
						if (!sec.getBlockState(x, y, z).is(Blocks.GILDED_BLACKSTONE)) continue;
						found(mc, chunk.getPos().getMinBlockX() + x, (minSection + i) * 16 + y, chunk.getPos().getMinBlockZ() + z);
						return;
					}
				}
			}
		}
	}

	private static void found(Minecraft mc, int x, int y, int z) {
		String name = Waypoints.addAuto(mc, "Bastion", x, y, z, 96);
		if (name == null) return; // diese Bastion kennen wir schon
		mc.player.sendSystemMessage(Component.literal("🏰 " + name + " gefunden bei " + x + " " + y + " " + z + " – Wegpunkt gesetzt")
			.withStyle(ChatFormatting.GOLD));
	}

	static void toggle(Minecraft mc) {
		GuideState s = GuideClient.settings;
		s.bastionFinder = !s.bastionFinder;
		s.save();
		scanned.clear();
		mc.player.sendOverlayMessage(Component.literal("Bastion-Finder " + (s.bastionFinder ? "an" : "aus"))
			.withStyle(s.bastionFinder ? ChatFormatting.GREEN : ChatFormatting.GRAY));
	}

	private BastionFinder() {}
}
