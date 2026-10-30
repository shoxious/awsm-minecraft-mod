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

/** Server-Seite des Loot-Finders: ungeöffnete Container (mit Loot-Tabelle) rund um den Spieler. */
final class LootScan {
	static final int CHUNK_RADIUS = 5;

	static GuideNet.LootSpots scan(ServerPlayer p) {
		ServerLevel level = p.level();
		List<BlockPos> blocks = new ArrayList<>();
		int cx = p.blockPosition().getX() >> 4;
		int cz = p.blockPosition().getZ() >> 4;

		for (int x = cx - 5; x <= cx + 5; x++) {
			for (int z = cz - 5; z <= cz + 5; z++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
				if (chunk != null) {
					for (BlockEntity be : chunk.getBlockEntities().values()) {
						if (be instanceof RandomizableContainer rc && rc.getLootTable() != null) {
							blocks.add(be.getBlockPos());
						}
					}
				}
			}
		}

		List<Integer> entities = new ArrayList<>();
		AABB box = p.getBoundingBox().inflate(96.0);

		for (Entity e : level.getEntities((Entity)null, box, ex -> ex instanceof ContainerEntity ce && ce.getContainerLootTable() != null)) {
			entities.add(e.getId());
		}

		return new GuideNet.LootSpots(blocks, entities);
	}

	private LootScan() {
	}
}
