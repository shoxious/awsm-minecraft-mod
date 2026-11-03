package de.shoxi.mcguide;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Loot-Finder: nur Generiertes, nichts von Spielern Platziertes (durch Waende).
 * Ungeoeffnete Loot-Kisten/-Faesser/-Toepfe/-Loren kennt nur der Server ({@link LootScan}); ohne Mod auf dem
 * Server bleiben Spawner, Tresore, verdaechtiger Sand/Kies und herumliegende Items.
 * Dazu und Mob-Markierung (Leuchtumriss), rein clientseitig. */
final class Finders {
	private record Hit(BlockPos pos, int color) {}
	private record EntityHit(Entity entity, int color) {}

	private static final int CHUNK_RADIUS = LootScan.CHUNK_RADIUS;
	private static final double MOB_RANGE = 64;
	private static List<Hit> hits = List.of();
	private static List<EntityHit> entityHits = List.of();
	/** Letzte Antwort des Servers, null ohne Mod auf dem Server. */
	private static Set<BlockPos> lootBlocks;
	private static Set<Integer> lootEntities = Set.of();
	private static int ticks;

	static void toggleChests(Minecraft mc) {
		GuideState s = GuideClient.settings;
		s.chestFinder = !s.chestFinder;
		s.save();
		hits = List.of();
		entityHits = List.of();
		ticks = 0;
		mc.player.sendOverlayMessage(Component.literal("Loot-Finder " + (s.chestFinder ? "an" : "aus"))
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
			entityHits = List.of();
			return;
		}
		if (ticks++ % 20 == 0) scan(mc);
		for (Hit h : hits) {
			Gizmos.cuboid(h.pos(), GizmoStyle.strokeAndFill(0xFF000000 | h.color(), 2.5f, 0x30000000 | h.color())).setAlwaysOnTop();
		}
		// Wesen bewegen sich: Box jeden Tick an der aktuellen Position zeichnen
		for (EntityHit h : entityHits) {
			if (h.entity().isRemoved()) continue;
			Gizmos.cuboid(h.entity().getBoundingBox(), GizmoStyle.strokeAndFill(0xFF000000 | h.color(), 2f, 0x30000000 | h.color())).setAlwaysOnTop();
		}
	}

	static void onLootSpots(GuideNet.LootSpots spots) {
		lootBlocks = new HashSet<>(spots.blocks());
		lootEntities = new HashSet<>(spots.entities());
	}

	private static void scan(Minecraft mc) {
		// Antwort kommt asynchron und gilt ab dem naechsten Scan
		if (ClientPlayNetworking.canSend(GuideNet.LootRequest.TYPE)) ClientPlayNetworking.send(new GuideNet.LootRequest());
		else lootBlocks = null;
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

		List<EntityHit> foundEntities = new ArrayList<>();
		double range = (CHUNK_RADIUS + 1) * 16;
		for (Entity e : mc.level.entitiesForRendering()) {
			if (e.distanceToSqr(mc.player) > range * range) continue;
			int c = color(e);
			if (c != 0) foundEntities.add(new EntityHit(e, c));
		}
		entityHits = foundEntities;
	}

	private static int color(BlockEntity be) {
		// koennen Spieler nicht platzieren, also immer generiert
		if (be instanceof SpawnerBlockEntity || be instanceof TrialSpawnerBlockEntity) return 0xFF3030;
		if (be instanceof VaultBlockEntity) return 0xFFD700;
		if (be instanceof BrushableBlockEntity) return 0xF0E080;
		if (lootBlocks == null || !lootBlocks.contains(be.getBlockPos())) return 0;
		if (be instanceof ChestBlockEntity) return 0xFFA020;
		if (be instanceof BarrelBlockEntity) return 0xB07840;
		if (be instanceof DecoratedPotBlockEntity) return 0xD06A40;
		return 0xA0A0A0;
	}

	/** Herumliegende Items und ungeoeffnete Kistenloren/-boote. */
	private static int color(Entity e) {
		if (e instanceof ItemEntity) return 0xFFFFFF;
		if (e instanceof ContainerEntity && lootEntities.contains(e.getId())) return 0xFFA020;
		return 0;
	}

	static int chestCount() {
		return hits.size() + entityHits.size();
	}

	/** Fuer den Glow-Mixin: soll dieses Wesen leuchten? */
	static boolean glows(Entity e) {
		if (AllayFinder.glows(e)) return true;
		Minecraft mc = Minecraft.getInstance();
		return GuideClient.settings.mobGlow && e instanceof LivingEntity && e != mc.player
			&& mc.player != null && e.distanceToSqr(mc.player) < MOB_RANGE * MOB_RANGE;
	}

	/** Umrissfarbe: Allays tuerkis, Monster rot, Spieler blau, Tiere gruen. */
	static int glowColor(Entity e) {
		if (AllayFinder.glows(e)) return AllayFinder.COLOR;
		if (e instanceof Enemy) return 0xFF4040;
		if (e instanceof Player) return 0x40A0FF;
		return 0x50FF50;
	}

	private Finders() {}
}
