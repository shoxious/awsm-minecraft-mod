package de.shoxi.mcguide;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * /lager ansehen: schreibt die Umgebung (Kisten, Shulker, Schilder, freie Plaetze, Inventar) nach
 * config/mcguide-lager.txt und macht einen Screenshot – damit sich jemand ausserhalb des Spiels
 * (z. B. Claude Code) den Lagerplatz ansehen kann.
 */
final class StorageView {
	private static final int R = 10;
	private static int screenshotIn = -1;

	static void dump(Minecraft mc) {
		if (mc.player == null || mc.level == null) return;
		BlockPos me = mc.player.blockPosition();
		StringBuilder out = new StringBuilder();
		out.append("Spieler: ").append(me.getX()).append(' ').append(me.getY()).append(' ').append(me.getZ())
			.append(", Blick nach ").append(mc.player.getDirection().getName())
			.append(" (yaw ").append(Math.round(mc.player.getYRot())).append(", pitch ").append(Math.round(mc.player.getXRot())).append(")")
			.append(", Greifweite ").append(mc.player.blockInteractionRange()).append('\n');
		out.append("Dimension: ").append(mc.level.dimension().identifier()).append("\n\n");

		Map<String, Integer> inv = new TreeMap<>();
		for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
			ItemStack s = mc.player.getInventory().getItem(i);
			if (!s.isEmpty()) inv.merge(BuiltInRegistries.ITEM.getKey(s.getItem()).getPath(), s.getCount(), Integer::sum);
		}
		out.append("Inventar: ").append(inv).append("\n\n");

		out.append("Behaelter und Schilder (dx dy dz relativ zum Spieler | absolut | Art | Info | in Greifweite):\n");
		List<String> rows = new ArrayList<>();
		for (BlockPos p : BlockPos.betweenClosed(me.offset(-R, -3, -R), me.offset(R, 4, R))) {
			BlockState st = mc.level.getBlockState(p);
			String kind = null, info = "";
			if (st.getBlock() instanceof ChestBlock) {
				kind = BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath();
				info = st.getValue(ChestBlock.TYPE).getSerializedName() + ", Front " + st.getValue(ChestBlock.FACING).getName();
				String label = ItemCategories.label(mc.level, List.of(p.immutable()));
				if (label != null) info += ", Schild \"" + label + "\"";
			} else if (st.getBlock() instanceof BarrelBlock) {
				kind = "barrel";
			} else if (st.getBlock() instanceof ShulkerBoxBlock sb) {
				kind = "shulker_box";
				info = sb.getColor() == null ? "ungefaerbt" : sb.getColor().getName();
			} else if (st.getBlock() instanceof EnderChestBlock) {
				kind = "ender_chest";
			} else if (st.getBlock() instanceof SignBlock) {
				kind = BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath();
			}
			if (kind == null) continue;
			rows.add((p.getX() - me.getX()) + " " + (p.getY() - me.getY()) + " " + (p.getZ() - me.getZ()) + " | "
				+ p.getX() + " " + p.getY() + " " + p.getZ() + " | " + kind + " | " + info + " | "
				+ (mc.player.isWithinBlockInteractionRange(p, 0) ? "ja" : "nein"));
		}
		rows.forEach(r -> out.append(r).append('\n'));

		out.append("\nDraufsicht je Hoehe (Norden oben, x nach rechts): @ Spieler, C Kiste, T Fallenkiste, B Fass, S Shulker, ")
			.append("E Endertruhe, s Schild, # fester Block, + anderer Block, . frei\n");
		for (int dy = 4; dy >= -2; dy--) {
			out.append("\ndy ").append(dy).append(" (y ").append(me.getY() + dy).append("):\n");
			for (int dz = -R; dz <= R; dz++) {
				for (int dx = -R; dx <= R; dx++) {
					BlockPos p = me.offset(dx, dy, dz);
					out.append(dx == 0 && dz == 0 && (dy == 0 || dy == 1) ? '@' : symbol(mc, p));
				}
				out.append('\n');
			}
		}

		Path file = FabricLoader.getInstance().getConfigDir().resolve("mcguide-lager.txt");
		try {
			Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
			mc.player.sendSystemMessage(Component.literal("Lager angesehen: " + rows.size() + " Behälter/Schilder notiert, Screenshot folgt")
				.withStyle(ChatFormatting.GREEN));
		} catch (Exception e) {
			GuideServer.LOG.warn("Lager-Ansicht nicht gespeichert", e);
		}
		screenshotIn = 10; // erst wenn der Chat zu ist
	}

	private static char symbol(Minecraft mc, BlockPos p) {
		BlockState st = mc.level.getBlockState(p);
		if (st.getBlock() instanceof ChestBlock) return BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath().contains("trapped") ? 'T' : 'C';
		if (st.getBlock() instanceof BarrelBlock) return 'B';
		if (st.getBlock() instanceof ShulkerBoxBlock) return 'S';
		if (st.getBlock() instanceof EnderChestBlock) return 'E';
		if (st.getBlock() instanceof SignBlock) return 's';
		if (st.canBeReplaced()) return '.';
		return st.isCollisionShapeFullBlock(mc.level, p) ? '#' : '+';
	}

	static void tick(Minecraft mc) {
		if (screenshotIn < 0 || --screenshotIn > 0) return;
		screenshotIn = -1;
		if (mc.gui.screen() == null) Screenshot.grab(mc, false);
	}

	private StorageView() {}
}
