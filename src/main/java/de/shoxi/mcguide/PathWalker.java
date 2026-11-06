package de.shoxi.mcguide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Wegsuche fuer den Bauplan-Schritt "lauf": A* ueber die geladenen Bloecke (Treppen, Stufen, Leitern,
 * Fallhoehe bis 3), Holztueren und Zauntore werden geoeffnet (Druckplatten duerfen zuerst), Eisentueren,
 * Lava, Feuer, Kakteen usw. werden gemieden. Laeuft ohne Sprinten und springt nur, wenn es bergauf geht.
 */
final class PathWalker {
	enum Result { RUNNING, DONE, FAIL }

	private static List<BlockPos> path;
	private static int idx;
	private static BlockPos goal;
	private static int stuck, replans, doorWait;
	private static BlockPos leaf;
	private static double best;
	private static final List<BlockPos> opened = new ArrayList<>();
	static String error = "";

	static void reset() {
		path = null;
		goal = null;
		opened.clear();
	}

	static Result tick(Minecraft mc, BlockPos target) {
		var p = mc.player;
		Level level = mc.level;
		if (path == null || !target.equals(goal)) {
			goal = target;
			replans = 0;
			opened.clear();
			if (!plan(level, p.position())) return fail(mc, "kein Weg nach " + target.toShortString() + " gefunden");
		}
		closeBehind(mc);
		if (idx >= path.size()) {
			release(mc);
			reset();
			return Result.DONE;
		}
		BlockPos n = path.get(idx);
		Vec3 c = Vec3.atBottomCenterOf(n);
		double dx = c.x - p.getX(), dz = c.z - p.getZ(), h = Math.sqrt(dx * dx + dz * dz);
		double dy = n.getY() - p.getY();
		boolean last = idx == path.size() - 1;
		if (h < (last ? 0.25 : 0.4) && dy > -1.2 && dy < 0.8 && (p.onGround() || climbable(level, n) || p.isInWater())) {
			idx++;
			stuck = 0;
			best = 1e9;
			return Result.RUNNING;
		}
		// Tuer im naechsten Feld: kurz warten (Druckplatte), dann selbst oeffnen
		BlockState ns = level.getBlockState(n);
		if (openable(ns) && !ns.getValue(BlockStateProperties.OPEN) && h < 1.8) {
			release(mc);
			if (++doorWait == 6) {
				look(mc, Vec3.atCenterOf(n));
				mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(n), Direction.getApproximateNearest(-dx, 0, -dz), n, false));
				opened.add(n.immutable());
			}
			if (doorWait > 30) {
				doorWait = 0;
				return fail(mc, "Tür " + n.toShortString() + " geht nicht auf");
			}
			return Result.RUNNING;
		}
		doorWait = 0;
		// Laub im Weg (Fuesse/Kopf): mit der Hand bzw. etwas Unbeschaedigbarem abbauen, nie mit Werkzeug
		BlockPos[] around = n.getY() > p.getBlockY() ? new BlockPos[] {n, n.above(), p.blockPosition().above(2)} : new BlockPos[] {n, n.above()};
		for (BlockPos q : around) {
			if (!level.getBlockState(q).is(BlockTags.LEAVES)) continue;
			release(mc);
			if (!q.equals(leaf)) {
				int slot = -1;
				for (int i = 0; i < 9 && slot < 0; i++) if (!p.getInventory().getItem(i).isDamageableItem()) slot = i;
				if (slot >= 0) p.getInventory().setSelectedSlot(slot);
				look(mc, Vec3.atCenterOf(q));
				mc.gameMode.startDestroyBlock(q, Direction.getApproximateNearest(p.getEyePosition().subtract(Vec3.atCenterOf(q))));
				leaf = q.immutable();
			} else {
				mc.gameMode.continueDestroyBlock(q, Direction.getApproximateNearest(p.getEyePosition().subtract(Vec3.atCenterOf(q))));
			}
			stuck = 0;
			return Result.RUNNING;
		}
		leaf = null;
		// steckengeblieben? neu planen
		double dist = h + Math.abs(dy);
		if (dist < best - 0.05) {
			best = dist;
			stuck = 0;
		} else if (++stuck > 50) {
			release(mc);
			if (++replans > 4 || !plan(level, p.position())) return fail(mc, "komme nicht weiter bei " + p.blockPosition().toShortString());
			return Result.RUNNING;
		}
		mc.options.keyShift.setDown(false);
		mc.options.keySprint.setDown(false);
		p.setSprinting(false);
		boolean ladderUp = dy > 0.5 && h < 0.6 && (climbable(level, n) || climbable(level, p.blockPosition()));
		if (ladderUp) {
			BlockState ls = level.getBlockState(climbable(level, p.blockPosition()) ? p.blockPosition() : n);
			if (ls.getBlock() instanceof LadderBlock) p.setYRot(ls.getValue(LadderBlock.FACING).getOpposite().toYRot());
			p.setXRot(-30);
			mc.options.keyUp.setDown(true);
			mc.options.keyJump.setDown(false);
			return Result.RUNNING;
		}
		if (h > 0.05) p.setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
		p.setXRot(10);
		mc.options.keyUp.setDown(h > 0.08);
		boolean up = dy > 0.6 && h < 1.6;
		mc.options.keyJump.setDown(p.onGround() && (up || (p.horizontalCollision && dy > 0.3)) || (p.isInWater() && dy > -0.5));
		return Result.RUNNING;
	}

	private static Result fail(Minecraft mc, String why) {
		release(mc);
		error = why;
		reset();
		return Result.FAIL;
	}

	/** Holztueren, die wir selbst geoeffnet haben, hinter uns wieder schliessen. */
	private static void closeBehind(Minecraft mc) {
		var it = opened.iterator();
		while (it.hasNext()) {
			BlockPos d = it.next();
			BlockState s = mc.level.getBlockState(d);
			if (!openable(s) || !s.getValue(BlockStateProperties.OPEN)) {
				it.remove();
				continue;
			}
			double far = mc.player.position().distanceTo(Vec3.atBottomCenterOf(d));
			if (far > 1.6 && far < 4 && !mc.player.getBoundingBox().inflate(0.2).intersects(new net.minecraft.world.phys.AABB(d))) {
				mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(d), Direction.UP, d, false));
				it.remove();
			}
		}
	}

	// ---------- A* ----------

	private record Node(BlockPos pos, double g, double f) {}

	private static boolean plan(Level level, Vec3 from) {
		BlockPos start = standNear(level, BlockPos.containing(from.add(0, 0.2, 0)));
		BlockPos end = standNear(level, goal);
		if (start == null || end == null) return false;
		Map<BlockPos, Double> g = new HashMap<>();
		Map<BlockPos, BlockPos> prev = new HashMap<>();
		PriorityQueue<Node> open = new PriorityQueue<>((a, b) -> Double.compare(a.f, b.f));
		g.put(start, 0.0);
		open.add(new Node(start, 0, start.distManhattan(end)));
		int count = 0;
		while (!open.isEmpty() && count++ < 60000) {
			Node cur = open.poll();
			if (cur.g > g.getOrDefault(cur.pos, 1e18)) continue;
			if (cur.pos.equals(end)) {
				List<BlockPos> out = new ArrayList<>();
				for (BlockPos q = end; q != null; q = prev.get(q)) out.add(0, q);
				out.remove(0);
				path = out;
				idx = 0;
				stuck = 0;
				best = 1e9;
				return true;
			}
			for (Edge e : neighbors(level, cur.pos)) {
				double ng = cur.g + e.cost;
				if (ng < g.getOrDefault(e.to, 1e18)) {
					g.put(e.to, ng);
					prev.put(e.to, cur.pos);
					double hx = e.to.getX() - end.getX(), hy = e.to.getY() - end.getY(), hz = e.to.getZ() - end.getZ();
					open.add(new Node(e.to, ng, ng + Math.sqrt(hx * hx + hz * hz) + Math.abs(hy)));
				}
			}
		}
		return false;
	}

	private record Edge(BlockPos to, double cost) {}

	private static List<Edge> neighbors(Level level, BlockPos p) {
		List<Edge> out = new ArrayList<>();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx == 0 && dz == 0) continue;
				boolean diag = dx != 0 && dz != 0;
				BlockPos side = p.offset(dx, 0, dz);
				if (!level.hasChunkAt(side)) continue;
				if (diag) { // nicht um Ecken schneiden
					if (!body(level, p.offset(dx, 0, 0)) || !body(level, p.offset(0, 0, dz))) continue;
					if (leafy(level, p.offset(dx, 0, 0)) || leafy(level, p.offset(0, 0, dz)) || leafy(level, side)) continue;
					if (stand(level, side)) out.add(new Edge(side, 1.42 + penalty(level, side)));
					continue;
				}
				if (stand(level, side)) out.add(new Edge(side, 1 + penalty(level, side)));
				// hoch: Kopf frei zum Springen
				BlockPos upp = side.above();
				// auf Treppen/untere Stufen geht es ohne Sprung hinauf
				BlockState step = level.getBlockState(side);
				boolean lowStep = step.getBlock() instanceof net.minecraft.world.level.block.StairBlock
					|| (step.getBlock() instanceof net.minecraft.world.level.block.SlabBlock && step.getValue(net.minecraft.world.level.block.SlabBlock.TYPE) == net.minecraft.world.level.block.state.properties.SlabType.BOTTOM);
				if (stand(level, upp) && (lowStep || free(level, p.above(2))) && !openable(step)) out.add(new Edge(upp, (lowStep ? 1.2 : 1.6) + penalty(level, upp)));
				// runter: bis 3 Bloecke fallen
				if (body(level, side)) {
					for (int k = 1; k <= 3; k++) {
						BlockPos down = side.below(k);
						if (stand(level, down)) {
							out.add(new Edge(down, 1 + k * 0.6 + penalty(level, down)));
							break;
						}
						if (!free(level, down)) break;
					}
				}
			}
		}
		// Leitern senkrecht
		if (climbable(level, p) || climbable(level, p.above())) {
			if (free(level, p.above()) && (stand(level, p.above()) || climbable(level, p.above()))) out.add(new Edge(p.above(), 1.2));
		}
		if (climbable(level, p.below()) && (stand(level, p.below()) || climbable(level, p.below()))) out.add(new Edge(p.below(), 1.0));
		return out;
	}

	private static boolean leafy(Level level, BlockPos p) {
		return level.getBlockState(p).is(BlockTags.LEAVES) || level.getBlockState(p.above()).is(BlockTags.LEAVES);
	}

	private static double penalty(Level level, BlockPos p) {
		double c = 0;
		if (level.getFluidState(p).is(FluidTags.WATER)) c += 4;
		if (openable(level.getBlockState(p))) c += 1.5;
		if (level.getBlockState(p).is(BlockTags.LEAVES)) c += 3;
		if (level.getBlockState(p.above()).is(BlockTags.LEAVES)) c += 3;
		return c;
	}

	private static BlockPos standNear(Level level, BlockPos p) {
		if (stand(level, p)) return p.immutable();
		for (int r = 1; r <= 2; r++)
			for (int y = -r; y <= r; y++)
				for (int x = -r; x <= r; x++)
					for (int z = -r; z <= r; z++) {
						BlockPos q = p.offset(x, y, z);
						if (stand(level, q)) return q.immutable();
					}
		return null;
	}

	/** Hier kann die Figur stehen: Fuesse + Kopf frei, darunter Halt (oder Leiter). */
	private static boolean stand(Level level, BlockPos p) {
		if (!body(level, p)) return false;
		if (climbable(level, p)) return true;
		BlockState feet = level.getBlockState(p);
		VoxelShape fs = feet.getCollisionShape(level, p);
		if (!fs.isEmpty() && !openable(feet) && !feet.is(BlockTags.LEAVES)) return fs.max(Direction.Axis.Y) <= 0.5 && (fs.max(Direction.Axis.Y) <= 0.2 || free(level, p.above(2)));
		BlockState below = level.getBlockState(p.below());
		if (danger(below)) return false;
		VoxelShape bs = below.getCollisionShape(level, p.below());
		return (!bs.isEmpty() && bs.max(Direction.Axis.Y) > 0.5 && bs.max(Direction.Axis.Y) <= 1.0) || level.getFluidState(p).is(FluidTags.WATER);
	}

	/** Platz fuer den Koerper: Fuesse (darf flach sein) und Kopf. */
	private static boolean body(Level level, BlockPos p) {
		BlockState feet = level.getBlockState(p);
		if (danger(feet) || level.getFluidState(p).is(FluidTags.LAVA)) return false;
		VoxelShape fs = feet.getCollisionShape(level, p);
		boolean feetOk = fs.isEmpty() || openable(feet) || feet.is(BlockTags.LEAVES) || (fs.max(Direction.Axis.Y) <= 0.5);
		return feetOk && free(level, p.above());
	}

	private static boolean free(Level level, BlockPos p) {
		BlockState s = level.getBlockState(p);
		if (danger(s) || level.getFluidState(p).is(FluidTags.LAVA)) return false;
		return s.getCollisionShape(level, p).isEmpty() || openable(s) || s.is(BlockTags.LEAVES);
	}

	private static boolean danger(BlockState s) {
		return s.is(Blocks.LAVA) || s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE) || s.is(Blocks.CACTUS) || s.is(Blocks.MAGMA_BLOCK)
			|| s.is(Blocks.SWEET_BERRY_BUSH) || s.is(Blocks.POWDER_SNOW) || s.is(Blocks.CAMPFIRE) || s.is(Blocks.SOUL_CAMPFIRE)
			|| s.is(Blocks.WITHER_ROSE) || s.is(Blocks.POINTED_DRIPSTONE) || s.is(Blocks.COBWEB);
	}

	private static boolean openable(BlockState s) {
		return (s.getBlock() instanceof DoorBlock && s.is(BlockTags.WOODEN_DOORS)) || s.getBlock() instanceof FenceGateBlock;
	}

	private static boolean climbable(Level level, BlockPos p) {
		return level.getBlockState(p).is(BlockTags.CLIMBABLE);
	}

	private static void look(Minecraft mc, Vec3 target) {
		Vec3 d = target.subtract(mc.player.getEyePosition());
		mc.player.setYRot((float) Math.toDegrees(Math.atan2(-d.x, d.z)));
		mc.player.setXRot((float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z))));
	}

	private static void release(Minecraft mc) {
		mc.options.keyUp.setDown(false);
		mc.options.keyJump.setDown(false);
		mc.options.keyShift.setDown(false);
	}
}
