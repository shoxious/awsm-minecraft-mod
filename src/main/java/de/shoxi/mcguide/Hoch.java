package de.shoxi.mcguide;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Bauplan-Schritte fuer Arbeiten in der Hoehe.
 * <ul>
 * <li>{@code saeule}: auf der Stelle mit Bruchstein hochbauen, bis die Fuesse auf pos.y sind; an jeden Saeulenblock
 *     kommt auf der Seite {@code face} eine Leiter.</li>
 * <li>{@code bruecke}: geschlichen (kein Absturz ueber Kanten) count Bloecke in Richtung face gehen; fehlt vorne der
 *     Boden, wird erst ein Bruchstein gesetzt.</li>
 * <li>{@code runter}: einen Block in Richtung face in die Leiterspalte treten und hinunterrutschen bis pos.y.</li>
 * </ul>
 */
final class Hoch {
	static String error = "";
	private static int phase, wait, done;
	private static BlockPos goal;

	static void reset() {
		phase = wait = done = 0;
		goal = null;
		digging = null;
	}

	private static BlockPos feet(Minecraft mc) {
		var p = mc.player;
		return BlockPos.containing(p.getX(), Math.floor(p.getY() + 0.01), p.getZ());
	}

	private static float yaw(Direction d) {
		return switch (d) {
			case EAST -> -90f;
			case WEST -> 90f;
			case NORTH -> 180f;
			default -> 0f;
		};
	}

	private static void click(Minecraft mc, BlockPos block, Direction face) {
		Vec3 hit = Vec3.atCenterOf(block).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
		mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, block, false));
	}

	private static void keysOff(Minecraft mc) {
		mc.options.keyJump.setDown(false);
		mc.options.keyUp.setDown(false);
		mc.options.keyShift.setDown(false);
	}

	private static TreeJob.Result fail(Minecraft mc, String why) {
		keysOff(mc);
		error = why;
		return TreeJob.Result.STOP;
	}

	// ---------- saeule ----------

	static TreeJob.Result column(Minecraft mc, int cx, int cz, int topY, Direction side, String item) {
		var p = mc.player;
		mc.options.keyShift.setDown(false);
		// erst genau in die Mitte der Saeule stellen
		double dx = cx + 0.5 - p.getX(), dz = cz + 0.5 - p.getZ();
		if (p.onGround() && dx * dx + dz * dz > 0.03 && p.getBlockY() < topY) {
			p.setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
			mc.options.keyShift.setDown(true);
			mc.options.keyUp.setDown(true);
			if (++wait > 2400) return fail(mc, "Säule: komme nicht in die Mitte");
			return TreeJob.Result.RUNNING;
		}
		mc.options.keyUp.setDown(false);
		if (++wait > 2400) return fail(mc, "Säule: dauert zu lange");
		BlockPos feet = feet(mc);
		if (p.onGround()) {
			mc.options.keyJump.setDown(false);
			BlockPos below = feet.below();
			BlockPos ladder = side == null ? below : below.relative(side);
			if (side != null && mc.level.getBlockState(ladder).canBeReplaced() && !mc.level.getBlockState(below).isAir()
				&& !mc.level.getBlockState(below.below()).isAir()) {
				// Leiter an die Saeule (nur ueber dem Boden: dort ist below.below() wieder Saeule)
				if (phase == 1 && ++done < 6) return TreeJob.Result.RUNNING;
				int s = Hotbar.bring(mc, st -> st.is(Items.LADDER));
				if (s < 0) return fail(mc, "keine Leitern mehr");
				p.getInventory().setSelectedSlot(s);
				click(mc, below, side);
				phase = 1;
				done = 0;
				return TreeJob.Result.RUNNING;
			}
			phase = 0;
			if (feet.getY() >= topY) {
				keysOff(mc);
				return TreeJob.Result.DONE;
			}
		}
		int c = Hotbar.bring(mc, st -> item == null ? st.is(Items.COBBLESTONE)
			: net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(st.getItem()).toString().equals(item));
		if (c < 0) return fail(mc, "kein " + (item == null ? "Bruchstein" : item) + " mehr");
		p.getInventory().setSelectedSlot(c);
		p.setXRot(90);
		if (p.onGround()) {
			mc.options.keyJump.setDown(true);
			return TreeJob.Result.RUNNING;
		}
		mc.options.keyJump.setDown(false);
		// erst setzen, wenn die Fuesse ueber der Oberkante des Zielblocks sind (sonst steckt er im eigenen Koerper)
		BlockPos ground = BlockPos.containing(p.getX(), p.getY() - 1.01, p.getZ());
		if (p.getY() - ground.getY() > 1.05 && mc.level.getBlockState(ground).canBeReplaced() && !mc.level.getBlockState(ground.below()).isAir()) {
			click(mc, ground.below(), Direction.UP);
		}
		return TreeJob.Result.RUNNING;
	}

	// ---------- bruecke ----------

	static TreeJob.Result bridge(Minecraft mc, Direction dir, int count) {
		var p = mc.player;
		mc.options.keyShift.setDown(true);
		p.setYRot(yaw(dir));
		if (++wait > 200) return fail(mc, "Brücke: komme nicht weiter");
		if (goal == null) {
			if (done >= count) {
				keysOff(mc);
				return TreeJob.Result.DONE;
			}
			if (!p.onGround()) return TreeJob.Result.RUNNING;
			BlockPos f = feet(mc);
			BlockPos floor = f.below().relative(dir);
			if (!mc.level.getBlockState(f.relative(dir)).isAir() || !mc.level.getBlockState(f.relative(dir).above()).isAir())
				return fail(mc, "Brücke: Weg versperrt bei " + f.relative(dir).toShortString());
			if (mc.level.getBlockState(floor).isAir()) {
				if (phase == 0) {
					int c = Hotbar.bring(mc, st -> st.is(Items.COBBLESTONE));
					if (c < 0) return fail(mc, "kein Bruchstein mehr");
					p.getInventory().setSelectedSlot(c);
					p.setXRot(75);
					click(mc, f.below(), dir);
					phase = 1;
					wait = 0;
				} else if (wait > 10) {
					return fail(mc, "Brücke: Block bei " + floor.toShortString() + " ließ sich nicht setzen");
				}
				return TreeJob.Result.RUNNING;
			}
			phase = 0;
			goal = f.relative(dir);
			wait = 0;
		}
		Vec3 c = Vec3.atBottomCenterOf(goal);
		double along = (c.x - p.getX()) * dir.getStepX() + (c.z - p.getZ()) * dir.getStepZ();
		if (along < 0.2) {
			mc.options.keyUp.setDown(false);
			if (Math.abs(p.getDeltaMovement().horizontalDistance()) < 0.01) {
				goal = null;
				done++;
				wait = 0;
			}
		} else {
			mc.options.keyUp.setDown(true);
		}
		return TreeJob.Result.RUNNING;
	}

	// ---------- runter ----------

	static TreeJob.Result down(Minecraft mc, Direction dir, int groundY) {
		var p = mc.player;
		mc.options.keyShift.setDown(false);
		mc.options.keyJump.setDown(false);
		p.setYRot(yaw(dir));
		p.setXRot(0);
		if (++wait > 1200) return fail(mc, "Runter: dauert zu lange");
		if (goal == null) goal = feet(mc).relative(dir);
		boolean inColumn = p.getBlockX() == goal.getX() && p.getBlockZ() == goal.getZ();
		if (!inColumn) {
			mc.options.keyUp.setDown(true);
			return TreeJob.Result.RUNNING;
		}
		mc.options.keyUp.setDown(false);
		if (p.onGround() && p.getY() <= groundY + 0.1) {
			keysOff(mc);
			return TreeJob.Result.DONE;
		}
		return TreeJob.Result.RUNNING;
	}

	// ---------- abtragen ----------

	private static BlockPos digging;

	/** Den Block unter den Fuessen abbauen, bis die Fuesse auf groundY sind (eigene Hilfssaeule abtragen). */
	static TreeJob.Result digDown(Minecraft mc, int groundY) {
		var p = mc.player;
		keysOff(mc);
		if (++wait > 2400) return fail(mc, "Abtragen: dauert zu lange");
		if (p.onGround() && p.getBlockY() <= groundY) {
			if (digging != null) mc.gameMode.stopDestroyBlock();
			digging = null;
			return TreeJob.Result.DONE;
		}
		if (!p.onGround()) return TreeJob.Result.RUNNING;
		BlockPos under = p.getOnPos(); // der Block, auf dem die Figur wirklich steht (auch am Rand)
		if (mc.level.getBlockState(under).isAir()) return TreeJob.Result.RUNNING;
		// nur eigene Erdsaeulen abtragen, nie Gebautes
		var ub = mc.level.getBlockState(under);
		if (!(ub.is(net.minecraft.world.level.block.Blocks.DIRT) || ub.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK))) {
			if (digging != null) mc.gameMode.stopDestroyBlock();
			digging = null;
			return TreeJob.Result.DONE;
		}
		p.setXRot(90);
		if (digging == null || !digging.equals(under)) {
			if (digging != null) mc.gameMode.stopDestroyBlock();
			var bs = mc.level.getBlockState(under);
			var tag = bs.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL) ? net.minecraft.tags.ItemTags.SHOVELS : net.minecraft.tags.ItemTags.PICKAXES;
			int s = Hotbar.bring(mc, st -> st.is(tag));
			if (s >= 0) p.getInventory().setSelectedSlot(s);
			mc.gameMode.startDestroyBlock(under, Direction.UP);
			digging = under;
		} else {
			mc.gameMode.continueDestroyBlock(under, Direction.UP);
		}
		return TreeJob.Result.RUNNING;
	}
}
