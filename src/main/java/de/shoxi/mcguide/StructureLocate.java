package de.shoxi.mcguide;

import java.util.List;
import java.util.Map;

import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Serverseite von /finde: sucht wie /locate das naechste Bauwerk. Nur der Server kennt den Welt-Seed.
 * Gedacht fuer Allays: die sitzen in Kaefigen an Aussenposten und in Zellen im Waldanwesen.
 */
final class StructureLocate {
	record Kind(List<ResourceKey<Structure>> keys, String label) {}

	static final Map<String, Kind> KINDS = Map.of(
		"aussenposten", new Kind(List.of(BuiltinStructures.PILLAGER_OUTPOST), "Außenposten"),
		"anwesen", new Kind(List.of(BuiltinStructures.WOODLAND_MANSION), "Anwesen"),
		// was naeher liegt
		"allay", new Kind(List.of(BuiltinStructures.PILLAGER_OUTPOST, BuiltinStructures.WOODLAND_MANSION), "Allay-Versteck"));

	/** Suchradius in Chunks, wie bei /locate. */
	private static final int RADIUS = 100;

	static GuideNet.StructureFound locate(ServerPlayer p, String kindId) {
		GuideNet.StructureFound none = new GuideNet.StructureFound(kindId, false, BlockPos.ZERO);
		Kind kind = KINDS.get(kindId);
		if (kind == null) return none;
		ServerLevel level = p.level();
		var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		List<Holder<Structure>> holders = kind.keys().stream().<Holder<Structure>>flatMap(k -> registry.get(k).stream()).toList();
		if (holders.isEmpty()) return none;
		Pair<BlockPos, Holder<Structure>> hit = level.getChunkSource().getGenerator()
			.findNearestMapStructure(level, HolderSet.direct(holders), p.blockPosition(), RADIUS, false);
		return hit == null ? none : new GuideNet.StructureFound(kindId, true, hit.getFirst());
	}

	private StructureLocate() {}
}
