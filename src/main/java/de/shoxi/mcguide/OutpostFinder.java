package de.shoxi.mcguide;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/** Außenposten-/Anwesen-Finder: erkennt die typischen Blöcke in geladenen Oberwelt-Chunks, rein clientseitig. */
final class OutpostFinder {
	private static final int CHUNK_RADIUS = 16;
	private static final Set<Long> scanned = new HashSet<>();
	private static Level lastLevel;
	private static int ticks;

	static void tick(Minecraft mc) {
		if (GuideClient.settings.outpostFinder && mc.player != null && mc.level != null) {
			if (mc.level != lastLevel) {
				lastLevel = mc.level;
				scanned.clear();
			}

			if (mc.level.dimension() == Level.OVERWORLD && ticks++ % 40 == 0) {
				int cx = mc.player.blockPosition().getX() >> 4;
				int cz = mc.player.blockPosition().getZ() >> 4;

				for (int x = cx - 16; x <= cx + 16; x++) {
					for (int z = cz - 16; z <= cz + 16; z++) {
						long key = (long)x << 32 | z & 4294967295L;
						if (!scanned.contains(key)) {
							LevelChunk chunk = mc.level.getChunkSource().getChunkNow(x, z);
							if (chunk != null) {
								scanned.add(key);
								scan(mc, chunk);
							}
						}
					}
				}
			}
		}
	}

	private static void scan(Minecraft mc, LevelChunk chunk) {
		LevelChunkSection[] sections = chunk.getSections();
		int minSection = mc.level.getMinY() >> 4;

		for (int i = 0; i < sections.length; i++) {
			LevelChunkSection sec = sections[i];
			if (!sec.hasOnlyAir() && has(sec, Blocks.BIRCH_PLANKS) && has(sec, Blocks.DARK_OAK_LOG) && has(sec, Blocks.COBBLESTONE)) {
				boolean mansion = has(sec, Blocks.POLISHED_ANDESITE) || has(sec, Blocks.BOOKSHELF);

				for (int y = 0; y < 16; y++) {
					for (int z = 0; z < 16; z++) {
						for (int x = 0; x < 16; x++) {
							if (sec.getBlockState(x, y, z).is(Blocks.DARK_OAK_LOG)) {
								found(
									mc,
									mansion ? "Anwesen" : "Außenposten",
									chunk.getPos().getMinBlockX() + x,
									(minSection + i) * 16 + y,
									chunk.getPos().getMinBlockZ() + z
								);
								return;
							}
						}
					}
				}
			}
		}
	}

	private static boolean has(LevelChunkSection sec, Block block) {
		return sec.getStates().maybeHas(s -> s.is(block));
	}

	private static void found(Minecraft mc, String kind, int x, int y, int z) {
		String name = Waypoints.addAuto(mc, kind, x, y, z, 128);
		if (name != null) {
			mc.player
				.sendSystemMessage(
					Component.literal("\ud83c\udfda " + name + " gefunden bei " + x + " " + y + " " + z + " – Wegpunkt gesetzt, dort können Allays sein")
						.withStyle(ChatFormatting.GOLD)
				);
		}
	}

	private OutpostFinder() {
	}
}
