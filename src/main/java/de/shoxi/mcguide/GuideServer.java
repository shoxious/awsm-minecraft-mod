package de.shoxi.mcguide;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.world.level.storage.LevelResource;

/** Serverseite: prueft die Ziele, speichert pro Spieler in der Welt, /guide im Chat. */
public class GuideServer implements ModInitializer {
	public static final String MOD_ID = "mcguide";
	public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

	private static final Map<UUID, GuideData> DATA = new HashMap<>();
	/** Spieler, deren Client den Stand schon bekommen hat (beim Join kennt der Server die Client-Kanaele oft noch nicht). */
	private static final Set<UUID> SYNCED = new HashSet<>();
	private static MinecraftServer server;
	private static int ticks;

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.clientboundPlay().register(GuideNet.Sync.TYPE, GuideNet.Sync.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(GuideNet.Toggle.TYPE, GuideNet.Toggle.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(GuideNet.Hello.TYPE, GuideNet.Hello.CODEC);

		ServerLifecycleEvents.SERVER_STARTED.register(s -> server = s);
		ServerLifecycleEvents.SERVER_STOPPING.register(s -> {
			DATA.forEach((id, d) -> d.save(file(id)));
			DATA.clear();
			server = null;
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, s) -> onJoin(handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, s) -> {
			SYNCED.remove(handler.player.getUUID());
			GuideData d = DATA.remove(handler.player.getUUID());
			if (d != null) d.save(file(handler.player.getUUID()));
		});
		ServerTickEvents.END_SERVER_TICK.register(s -> {
			GiantTrees.tick(s);
			if (++ticks % 20 != 0) return;
			for (ServerPlayer p : s.getPlayerList().getPlayers()) {
				evaluate(p);
				if (!SYNCED.contains(p.getUUID())) sync(p, data(p));
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(GuideNet.Toggle.TYPE, (payload, ctx) -> toggle(ctx.player(), payload.id(), false));
		ServerPlayNetworking.registerGlobalReceiver(GuideNet.Hello.TYPE, (payload, ctx) -> {
			LOG.info("Guide-Mod-Client von {} gemeldet", ctx.player().getName().getString());
			SYNCED.remove(ctx.player().getUUID());
			// der Client hat sich selbst gemeldet, also hat er den Mod – auch senden, wenn die Kanalliste noch fehlt
			sync(ctx.player(), data(ctx.player()), true);
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registries, env) -> dispatcher.register(
			Commands.literal("guide")
				.executes(c -> {
					show(c.getSource().getPlayerOrException());
					return 1;
				})
				.then(Commands.literal("toggle").then(Commands.argument("id", StringArgumentType.word()).executes(c -> {
					toggle(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "id"), true);
					return 1;
				})))));
		// Admin: Stammbueschel fuer aeltere Riesenbaeume nachruesten
		CommandRegistrationCallback.EVENT.register((dispatcher, registries, env) -> dispatcher.register(
			Commands.literal("riesenbaum").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("bueschel")
					.then(Commands.argument("ecke", BlockPosArgument.blockPos())
						.then(Commands.argument("groesse", IntegerArgumentType.integer(4, 128)).executes(c -> {
							int n = GiantTrees.addSprouts(c.getSource().getLevel(), BlockPosArgument.getLoadedBlockPos(c, "ecke"),
								IntegerArgumentType.getInteger(c, "groesse"));
							c.getSource().sendSuccess(() -> Component.literal(n < 0 ? "Kein Stamm gefunden" : n + " Blätter gesetzt"), false);
							return Math.max(n, 0);
						}))))
				.then(Commands.literal("lichten")
					.then(Commands.argument("ecke", BlockPosArgument.blockPos())
						.then(Commands.argument("groesse", IntegerArgumentType.integer(4, 128)).executes(c -> {
							int n = GiantTrees.carveExisting(c.getSource().getLevel(), BlockPosArgument.getLoadedBlockPos(c, "ecke"),
								IntegerArgumentType.getInteger(c, "groesse"));
							c.getSource().sendSuccess(() -> Component.literal(n < 0 ? "Kein Stamm gefunden" : n + " Blätter für Lichtlücken entfernt"), false);
							return Math.max(n, 0);
						}))))));
	}

	private static Path file(UUID id) {
		return server.getWorldPath(LevelResource.ROOT).resolve("mcguide").resolve(id + ".json");
	}

	private static GuideData data(ServerPlayer p) {
		return DATA.computeIfAbsent(p.getUUID(), id -> GuideData.load(file(id)));
	}

	private static void onJoin(ServerPlayer p) {
		GuideData d = data(p);
		boolean fresh = d.baseline == null;
		if (fresh) {
			Map<String, Integer> base = new HashMap<>();
			for (Stat<?> s : Goals.TRACKED) base.put(GuideData.key(s), p.getStats().getValue(s));
			d.baseline = base;
			d.save(file(p.getUUID()));
		}
		evaluate(p);
		sync(p, d);
		if (fresh) {
			p.sendSystemMessage(Component.literal("📖 Neu: dein Anfänger-Guide! ").withStyle(ChatFormatting.GOLD)
				.append(command("[/guide]", "/guide", "Guide anzeigen"))
				.append(Component.literal(" oder F9 mit dem Guide-Mod.").withStyle(ChatFormatting.GRAY)));
		}
	}

	private static void evaluate(ServerPlayer p) {
		GuideData d = data(p);
		if (d.baseline == null) return;
		Progress progress = new Progress(p, p.getStats(), d.baseline);
		boolean changed = false;
		for (Goals.Phase phase : Goals.PHASES) {
			for (Goals.Goal g : phase.goals()) {
				if (d.auto.contains(g.id())) continue;
				boolean ok;
				try {
					ok = g.check().test(progress);
				} catch (RuntimeException e) {
					ok = false;
				}
				if (!ok) continue;
				boolean wasDone = d.isDone(g.id());
				d.auto.add(g.id());
				changed = true;
				if (!wasDone) announce(p, d, g);
			}
		}
		if (changed) {
			d.save(file(p.getUUID()));
			sync(p, d);
		}
	}

	private static void announce(ServerPlayer p, GuideData d, Goals.Goal g) {
		p.sendSystemMessage(Component.literal("✔ Erreicht: " + g.text()).withStyle(ChatFormatting.GREEN));
		Goals.Phase phase = Goals.phaseOf(g);
		if (phase != null && phase.goals().stream().allMatch(x -> d.isDone(x.id()))) {
			p.sendSystemMessage(Component.literal("🎉 " + phase.title() + " geschafft!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		}
		Goals.Goal next = d.next();
		if (next == null) {
			p.sendSystemMessage(Component.literal("Alles geschafft! 🐉").withStyle(ChatFormatting.GOLD));
		} else {
			p.sendSystemMessage(Component.literal("➜ Nächstes Ziel: " + next.text()).withStyle(ChatFormatting.YELLOW));
			p.sendSystemMessage(Component.literal("   " + Goals.howTo(next)).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
		}
		p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);
	}

	private static void toggle(ServerPlayer p, String id, boolean fromChat) {
		boolean known = Goals.PHASES.stream().flatMap(ph -> ph.goals().stream()).anyMatch(g -> g.id().equals(id));
		if (!known) return;
		GuideData d = data(p);
		d.toggle(id);
		d.save(file(p.getUUID()));
		sync(p, d);
		if (fromChat) show(p);
	}

	private static void sync(ServerPlayer p, GuideData d) {
		sync(p, d, false);
	}

	private static void sync(ServerPlayer p, GuideData d, boolean force) {
		if (!force && !ServerPlayNetworking.canSend(p, GuideNet.Sync.TYPE)) return;
		ArrayList<String> done = new ArrayList<>();
		for (Goals.Phase phase : Goals.PHASES)
			for (Goals.Goal g : phase.goals())
				if (d.isDone(g.id())) done.add(g.id());
		ServerPlayNetworking.send(p, new GuideNet.Sync(done, new ArrayList<>(d.manual)));
		if (SYNCED.add(p.getUUID())) LOG.info("Guide-Stand an {} gesendet ({} erledigt)", p.getName().getString(), done.size());
	}

	/** Die Liste im Chat: erledigte Ziele durchgestrichen, Klick hakt ab. */
	private static void show(ServerPlayer p) {
		GuideData d = data(p);
		p.sendSystemMessage(Component.literal("━━━━ 📖 Anfänger-Guide ━━━━").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		Goals.Goal next = d.next();
		for (Goals.Phase phase : Goals.PHASES) {
			long done = phase.goals().stream().filter(g -> d.isDone(g.id())).count();
			boolean complete = done == phase.goals().size();
			boolean current = next != null && phase.goals().contains(next);
			MutableComponent head = Component.literal(phase.title() + "  " + done + "/" + phase.goals().size())
				.withStyle(complete ? ChatFormatting.DARK_GREEN : current ? ChatFormatting.YELLOW : ChatFormatting.GRAY);
			p.sendSystemMessage(head);
			// abgeschlossene und spaetere Phasen nur als Kopfzeile, die aktuelle ausgeklappt
			if (!current) continue;
			for (Goals.Goal g : phase.goals()) {
				boolean ok = d.isDone(g.id());
				MutableComponent line = Component.literal((ok ? "  ☑ " : "  ☐ ") + g.text())
					.withStyle(ok ? new ChatFormatting[] {ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH}
						: new ChatFormatting[] {ChatFormatting.WHITE});
				line.withStyle(s -> s.withClickEvent(new ClickEvent.RunCommand("/guide toggle " + g.id()))
					.withHoverEvent(new HoverEvent.ShowText(Component.literal(ok ? "Klicken: wieder offen" : "Klicken: abhaken"))));
				p.sendSystemMessage(line);
			}
		}
		p.sendSystemMessage(next == null
			? Component.literal("Alles geschafft! 🐉").withStyle(ChatFormatting.GOLD)
			: Component.literal("➜ Als Nächstes: " + next.text()).withStyle(ChatFormatting.GREEN));
		if (next != null) p.sendSystemMessage(Component.literal("   " + Goals.howTo(next)).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
	}

	private static MutableComponent command(String label, String cmd, String hover) {
		return Component.literal(label).withStyle(s -> s.withColor(ChatFormatting.AQUA).withUnderlined(true)
			.withClickEvent(new ClickEvent.RunCommand(cmd))
			.withHoverEvent(new HoverEvent.ShowText(Component.literal(hover))));
	}
}
