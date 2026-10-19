package de.shoxi.mcguide;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/** Kisten-/Spawner-Finder (Kaesten durch Waende) und Mob-Markierung (Leuchtumriss), rein clientseitig. */
final class Finders {
	private record Hit(BlockPos pos, int color) {}

	private static final int CHUNK_RADIUS = 5;
	private static final double MOB_RANGE = 64;
	private static List<Hit> hits = List.of();
	private static int ticks;

	static void toggleChests(Minecraft mc) {
		GuideState s = GuideClient.settings;
		s.chestFinder = !s.chestFinder;
		s.save();
		hits = List.of();
		ticks = 0;
		mc.player.sendOverlayMessage(Component.literal("Kisten-Finder " + (s.chestFinder ? "an" : "aus"))
			.withStyle(s.chestFinder ? ChatFormatting.GREEN : ChatFormatting.GRAY));
	}

	static void toggleMobs(Minecraft mc) {
		GuideState s = GuideClient.settings;
		s.mobGlow = !s.mobGlow;
		s.save();
		mc.player.sendOverlayMessage(Component.literal("Mob-Markierung " + (s.mobGlow ? "an" : "aus"))
			.withStyle(s.mobGlow ? ChatFormatting.GREEN : ChatFormatting.GRAY));
	}

	static void tick(Minecraft mc) {
		if (!GuideClient.settings.chestFinder || mc.player == null || mc.level == null) {
			hits = List.of();
			return;
		}
		if (ticks++ % 20 == 0) scan(mc);
		for (Hit h : hits) {
			Gizmos.cuboid(h.pos(), GizmoStyle.strokeAndFill(0xFF000000 | h.color(), 2.5f, 0x30000000 | h.color())).setAlwaysOnTop();
		}
	}

	private static void scan(Minecraft mc) {
		List<Hit> found = new ArrayList<>();
		int cx = mc.player.blockPosition().getX() >> 4;
		int cz = mc.player.blockPosition().getZ() >> 4;
		for (int x = cx - CHUNK_RADIUS; x <= cx + CHUNK_RADIUS; x++) {
			for (int z = cz - CHUNK_RADIUS; z <= cz + CHUNK_RADIUS; z++) {
				LevelChunk chunk = mc.level.getChunkSource().getChunkNow(x, z);
				if (chunk == null) continue;
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					int c = color(be);
					if (c != 0) found.add(new Hit(be.getBlockPos(), c));
				}
			}
		}
		hits = found;
	}

	private static int color(BlockEntity be) {
		if (be instanceof SpawnerBlockEntity || be instanceof TrialSpawnerBlockEntity) return 0xFF3030;
		if (be instanceof VaultBlockEntity) return 0xFFD700;
		if (be instanceof ShulkerBoxBlockEntity) return 0xC060FF;
		if (be instanceof EnderChestBlockEntity) return 0x20A080;
		if (be instanceof ChestBlockEntity) return 0xFFA020;
		if (be instanceof BarrelBlockEntity) return 0xB07840;
		return 0;
	}

	static int chestCount() {
		return hits.size();
	}

	/** Fuer den Glow-Mixin: soll dieses Wesen leuchten? */
	static boolean glows(Entity e) {
		Minecraft mc = Minecraft.getInstance();
		return GuideClient.settings.mobGlow && e instanceof LivingEntity && e != mc.player
			&& mc.player != null && e.distanceToSqr(mc.player) < MOB_RANGE * MOB_RANGE;
	}

	/** Umrissfarbe: Monster rot, Spieler blau, Tiere gruen. */
	static int glowColor(Entity e) {
		if (e instanceof Enemy) return 0xFF4040;
		if (e instanceof Player) return 0x40A0FF;
		return 0x50FF50;
	}

	private Finders() {}
}
