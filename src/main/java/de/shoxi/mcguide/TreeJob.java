package de.shoxi.mcguide;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Bauplan-Schritte fuer die Holzfarm.
 * <ul>
 * <li>{@code wachsen}: Knochenmehl auf den Setzling bei pos, bis dort ein Stamm steht (hoechstens count Mal, Standard 24).</li>
 * <li>{@code baum}: Baum mit Stammfuss pos faellen. Erst von aussen alles Erreichbare, dann in den Stammplatz stellen und
 *     mit Erde hochbauen, bis die Spitze erreichbar ist; danach das Erdtuermchen wieder abbauen und einen Setzling setzen
 *     (item, Standard Fichtensetzling). Die Axt wird geschont: unter {@link #AXE_MIN} Haltbarkeit bricht der Plan ab.</li>
 * </ul>
 */
final class TreeJob {
	enum Result { RUNNING, DONE, FAIL, STOP }

	static final int AXE_MIN = 300;
	private static final int MAX_HEIGHT = 22;
	static String error = "";

	private static int phase, wait, uses, baseY, approach;
	/** Turm-Modus: Ziel sind Erde/Gras statt Staemme, hochgebaut wird mit Bruchstein. */
	private static boolean towerMode;

	private static boolean target(net.minecraft.world.level.block.state.BlockState s) {
		return towerMode ? (s.is(net.minecraft.world.level.block.Blocks.DIRT) || s.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) : s.is(BlockTags.LOGS);
	}

	/** Uebrig gebliebenen Erdturm in der Saeule ueber pos abtragen (wie Faellen, Hochbauen mit Bruchstein). */
	static Result tower(Minecraft mc, BlockPos base) {
		towerMode = true;
		Result r = fell(mc, base, null);
		if (r != Result.RUNNING) towerMode = false;
		return r;
	}
	private static BlockPos breaking;

	static void reset() {
		phase = wait = uses = approach = 0;
		towerMode = false;
		PathWalker.reset();
		breaking = null;
	}

	// ---------- wachsen ----------

	static Result grow(Minecraft mc, BlockPos pos, Integer max) {
		var level = mc.level;
		var p = mc.player;
		if (level.getBlockState(pos).is(BlockTags.LOGS)) return done();
		if (!level.getBlockState(pos).is(BlockTags.SAPLINGS)) return fail("kein Setzling bei " + pos.toShortString());
		// nie duengen, solange die Figur im kuenftigen Stamm steht – erst zur Seite gehen
		if (inColumn(mc, pos)) {
			if (approach >= 4) return fail("komme nicht vom Setzling " + pos.toShortString() + " weg");
			BlockPos to = switch (approach) { case 0 -> pos.west(); case 1 -> pos.east(); case 2 -> pos.north(); default -> pos.south(); };
			var r = PathWalker.tick(mc, to);
			if (r != PathWalker.Result.RUNNING) approach++;
			return Result.RUNNING;
		}
		if (uses >= (max == null ? 24 : max)) return fail("Setzling " + pos.toShortString() + " wächst nicht (zu dunkel oder zu eng?)");
		if (++wait < 6) return Result.RUNNING;
		wait = 0;
		int slot = Hotbar.bring(mc, s -> s.is(net.minecraft.world.item.Items.BONE_MEAL));
		if (slot < 0) return fail("kein Knochenmehl mehr");
		p.getInventory().setSelectedSlot(slot);
		look(mc, Vec3.atCenterOf(pos));
		mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
		uses++;
		return Result.RUNNING;
	}

	/** Steht die Figur (auch teilweise) in der Saeule ueber pos? */
	static boolean inColumn(Minecraft mc, BlockPos pos) {
		return mc.player.getBoundingBox().intersects(new net.minecraft.world.phys.AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 16, pos.getZ() + 1));
	}

	// ---------- laub ----------

	/** Laub in pos +-r (Hoehe pos..pos+2) mit der Hand abbauen, soweit erreichbar. */
	static Result leaves(Minecraft mc, BlockPos pos, Integer r0) {
		int r = r0 == null ? 1 : r0;
		var level = mc.level;
		if (breaking != null) {
			if (!level.getBlockState(breaking).is(BlockTags.LEAVES) || ++wait > 60) {
				mc.gameMode.stopDestroyBlock();
				breaking = null;
			} else {
				mc.gameMode.continueDestroyBlock(breaking, face(mc, breaking));
				return Result.RUNNING;
			}
		}
		BlockPos best = null;
		double bd = 1e9;
		for (int dy = 0; dy <= 2; dy++)
			for (int dx = -r; dx <= r; dx++)
				for (int dz = -r; dz <= r; dz++) {
					BlockPos q = pos.offset(dx, dy, dz);
					if (!level.getBlockState(q).is(BlockTags.LEAVES) || !mc.player.isWithinBlockInteractionRange(q, -0.3)) continue;
					double d = mc.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(q));
					if (d < bd) { bd = d; best = q; }
				}
		if (best == null) return done();
		int slot = -1;
		for (int i = 0; i < 9 && slot < 0; i++) if (!mc.player.getInventory().getItem(i).isDamageableItem()) slot = i;
		if (slot >= 0) mc.player.getInventory().setSelectedSlot(slot);
		look(mc, Vec3.atCenterOf(best));
		mc.gameMode.startDestroyBlock(best, face(mc, best));
		breaking = best;
		wait = 0;
		return Result.RUNNING;
	}

	// ---------- baum ----------

	static Result fell(Minecraft mc, BlockPos base, String saplingId) {
		var level = mc.level;
		var p = mc.player;
		if (phase == 0) {
			baseY = base.getY();
			phase = highestLog(level, base) == null ? 5 : 1; // kein Baum: nur Tuermchen-Reste wegraeumen
		}
		// Axt pruefen und in die Hand
		if (phase <= 3 && !towerMode) {
			int axe = -1;
			for (int i = 0; i < 36 && axe < 0; i++) if (p.getInventory().getItem(i).is(ItemTags.AXES)) axe = i;
			if (axe < 0) return stop("keine Axt im Inventar");
			ItemStack a = p.getInventory().getItem(axe);
			if (a.getMaxDamage() - a.getDamageValue() < AXE_MIN)
				return stop("Axt hat nur noch " + (a.getMaxDamage() - a.getDamageValue()) + " Haltbarkeit – höre auf, damit sie nicht kaputtgeht");
		}
		BlockPos top = highestLog(level, base);
		switch (phase) {
			case 1 -> { // von aussen abbauen, was erreichbar ist
				if (breaking != null) {
					if (!target(level.getBlockState(breaking)) || ++wait > 200) {
						mc.gameMode.stopDestroyBlock();
						breaking = null;
					} else {
						mc.gameMode.continueDestroyBlock(breaking, face(mc, breaking));
						return Result.RUNNING;
					}
				}
				BlockPos next = lowestReachableLog(mc, base);
				if (next == null) {
					if (top == null) return finishFell(mc);
					Vec3 c = Vec3.atBottomCenterOf(base);
					double hx = c.x - p.getX(), hz = c.z - p.getZ();
					if (hx * hx + hz * hz > 2.6 * 2.6 || target(level.getBlockState(base))) {
						// zu weit weg: selbst hinlaufen (nacheinander W, N, S, O vom Stamm)
						if (approach >= 4) return fail("komme nicht an den Baum " + base.toShortString());
						BlockPos to = switch (approach) { case 0 -> base.west(); case 1 -> base.north(); case 2 -> base.south(); default -> base.east(); };
						var r = PathWalker.tick(mc, to);
						if (r == PathWalker.Result.FAIL) approach++;
						else if (r == PathWalker.Result.DONE && lowestReachableLog(mc, base) == null) approach++;
						return Result.RUNNING;
					}
					phase = 2; // in den Stammplatz stellen
					wait = 0;
					return Result.RUNNING;
				}
				startBreak(mc, next, towerMode ? ItemTags.SHOVELS : ItemTags.AXES);
			}
			case 2 -> { // in den freien Stammplatz laufen
				if (!level.getBlockState(base).isAir() || !level.getBlockState(base.above()).isAir()) {
					// Stammfuss noch da, aber nicht erreichbar: aufgeben
					return fail("Stammfuss " + base.toShortString() + " nicht erreichbar");
				}
				Vec3 c = Vec3.atBottomCenterOf(base);
				double dx = c.x - p.getX(), dz = c.z - p.getZ();
				if (dx * dx + dz * dz < 0.04 && p.onGround()) {
					release(mc);
					phase = 3;
					wait = 0;
					return Result.RUNNING;
				}
				p.setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
				mc.options.keyUp.setDown(true);
				mc.options.keyShift.setDown(true); // vorsichtig, nicht vorbeirutschen
				if (++wait > 200) return fail("komme nicht in den Stammplatz");
			}
			case 3 -> { // im Stamm: abbauen, bei Bedarf mit Erde hochbauen
				mc.options.keyUp.setDown(false);
				mc.options.keyShift.setDown(false);
				if (breaking != null) {
					if (!target(level.getBlockState(breaking)) || ++wait > 200) {
						mc.gameMode.stopDestroyBlock();
						breaking = null;
					} else {
						mc.gameMode.continueDestroyBlock(breaking, face(mc, breaking));
						return Result.RUNNING;
					}
				}
				BlockPos next = lowestReachableLog(mc, base);
				if (next != null) {
					startBreak(mc, next, towerMode ? ItemTags.SHOVELS : ItemTags.AXES);
					return Result.RUNNING;
				}
				if (top == null) {
					phase = 4; // alles weg: Tuermchen abbauen
					wait = 0;
					return Result.RUNNING;
				}
				// Sicherheit: nur im Stammplatz selbst hochbauen, und nie hoeher als bis knapp unter den obersten Block
				if (p.getBlockX() != base.getX() || p.getBlockZ() != base.getZ() || p.getBlockY() - baseY >= MAX_HEIGHT || p.getBlockY() + 2 >= top.getY()) {
					phase = 4;
					wait = 0;
					return Result.RUNNING;
				}
				pillar(mc);
			}
			case 4 -> { // Erdtuermchen unter den Fuessen abbauen bis zum Boden
				mc.options.keyJump.setDown(false);
				if (p.getBlockY() <= baseY && p.onGround()) {
					phase = 5;
					wait = 0;
					return Result.RUNNING;
				}
				BlockPos under = p.blockPosition().below();
				if (under.getY() < baseY) { // nie in den Boden graben
					phase = 5;
					return Result.RUNNING;
				}
				if (breaking == null || !breaking.equals(under)) {
					if (breaking != null) mc.gameMode.stopDestroyBlock();
					startBreak(mc, under, level.getBlockState(under).is(net.minecraft.world.level.block.Blocks.COBBLESTONE) ? ItemTags.PICKAXES : ItemTags.SHOVELS);
				} else if (level.getBlockState(under).isAir() || ++wait > 300) {
					mc.gameMode.stopDestroyBlock();
					breaking = null;
				} else {
					mc.gameMode.continueDestroyBlock(under, Direction.UP);
				}
			}
			default -> { // 5: Erde/Gras, die vom Tuermchen auf dem Stammplatz uebrig sind, wegraeumen
				mc.options.keyJump.setDown(false);
				for (int y = MAX_HEIGHT; y >= 0; y--) {
					BlockPos q = base.above(y);
					if (!mc.player.isWithinBlockInteractionRange(q, -0.3)) continue;
					var bs = level.getBlockState(q);
					if (!(bs.is(net.minecraft.world.level.block.Blocks.DIRT) || bs.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) || bs.is(net.minecraft.world.level.block.Blocks.COBBLESTONE))) continue;
					if (breaking == null || !breaking.equals(q)) {
						if (breaking != null) mc.gameMode.stopDestroyBlock();
						startBreak(mc, q, bs.is(net.minecraft.world.level.block.Blocks.COBBLESTONE) ? ItemTags.PICKAXES : ItemTags.SHOVELS);
					} else if (++wait > 200) {
						mc.gameMode.stopDestroyBlock();
						breaking = null;
						return fail("Erde auf " + q.toShortString() + " geht nicht weg");
					} else {
						mc.gameMode.continueDestroyBlock(q, face(mc, q));
					}
					return Result.RUNNING;
				}
				return finishFell(mc);
			}
		}
		return Result.RUNNING;
	}

	private static Result finishFell(Minecraft mc) {
		release(mc);
		reset();
		return Result.DONE;
	}

	/** Auf der Stelle hochspringen und Erde unter die Fuesse setzen. */
	private static void pillar(Minecraft mc) {
		var p = mc.player;
		int dirt = Hotbar.bring(mc, s -> towerMode ? s.is(net.minecraft.world.item.Items.COBBLESTONE)
			: s.is(net.minecraft.world.item.Items.DIRT) || s.is(net.minecraft.world.item.Items.GRASS_BLOCK) || s.is(net.minecraft.world.item.Items.COBBLESTONE));
		if (dirt < 0) {
			error = "keine Erde zum Hochbauen";
			return;
		}
		p.getInventory().setSelectedSlot(dirt);
		p.setXRot(90);
		if (p.onGround()) {
			mc.options.keyJump.setDown(true);
			wait = 0;
			return;
		}
		mc.options.keyJump.setDown(false);
		BlockPos feet = BlockPos.containing(p.getX(), Math.floor(p.getY() + 0.01), p.getZ());
		BlockPos ground = BlockPos.containing(p.getX(), p.getY() - 1.01, p.getZ());
		// erst setzen, wenn die Fuesse ueber dem Zielblock sind
		if (p.getY() > feet.getY() + 0.05 && mc.level.getBlockState(feet).isAir() && !mc.level.getBlockState(feet.below()).isAir()) {
			mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(feet.below()).add(0, 0.5, 0), Direction.UP, feet.below(), false));
		} else if (p.getY() - ground.getY() > 1.1 && mc.level.getBlockState(ground).isAir() && !mc.level.getBlockState(ground.below()).isAir()) {
			mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(ground.below()).add(0, 0.5, 0), Direction.UP, ground.below(), false));
		}
	}

	private static BlockPos highestLog(net.minecraft.world.level.Level level, BlockPos base) {
		BlockPos best = null;
		for (int y = 0; y <= MAX_HEIGHT + 6; y++) {
			BlockPos q = base.above(y);
			if (target(level.getBlockState(q))) best = q;
		}
		return best;
	}

	private static BlockPos lowestReachableLog(Minecraft mc, BlockPos base) {
		for (int y = 0; y <= MAX_HEIGHT + 6; y++) {
			BlockPos q = base.above(y);
			if (target(mc.level.getBlockState(q)) && mc.player.isWithinBlockInteractionRange(q, -0.3)
				&& (!towerMode || q.getY() > mc.player.getBlockY() - 1 || mc.player.getBlockX() != q.getX() || mc.player.getBlockZ() != q.getZ())) return q;
		}
		return null;
	}

	private static void startBreak(Minecraft mc, BlockPos pos, net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tool) {
		int slot = Hotbar.bring(mc, s -> s.is(tool) && s.getMaxDamage() - s.getDamageValue() > 20);
		if (slot >= 0) mc.player.getInventory().setSelectedSlot(slot);
		look(mc, Vec3.atCenterOf(pos));
		mc.gameMode.startDestroyBlock(pos, face(mc, pos));
		breaking = pos;
		wait = 0;
	}

	private static Direction face(Minecraft mc, BlockPos pos) {
		return Direction.getApproximateNearest(mc.player.getEyePosition().subtract(Vec3.atCenterOf(pos)));
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

	private static Result done() {
		reset();
		return Result.DONE;
	}

	private static Result fail(String why) {
		error = why;
		reset();
		return Result.FAIL;
	}

	private static Result stop(String why) {
		error = why;
		reset();
		return Result.STOP;
	}

	static String id(ItemStack s) {
		return BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
	}
}
