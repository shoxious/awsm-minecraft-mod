package de.shoxi.mcguide;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;

/** Server-Seite von /finde: Strukturen per Generator suchen. */
final class StructureLocate {
	static final Map<String, StructureLocate.Kind> KINDS = Map.of(
		"aussenposten",
		new StructureLocate.Kind(List.of(BuiltinStructures.PILLAGER_OUTPOST), "Außenposten"),
		"anwesen",
		new StructureLocate.Kind(List.of(BuiltinStructures.WOODLAND_MANSION), "Anwesen"),
		"allay",
		new StructureLocate.Kind(List.of(BuiltinStructures.PILLAGER_OUTPOST, BuiltinStructures.WOODLAND_MANSION), "Allay-Versteck")
	);
	private static final int RADIUS = 100;

	static GuideNet.StructureFound locate(ServerPlayer p, String kindId) {
		GuideNet.StructureFound none = new GuideNet.StructureFound(kindId, false, BlockPos.ZERO);
		StructureLocate.Kind kind = KINDS.get(kindId);
		if (kind == null) {
			return none;
		} else {
			ServerLevel level = p.level();
			Registry<Structure> registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
			List<Holder<Structure>> holders = kind.keys().stream().flatMap(k -> registry.get(k).stream()).<Holder<Structure>>map(h -> h).toList();
			if (holders.isEmpty()) {
				return none;
			} else {
				Pair<BlockPos, Holder<Structure>> hit = level.getChunkSource()
					.getGenerator()
					.findNearestMapStructure(level, HolderSet.direct(holders), p.blockPosition(), 100, false);
				return hit == null ? none : new GuideNet.StructureFound(kindId, true, (BlockPos)hit.getFirst());
			}
		}
	}

	private StructureLocate() {
	}

	record Kind(List<ResourceKey<Structure>> keys, String label) {
	}
}
