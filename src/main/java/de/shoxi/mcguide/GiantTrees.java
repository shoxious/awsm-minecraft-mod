package de.shoxi.mcguide;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Riesenbaeume: Ein quadratisches Feld gleicher Setzlinge (4x4, 8x8, 16x16, 32x32, ...) laesst einen
 * natuerlich geformten Riesenbaum wachsen, dessen Stamm das Feld ausfuellt: runder, sich verjuengender Stamm
 * mit Wurzelansatz, Aeste nach aussen/oben und runde Blaetterkronen (Fichte als gestufter Kegel).
 */
public final class GiantTrees {
	private static final int MAX_SIZE = 128;
	/** Flags: Clients informieren, keine Nachbar-Formupdates (sonst ruckelt es bei grossen Baeumen stark). */
	private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
	/** Gesetzte Bloecke pro Server-Tick; ein 8x8-Baum steht damit sofort, sehr grosse Baeume in wenigen Sekunden. */
	private static final int BUDGET = 60_000;
	private static final int SHAFT_CELL = 4;

	private enum Shape { BROAD, TALL, WIDE, JUNGLE, CONIFER }

	/** Ein wachsender Riesenbaum: Bloecke in Setz-Reihenfolge (Stamm und Aeste vor den Blaettern). */
	private record Job(ServerLevel level, List<Map.Entry<Long, BlockState>> blocks, int[] index) {}

	private static final Deque<Job> JOBS = new ArrayDeque<>();

	public static boolean tryGrow(ServerLevel level, BlockPos pos, BlockState sapling, TreeGrower grower, RandomSource random) {
		Block block = sapling.getBlock();
		BlockPos corner = null;
		int size = 0;
		// Ausdehnung der Setzlinge in der Reihe/Spalte begrenzt die moeglichen Quadrate
		int west = run(level, pos, block, -1, 0), east = run(level, pos, block, 1, 0);
		int north = run(level, pos, block, 0, -1), south = run(level, pos, block, 0, 1);
		search:
		for (int k = Integer.highestOneBit(Math.min(Math.min(west + east + 1, north + south + 1), MAX_SIZE)); k >= 4; k /= 2) {
			for (int x0 = pos.getX() - Math.min(west, k - 1); x0 <= pos.getX() + east - (k - 1); x0++) {
				if (x0 > pos.getX()) break;
				for (int z0 = pos.getZ() - Math.min(north, k - 1); z0 <= pos.getZ() + south - (k - 1); z0++) {
					if (z0 > pos.getZ()) break;
					BlockPos c = new BlockPos(x0, pos.getY(), z0);
					if (isSquare(level, c, k, block)) {
						corner = c;
						size = k;
						break search;
					}
				}
			}
		}
		if (corner == null) return false;

		BlockState log = woodBlock(block, "_log", Blocks.OAK_LOG);
		BlockState leaves = woodBlock(block, "_leaves", Blocks.OAK_LEAVES);

		for (int x = 0; x < size; x++)
			for (int z = 0; z < size; z++)
				level.setBlock(corner.offset(x, 0, z), Blocks.AIR.defaultBlockState(), FLAGS);

		Builder b = new Builder(level, log, leaves, RandomSource.create(corner.asLong() ^ level.getSeed()));
		double cx = corner.getX() + size / 2.0, cz = corner.getZ() + size / 2.0;
		b.tree(shapeOf(block), cx, corner.getY(), cz, size);
		b.carveLightShafts();
		b.computeLeafDistances();

		List<Map.Entry<Long, BlockState>> list = new ArrayList<>(b.logs.entrySet());
		list.addAll(b.leaves.entrySet());
		JOBS.add(new Job(level, list, new int[1]));
		GuideServer.LOG.info("Riesenbaum {}x{} ({}) bei {} mit {} Bloecken", size, size, BuiltInRegistries.BLOCK.getKey(block).getPath(), corner, list.size());
		return true;
	}

	/** Setzt pro Tick einen Teil der wartenden Riesenbaeume. */
	public static void tick(MinecraftServer server) {
		int budget = BUDGET;
		BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
		while (budget > 0 && !JOBS.isEmpty()) {
			Job j = JOBS.peek();
			while (budget > 0 && j.index()[0] < j.blocks().size()) {
				Map.Entry<Long, BlockState> e = j.blocks().get(j.index()[0]++);
				m.set(e.getKey());
				BlockState cur = j.level().getBlockState(m);
				if (cur.isAir() || cur.canBeReplaced() || cur.is(BlockTags.LEAVES) || cur.is(BlockTags.SAPLINGS)
					|| (cur.is(BlockTags.LOGS) && e.getValue().is(BlockTags.LOGS))) {
					j.level().setBlock(m, e.getValue(), FLAGS);
				}
				budget--;
			}
			if (j.index()[0] >= j.blocks().size()) JOBS.poll();
		}
	}

	/** Erzeugt die Bloecke eines Riesenbaums. Logs und Blaetter getrennt, Logs gewinnen. */
	private static final class Builder {
		final ServerLevel level;
		final BlockState log, leaf;
		final RandomSource rnd;
		final int minY, maxY;
		final Map<Long, BlockState> logs = new LinkedHashMap<>();
		final Map<Long, BlockState> leaves = new LinkedHashMap<>();

		Builder(ServerLevel level, BlockState log, BlockState leaf, RandomSource rnd) {
			this.level = level;
			this.log = log;
			this.leaf = leaf;
			this.rnd = rnd;
			this.minY = level.getMinY();
			this.maxY = level.getMaxY();
		}

		void tree(Shape shape, double cx, int y0, double cz, int size) {
			double r0 = size / 2.0;
			switch (shape) {
				case CONIFER -> conifer(cx, y0, cz, r0, (int) (size * 7.5));
				case TALL -> broadleaf(cx, y0, cz, r0, (int) (size * 6.5), 0.62, 0.22, 0.75, 3 + size / 3);
				case WIDE -> broadleaf(cx, y0, cz, r0, (int) (size * 3.6), 0.45, 0.5, 1.2, 6 + size / 2);
				case JUNGLE -> broadleaf(cx, y0, cz, r0, (int) (size * 8.5), 0.8, 0.22, 1.0, 4 + size / 3);
				default -> broadleaf(cx, y0, cz, r0, (int) (size * 5.0), 0.5, 0.35, 1.0, 5 + size / 2);
			}
		}

		/** Laubbaum: Stamm bis trunkFrac*H, Aeste ab branchStart, Kronen an den Astenden und oben. */
		void broadleaf(double cx, int y0, double cz, double r0, int h, double branchStart, double branchLen, double crown, int branches) {
			int trunkTop = (int) (h * 0.85);
			trunk(cx, y0, cz, r0, trunkTop, 0.45);
			sprouts(cx, y0, cz, r0, trunkTop, 0.45);
			double blob = (3.5 + r0 * 0.9) * crown;
			for (int i = 0; i < branches; i++) {
				double t = branchStart + (0.85 - branchStart) * (i + rnd.nextDouble()) / branches;
				double yaw = rnd.nextDouble() * Math.PI * 2;
				double pitch = Math.toRadians(20 + rnd.nextDouble() * 30);
				double len = h * branchLen * (0.75 + rnd.nextDouble() * 0.5);
				double sy = y0 + h * t;
				double rStart = Math.max(1.0, r0 * (1 - 0.45 * t) * 0.45);
				double ex = cx + Math.cos(yaw) * Math.cos(pitch) * len;
				double ey = sy + Math.sin(pitch) * len;
				double ez = cz + Math.sin(yaw) * Math.cos(pitch) * len;
				limb(cx, sy, cz, ex, ey, ez, rStart, 0.6);
				leafBlob(ex, ey + blob * 0.3, ez, blob * (0.8 + rnd.nextDouble() * 0.4));
				// kleinere Blaetterbuesche entlang des Asts
				double mid = 0.55 + rnd.nextDouble() * 0.2;
				leafBlob(cx + (ex - cx) * mid, sy + (ey - sy) * mid + blob * 0.4, cz + (ez - cz) * mid, blob * 0.6);
			}
			leafBlob(cx, y0 + trunkTop + blob * 0.2, cz, blob * 1.35);
		}

		/** Fichte: durchgehender Stamm bis zur Spitze, gestufter Kegel aus Blaettern. */
		void conifer(double cx, int y0, double cz, double r0, int h) {
			trunk(cx, y0, cz, r0, h, 0.92);
			sprouts(cx, y0, cz, r0, h, 0.92);
			double rMax = r0 * 2.4 + 4;
			int start = (int) (h * 0.18);
			for (int dy = start; dy <= h + 2; dy++) {
				double t = (dy - start) / (double) (h + 2 - start);
				double r = rMax * (1 - t);
				// Stufen: alle paar Bloecke springt der Radius wieder nach aussen
				int tier = Math.max(3, (int) (r0 * 0.75));
				double inTier = (dy % tier) / (double) tier;
				r *= 0.65 + 0.35 * (1 - inTier);
				if (dy % tier == 0 && r > 5) {
					int spokes = 6 + (int) (r / 3);
					double rot = rnd.nextDouble();
					for (int i = 0; i < spokes; i++) {
						double yaw = (i + rot) * Math.PI * 2 / spokes;
						limb(cx, y0 + dy, cz, cx + Math.cos(yaw) * (r - 1.5), y0 + dy - 1, cz + Math.sin(yaw) * (r - 1.5), Math.max(0.5, r0 * 0.15), 0.5);
					}
				}
				disc(cx, y0 + dy, cz, Math.max(1.2, r));
			}
		}

		/**
		 * Blaetterbueschel am unteren Stamm (wie Wasserreiser). Der Tree-Harvester-Mod erkennt einen Baum nur,
		 * wenn innerhalb von 30 Bloecken ueber dem Stammfuss und 2 Bloecken seitlich natuerliche Blaetter sind.
		 */
		void sprouts(double cx, int y0, double cz, double r0, int height, double taper) {
			RandomSource rnd = RandomSource.create(BlockPos.asLong((int) Math.floor(cx), y0, (int) Math.floor(cz)) * 31 + 7);
			for (int ring = 5; ring <= Math.min(27, height - 2); ring += 5) {
				int yRing = y0 + ring + rnd.nextInt(2);
				double r = r0 * (1 - taper * (ring / (double) height));
				int ri = (int) Math.ceil(r + 2);
				for (int dy = 0; dy < 3; dy++) {
					for (int ix = -ri; ix <= ri; ix++) {
						for (int iz = -ri; iz <= ri; iz++) {
							double px = Math.floor(cx) + ix + 0.5 - cx, pz = Math.floor(cz) + iz + 0.5 - cz;
							double d = Math.sqrt(px * px + pz * pz);
							// schmales, lueckenhaftes Band direkt an der Rinde, oben duenner
							if (d <= r || d > r + 1.8 - dy * 0.4) continue;
							if (rnd.nextFloat() < 0.25 + dy * 0.15) continue;
							putLeaf((int) Math.floor(cx) + ix, yRing + dy, (int) Math.floor(cz) + iz);
						}
					}
				}
			}
		}

		/** Runder Stamm mit Wurzelansatz, verjuengt sich bis taper*r0 kleiner. */
		void trunk(double cx, int y0, double cz, double r0, int height, double taper) {
			for (int dy = -2; dy < height; dy++) {
				double t = Math.max(0, dy) / (double) height;
				double r = r0 * (1 - taper * t);
				double flare = dy < r0 ? (r0 - dy) * 0.35 : 0; // Wurzelansatz
				fillCircle(cx, y0 + dy, cz, Math.max(0.7, r + flare));
			}
		}

		/** Ast als Linie aus Kugeln, Radius nimmt zum Ende ab. */
		void limb(double x1, double y1, double z1, double x2, double y2, double z2, double r1, double r2) {
			double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
			double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
			Direction.Axis axis = Math.abs(dy) > Math.max(Math.abs(dx), Math.abs(dz)) ? Direction.Axis.Y
				: Math.abs(dx) > Math.abs(dz) ? Direction.Axis.X : Direction.Axis.Z;
			for (double s = 0; s <= len; s += 0.5) {
				double f = s / len;
				ball(x1 + dx * f, y1 + dy * f, z1 + dz * f, r1 + (r2 - r1) * f, axis);
			}
		}

		void ball(double x, double y, double z, double r, Direction.Axis axis) {
			int ri = (int) Math.ceil(r);
			for (int ix = -ri; ix <= ri; ix++)
				for (int iy = -ri; iy <= ri; iy++)
					for (int iz = -ri; iz <= ri; iz++)
						if (ix * ix + iy * iy + iz * iz <= r * r + 0.3) putLog((int) Math.floor(x) + ix, (int) Math.floor(y) + iy, (int) Math.floor(z) + iz, axis);
		}

		/** Unregelmaessige Blaetterkugel (abgeflacht, ausgefranster Rand). */
		void leafBlob(double x, double y, double z, double r) {
			// kleine Zweige im Inneren halten die Blaetter in Reichweite (max. 6 Bloecke vom Holz)
			if (r > 4) {
				int spokes = 4 + (int) (r / 3);
				for (int i = 0; i < spokes; i++) {
					double yaw = rnd.nextDouble() * Math.PI * 2, pitch = (rnd.nextDouble() - 0.3) * Math.PI * 0.5;
					double len = r * (0.55 + rnd.nextDouble() * 0.2);
					limb(x, y, z, x + Math.cos(yaw) * Math.cos(pitch) * len, y + Math.sin(pitch) * len * 0.55,
						z + Math.sin(yaw) * Math.cos(pitch) * len, 0.5, 0.5);
				}
			}
			int ri = (int) Math.ceil(r);
			for (int ix = -ri; ix <= ri; ix++) {
				for (int iy = -ri; iy <= ri; iy++) {
					for (int iz = -ri; iz <= ri; iz++) {
						double d = (ix * ix + iz * iz) / (r * r) + (iy * iy) / (r * r * 0.55);
						if (d > 1) continue;
						if (d > 0.75 && rnd.nextFloat() < 0.35) continue;
						putLeaf((int) Math.floor(x) + ix, (int) Math.floor(y) + iy, (int) Math.floor(z) + iz);
					}
				}
			}
		}

		void disc(double cx, int y, double cz, double r) {
			int ri = (int) Math.ceil(r);
			for (int ix = -ri; ix <= ri; ix++) {
				for (int iz = -ri; iz <= ri; iz++) {
					double d = (ix * ix + iz * iz) / (r * r);
					if (d > 1) continue;
					if (d > 0.7 && rnd.nextFloat() < 0.3) continue;
					putLeaf((int) Math.floor(cx) + ix, y, (int) Math.floor(cz) + iz);
				}
			}
		}

		void fillCircle(double cx, int y, double cz, double r) {
			int ri = (int) Math.ceil(r);
			for (int ix = -ri; ix <= ri; ix++)
				for (int iz = -ri; iz <= ri; iz++) {
					double px = Math.floor(cx) + ix + 0.5 - cx, pz = Math.floor(cz) + iz + 0.5 - cz;
					if (px * px + pz * pz <= r * r) putLog((int) Math.floor(cx) + ix, y, (int) Math.floor(cz) + iz, Direction.Axis.Y);
				}
		}

		/** Senkrechte Luecken in der Krone, damit tagsueber genug Sonne auf den Boden faellt. */
		void carveLightShafts() {
			leaves.keySet().removeIf(k -> isShaft(BlockPos.getX(k), BlockPos.getZ(k)));
		}

		/** Natuerliche Blaetter: DISTANCE wie im Spiel (1-6 zum naechsten Holz), weiter entfernte fallen weg. */
		void computeLeafDistances() {
			Map<Long, Integer> dist = new java.util.HashMap<>();
			java.util.ArrayDeque<Long> queue = new java.util.ArrayDeque<>();
			for (long k : leaves.keySet()) {
				for (Direction d : Direction.values()) {
					if (logs.containsKey(BlockPos.offset(k, d))) {
						dist.put(k, 1);
						queue.add(k);
						break;
					}
				}
			}
			while (!queue.isEmpty()) {
				long k = queue.poll();
				int dk = dist.get(k);
				if (dk >= 6) continue;
				for (Direction d : Direction.values()) {
					long n = BlockPos.offset(k, d);
					if (leaves.containsKey(n) && !dist.containsKey(n)) {
						dist.put(n, dk + 1);
						queue.add(n);
					}
				}
			}
			leaves.entrySet().removeIf(e -> !dist.containsKey(e.getKey()));
			for (Map.Entry<Long, BlockState> e : leaves.entrySet()) {
				BlockState st = e.getValue();
				if (st.hasProperty(LeavesBlock.DISTANCE)) e.setValue(st.setValue(LeavesBlock.DISTANCE, dist.get(e.getKey())));
			}
		}

		void putLog(int x, int y, int z, Direction.Axis axis) {
			if (y < minY || y > maxY) return;
			long k = BlockPos.asLong(x, y, z);
			if (logs.containsKey(k)) return;
			BlockState st = log.hasProperty(RotatedPillarBlock.AXIS) ? log.setValue(RotatedPillarBlock.AXIS, axis) : log;
			logs.put(k, st);
			leaves.remove(k);
		}

		void putLeaf(int x, int y, int z) {
			if (y < minY || y > maxY) return;
			long k = BlockPos.asLong(x, y, z);
			if (!logs.containsKey(k)) leaves.putIfAbsent(k, leaf);
		}
	}

	/** Eine Lichtluecke pro 4x4-Zelle, zufaellig aber fest pro Zelle verteilt. */
	static boolean isShaft(int x, int z) {
		int cx = Math.floorDiv(x, SHAFT_CELL), cz = Math.floorDiv(z, SHAFT_CELL);
		long h = cx * 341873128712L + cz * 132897987541L;
		h ^= h >>> 29;
		h *= 0x9E3779B97F4A7C15L;
		h ^= h >>> 32;
		return Math.floorMod(x, SHAFT_CELL) == (int) Math.floorMod(h, SHAFT_CELL)
			&& Math.floorMod(z, SHAFT_CELL) == (int) Math.floorMod(h >>> 8, SHAFT_CELL);
	}

	/** Lichtet die Krone eines bestehenden Riesenbaums aus (Blaetter seiner Holzart in den Lichtluecken entfernen). */
	public static int carveExisting(ServerLevel level, BlockPos corner, int size) {
		BlockState log = level.getBlockState(corner.offset(size / 2, 1, size / 2));
		if (!log.is(BlockTags.LOGS)) return -1;
		String wood = BuiltInRegistries.BLOCK.getKey(log.getBlock()).getPath().replace("_log", "");
		Block leafBlock = BuiltInRegistries.BLOCK.getOptional(Identifier.withDefaultNamespace(wood + "_leaves")).orElse(Blocks.OAK_LEAVES);
		int r = size * 4 + 8, top = Math.min(level.getMaxY(), corner.getY() + size * 9);
		int cx = corner.getX() + size / 2, cz = corner.getZ() + size / 2;
		BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
		int removed = 0;
		for (int x = cx - r; x <= cx + r; x++) {
			for (int z = cz - r; z <= cz + r; z++) {
				if (!isShaft(x, z)) continue;
				for (int y = corner.getY(); y <= top; y++) {
					BlockState st = level.getBlockState(m.set(x, y, z));
					if (st.is(leafBlock) && !st.getValue(LeavesBlock.PERSISTENT)) {
						level.setBlock(m, Blocks.AIR.defaultBlockState(), FLAGS);
						removed++;
					}
				}
			}
		}
		return removed;
	}

	/**
	 * Fuegt einem bestehenden Riesenbaum (aelter als 1.4.2) die Stammbueschel hinzu.
	 * corner = Nordwest-Ecke des frueheren Setzlingsfelds, size = Feldgroesse.
	 */
	public static int addSprouts(ServerLevel level, BlockPos corner, int size) {
		BlockState log = level.getBlockState(corner.offset(size / 2, 1, size / 2));
		if (!log.is(BlockTags.LOGS)) return -1;
		String wood = BuiltInRegistries.BLOCK.getKey(log.getBlock()).getPath().replace("_log", "");
		Block sapling = BuiltInRegistries.BLOCK.getOptional(Identifier.withDefaultNamespace(wood + "_sapling")).orElse(Blocks.OAK_SAPLING);
		BlockState leaves = woodBlock(sapling, "_leaves", Blocks.OAK_LEAVES);
		Builder b = new Builder(level, log, leaves, RandomSource.create(0));
		double cx = corner.getX() + size / 2.0, cz = corner.getZ() + size / 2.0;
		Shape shape = shapeOf(sapling);
		int h = switch (shape) {
			case CONIFER -> (int) (size * 7.5);
			case TALL -> (int) (size * 6.5 * 0.85);
			case WIDE -> (int) (size * 3.6 * 0.85);
			case JUNGLE -> (int) (size * 8.5 * 0.85);
			default -> (int) (size * 5.0 * 0.85);
		};
		b.sprouts(cx, corner.getY(), cz, size / 2.0, h, shape == Shape.CONIFER ? 0.92 : 0.45);
		// Bueschel brauchen Holz direkt daneben (DISTANCE 1)
		BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(), n = new BlockPos.MutableBlockPos();
		int placed = 0;
		for (Map.Entry<Long, BlockState> e : b.leaves.entrySet()) {
			m.set(e.getKey());
			if (!level.getBlockState(m).isAir()) continue;
			boolean nearLog = false;
			for (Direction d : Direction.values()) nearLog |= level.getBlockState(n.setWithOffset(m, d)).is(BlockTags.LOGS);
			if (!nearLog) continue;
			BlockState st = e.getValue();
			if (st.hasProperty(LeavesBlock.DISTANCE)) st = st.setValue(LeavesBlock.DISTANCE, 1);
			level.setBlock(m, st, FLAGS);
			placed++;
		}
		return placed;
	}

	private static Shape shapeOf(Block sapling) {
		if (sapling == Blocks.SPRUCE_SAPLING) return Shape.CONIFER;
		if (sapling == Blocks.BIRCH_SAPLING || sapling == Blocks.POPLAR_SAPLING) return Shape.TALL;
		if (sapling == Blocks.DARK_OAK_SAPLING || sapling == Blocks.PALE_OAK_SAPLING || sapling == Blocks.ACACIA_SAPLING) return Shape.WIDE;
		if (sapling == Blocks.JUNGLE_SAPLING) return Shape.JUNGLE;
		return Shape.BROAD;
	}

	/** "oak_sapling" → "oak_log"/"oak_leaves", "mangrove_propagule" → "mangrove_...". */
	private static BlockState woodBlock(Block sapling, String suffix, Block fallback) {
		Identifier id = BuiltInRegistries.BLOCK.getKey(sapling);
		String wood = id.getPath().replace("_sapling", "").replace("_propagule", "");
		return BuiltInRegistries.BLOCK.getOptional(id.withPath(wood + suffix)).orElse(fallback).defaultBlockState();
	}

	/** Anzahl gleicher Setzlinge in Richtung (dx, dz), ohne den Startblock. */
	private static int run(ServerLevel level, BlockPos pos, Block block, int dx, int dz) {
		int n = 0;
		while (n < MAX_SIZE && level.getBlockState(pos.offset(dx * (n + 1), 0, dz * (n + 1))).is(block)) n++;
		return n;
	}

	private static boolean isSquare(ServerLevel level, BlockPos corner, int k, Block block) {
		for (int x = 0; x < k; x++)
			for (int z = 0; z < k; z++)
				if (!level.getBlockState(corner.offset(x, 0, z)).is(block)) return false;
		return true;
	}

	private GiantTrees() {}
}
