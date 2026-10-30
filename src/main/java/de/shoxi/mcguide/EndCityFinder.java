package de.shoxi.mcguide;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Endstadt-Finder: sucht in geladenen End-Chunks nach Purpur (Endstadt) und Drachenkopf (Endschiff)
 * und in Rahmen nach Elytren. Setzt jeweils automatisch einen Wegpunkt; eine gefundene Elytra wird sofort Ziel.
 * Alles landet zusaetzlich in der End-Karte (EndMap), inklusive ob die Elytra noch da ist.
 */
final class EndCityFinder {
	private static final int CHUNK_RADIUS = 16;
	private static final Set<Long> scanned = new HashSet<>();
	private static Level lastLevel;
	private static int ticks;

	private record Frame(BlockPos pos, boolean elytra) {}

	static void tick(Minecraft mc) {
		EndMap.tick();
		if (!GuideClient.settings.endCityFinder || mc.player == null || mc.level == null) return;
		if (mc.level != lastLevel) {
			lastLevel = mc.level;
			scanned.clear();
			EndMap.save();
		}
		if (mc.level.dimension() != Level.END || ticks++ % 40 != 0) return;
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
		checkFrames(mc);
	}

	/** Rahmen in Sichtweite: neue Elytren melden, bei bekannten Schiffen den Zustand aktualisieren. */
	private static void checkFrames(Minecraft mc) {
		List<Frame> frames = new ArrayList<>();
		for (Entity e : mc.level.entitiesForRendering()) {
			if (e instanceof ItemFrame f) frames.add(new Frame(f.blockPosition(), f.getItem().is(Items.ELYTRA)));
		}
		for (Frame f : frames) {
			if (!f.elytra()) continue;
			EndMap.Marker ship = EndMap.near(mc, "ship", f.pos().getX(), f.pos().getZ(), 24);
			if (ship == null) {
				String name = found(mc, "Elytra", f.pos(), 32, "🪽 Elytra im Rahmen", true);
				if (name != null) EndMap.mark(mc, "ship", name, f.pos().getX(), f.pos().getY(), f.pos().getZ(), EndMap.ELYTRA);
			} else {
				if (ship.state != EndMap.ELYTRA) Waypoints.target(mc, ship.name);
				EndMap.setState(ship, EndMap.ELYTRA);
			}
		}
		BlockPos me = mc.player.blockPosition();
		for (EndMap.Marker m : EndMap.data(mc).markers) {
			if (!m.type.equals("ship")) continue;
			long dx = m.x - me.getX(), dz = m.z - me.getZ();
			if (dx * dx + dz * dz > 48 * 48) continue; // zu weit weg, Rahmen evtl. noch nicht geladen
			// kein Rahmen mit Elytra in der Naehe = leerer oder abgebauter Rahmen = geplündert
			boolean elytra = false;
			for (Frame f : frames) {
				if (f.elytra() && f.pos().distSqr(new BlockPos(m.x, m.y, m.z)) <= 20 * 20) elytra = true;
			}
			EndMap.setState(m, elytra ? EndMap.ELYTRA : EndMap.LOOTED);
		}
	}

	private static boolean isShip(BlockState s) {
		return s.is(Blocks.DRAGON_HEAD) || s.is(Blocks.DRAGON_WALL_HEAD);
	}

	private static boolean isExit(BlockState s) {
		return s.is(Blocks.END_GATEWAY) || s.is(Blocks.END_PORTAL);
	}

	/** Ausgangsportal (Oberwelt) oder Endtor; ohne eigenen Wegpunkt, der entsteht erst per Klick auf der Karte. */
	private static void exit(Minecraft mc, BlockState s, BlockPos p) {
		String type = s.is(Blocks.END_PORTAL) ? "portal" : "gateway";
		if (EndMap.near(mc, type, p.getX(), p.getZ(), type.equals("portal") ? 16 : 6) != null) return;
		EndMap.mark(mc, type, null, p.getX(), p.getY(), p.getZ(), EndMap.UNKNOWN);
		if (type.equals("gateway") && Math.abs(p.getX()) + Math.abs(p.getZ()) > 300) {
			mc.player.sendOverlayMessage(Component.literal("🌀 Endtor zur Hauptinsel gefunden (Karte: " + GuideClient.endMapKeyName() + ")").withStyle(ChatFormatting.AQUA));
		}
	}

	private static void scan(Minecraft mc, LevelChunk chunk) {
		LevelChunkSection[] sections = chunk.getSections();
		int minSection = mc.level.getMinY() >> 4;
		boolean land = false, city = false, ship = false;
		for (int i = 0; i < sections.length; i++) {
			LevelChunkSection sec = sections[i];
			if (sec.hasOnlyAir()) continue;
			land = true;
			if (!sec.getStates().maybeHas(s -> s.is(Blocks.PURPUR_BLOCK) || isShip(s) || isExit(s))) continue;
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					for (int x = 0; x < 16; x++) {
						BlockState s = sec.getBlockState(x, y, z);
						BlockPos p = new BlockPos(chunk.getPos().getMinBlockX() + x, (minSection + i) * 16 + y, chunk.getPos().getMinBlockZ() + z);
						if (isExit(s)) exit(mc, s, p);
						if (!ship && isShip(s)) {
							ship = true;
							if (EndMap.near(mc, "ship", p.getX(), p.getZ(), 64) == null) {
								String name = found(mc, "Endschiff", p, 64, "⛵ Endschiff", false);
								if (name != null) EndMap.mark(mc, "ship", name, p.getX(), p.getY(), p.getZ(), EndMap.UNKNOWN);
							}
						}
						if (!city && s.is(Blocks.PURPUR_BLOCK)) {
							city = true;
							if (EndMap.near(mc, "city", p.getX(), p.getZ(), 160) == null) {
								String name = found(mc, "Endstadt", p, 160, "🏙 Endstadt", false);
								if (name != null) EndMap.mark(mc, "city", name, p.getX(), p.getY(), p.getZ(), EndMap.UNKNOWN);
							}
						}
					}
				}
			}
		}
		EndMap.chunk(mc, chunk.getPos().x(), chunk.getPos().z(), land);
	}

	private static String found(Minecraft mc, String prefix, BlockPos p, int minDist, String label, boolean makeTarget) {
		String name = Waypoints.addAuto(mc, prefix, p.getX(), p.getY(), p.getZ(), minDist);
		if (name == null) return null; // kennen wir schon
		if (makeTarget) Waypoints.target(mc, name);
		mc.player.sendSystemMessage(Component.literal(label + " („" + name + "“) bei " + p.getX() + " " + p.getY() + " " + p.getZ() + " – Wegpunkt gesetzt (Karte: " + GuideClient.endMapKeyName() + ")")
			.withStyle(makeTarget ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_PURPLE));
		return name;
	}

	private EndCityFinder() {}
}
