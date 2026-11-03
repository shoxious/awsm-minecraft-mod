package de.shoxi.mcguide;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

/**
 * Serverseite des Loot-Finders: nur der Server kennt die Loot-Tabelle eines Behaelters.
 * Sie ist nur bei generierten, noch nicht geoeffneten Kisten gesetzt – von Spielern platzierte haben keine.
 */
final class LootScan {
	static final int CHUNK_RADIUS = 5;

	static GuideNet.LootSpots scan(ServerPlayer p) {
		ServerLevel level = p.level();
		List<BlockPos> blocks = new ArrayList<>();
		int cx = p.blockPosition().getX() >> 4;
		int cz = p.blockPosition().getZ() >> 4;
		for (int x = cx - CHUNK_RADIUS; x <= cx + CHUNK_RADIUS; x++) {
			for (int z = cz - CHUNK_RADIUS; z <= cz + CHUNK_RADIUS; z++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
				if (chunk == null) continue;
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					if (be instanceof RandomizableContainer rc && rc.getLootTable() != null) blocks.add(be.getBlockPos());
				}
			}
		}
		List<Integer> entities = new ArrayList<>();
		AABB box = p.getBoundingBox().inflate((CHUNK_RADIUS + 1) * 16);
		for (Entity e : level.getEntities((Entity) null, box, e -> e instanceof ContainerEntity ce && ce.getContainerLootTable() != null)) {
			entities.add(e.getId());
		}
		return new GuideNet.LootSpots(blocks, entities);
	}

	private LootScan() {}
}
