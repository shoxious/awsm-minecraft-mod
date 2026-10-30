package de.shoxi.mcguide;

import java.util.HashSet;
import java.util.Set;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Clientseite: F9 oeffnet den Notizblock, Daten kommen vom Server. */
public class GuideClient implements ClientModInitializer {
	public static GuideState settings;
	private static KeyMapping openKey;
	private static KeyMapping oreKey;
	private static KeyMapping cropKey;
	private static KeyMapping chestKey;
	private static KeyMapping mobKey;
	private static KeyMapping brightKey;
	private static KeyMapping zoomKey;
	private static KeyMapping waypointKey;
	private static KeyMapping optionsKey;
	private static KeyMapping sortKey;
	private static KeyMapping feedKey;
	private static KeyMapping lightKey;
	private static KeyMapping endMapKey;

	/** Letzter Stand vom Server; synced = Server hat den Guide-Mod. */
	static final Set<String> done = new HashSet<>();
	static final Set<String> manual = new HashSet<>();
	static boolean synced;
	private static int helloTicks;
	private static boolean loggedNoChannel;

	@Override
	public void onInitializeClient() {
		settings = GuideState.load();
		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(GuideServer.MOD_ID, "main"));
		openKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.mcguide.open", InputConstants.KEY_F9, category));
		oreKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.mcguide.ores", InputConstants.KEY_F8, category));
		cropKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.mcguide.crops", InputConstants.KEY_F7, category));
		chestKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.mcguide.chests", InputConstants.KEY_F6, category));
		mobKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.mcguide.mobs", InputConstants.KEY_F10, category));
		brightKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.mcguide.fullbright", InputConstants.KEY_COMMA, category));
		zoomKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.mcguide.zoom", InputConstants.KEY_C, category));
		waypointKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.mcguide.waypoint", InputConstants.KEY_N, category));
		optionsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.mcguide.options", InputConstants.KEY_J, category));
		endMapKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.mcguide.endmap", InputConstants.KEY_H, category));
		lightKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.mcguide.light", InputConstants.KEY_PERIOD, category));
		feedKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.mcguide.feed", InputConstants.KEY_F12, category));
		sortKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.mcguide.sort", InputConstants.KEY_V, category));
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (screen instanceof AbstractContainerScreen<?> acs) {
				ScreenEvents.afterExtract(screen).register((s, g, mx, my, delta) -> EnchantAdvisor.render(acs, g));
			}
			if (screen instanceof AbstractContainerScreen<?> acs && !(screen instanceof CreativeModeInventoryScreen)) {
				ScreenKeyboardEvents.afterKeyPress(screen).register((s, event) -> {
					if (sortKey.matches(event) && !(s.getFocused() instanceof EditBox)) InventorySort.sort(client, acs);
				});
			}
		});
		ClientPlayerBlockBreakEvents.AFTER.register((level, player, pos, state) ->
			TreeFeller.onBreak(Minecraft.getInstance(), player, pos, state));

		ClientPlayNetworking.registerGlobalReceiver(GuideNet.Sync.TYPE, (payload, ctx) -> {
			done.clear();
			done.addAll(payload.done());
			manual.clear();
			manual.addAll(payload.manual());
			if (!synced) GuideServer.LOG.info("Guide-Daten vom Server empfangen ({} erledigt)", payload.done().size());
			synced = true;
		});
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> hello());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			synced = false;
			TreeFeller.cancel();
			Helpers.reset();
			AutoFeed.reset();
			done.clear();
			manual.clear();
		});
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			while (openKey.consumeClick()) {
				if (mc.player != null && mc.gui.screen() == null) mc.gui.setScreen(new GuideScreen());
			}
			while (oreKey.consumeClick()) {
				if (mc.player != null) OreFinder.toggle(mc);
			}
			while (cropKey.consumeClick()) {
				if (mc.player != null) AutoCrops.toggle(mc);
			}
			while (chestKey.consumeClick()) {
				if (mc.player != null) Finders.toggleChests(mc);
			}
			while (mobKey.consumeClick()) {
				if (mc.player != null) Finders.toggleMobs(mc);
			}
			while (brightKey.consumeClick()) {
				if (mc.player != null) Helpers.toggleFullbright(mc);
			}
			while (waypointKey.consumeClick()) {
				if (mc.player != null) Waypoints.set(mc, null);
			}
			while (lightKey.consumeClick()) {
				if (mc.player != null) LightOverlay.toggle(mc);
			}
			while (feedKey.consumeClick()) {
				if (mc.player != null) AutoFeed.toggle(mc);
			}
			while (endMapKey.consumeClick()) {
				if (mc.player != null && mc.gui.screen() == null) mc.gui.setScreen(new EndMapScreen());
			}
			while (optionsKey.consumeClick()) {
				if (mc.player != null && mc.gui.screen() == null) OptionsScreen.open(mc);
			}
			OreFinder.tick(mc);
			Finders.tick(mc);
			Helpers.tick(mc);
			Waypoints.tick(mc);
			BastionFinder.tick(mc);
			EndCityFinder.tick(mc);
			AutoFeed.tick(mc);
			LightOverlay.tick(mc);
			TreeFeller.tick(mc);
			AutoCrops.tick(mc);
			// solange der Server nicht geantwortet hat, alle 2 Sekunden erneut melden
			if (mc.player != null && !synced && ++helloTicks % 40 == 0) hello();
		});
		GuideHud.register();
		BotMode.register();

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx) -> dispatcher.register(
			ClientCommands.literal("erze")
				.executes(c -> {
					OreFinder.toggle(Minecraft.getInstance());
					return 1;
				})
				.then(ClientCommands.literal("an").executes(c -> {
					OreFinder.setEnabled(Minecraft.getInstance(), true);
					return 1;
				}))
				.then(ClientCommands.literal("aus").executes(c -> {
					OreFinder.setEnabled(Minecraft.getInstance(), false);
					return 1;
				}))
				.then(ClientCommands.literal("alle").executes(c -> {
					OreFinder.toggleExtras(Minecraft.getInstance());
					return 1;
				}))
				.then(ClientCommands.argument("umkreis", IntegerArgumentType.integer(3, 32)).executes(c -> {
					OreFinder.setRadius(Minecraft.getInstance(), IntegerArgumentType.getInteger(c, "umkreis"));
					return 1;
				}))));
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx) -> dispatcher.register(
			ClientCommands.literal("ernte")
				.executes(c -> {
					AutoCrops.toggle(Minecraft.getInstance());
					return 1;
				})
				.then(ClientCommands.literal("an").executes(c -> {
					AutoCrops.setEnabled(Minecraft.getInstance(), true);
					return 1;
				}))
				.then(ClientCommands.literal("aus").executes(c -> {
					AutoCrops.setEnabled(Minecraft.getInstance(), false);
					return 1;
				}))));
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx) -> dispatcher.register(
			ClientCommands.literal("faellen")
				.then(ClientCommands.literal("an").executes(c -> setFeller(true)))
				.then(ClientCommands.literal("aus").executes(c -> setFeller(false)))));
		SuggestionProvider<FabricClientCommandSource> wpNames = (c, b) -> {
			for (Waypoints.Wp w : Waypoints.list(Minecraft.getInstance())) b.suggest(w.name);
			return b.buildFuture();
		};
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx) -> dispatcher.register(
			ClientCommands.literal("wp")
				.executes(c -> {
					Waypoints.print(Minecraft.getInstance());
					return 1;
				})
				.then(ClientCommands.literal("setzen")
					.executes(c -> {
						Waypoints.set(Minecraft.getInstance(), null);
						return 1;
					})
					.then(ClientCommands.argument("name", StringArgumentType.greedyString()).executes(c -> {
						Waypoints.set(Minecraft.getInstance(), StringArgumentType.getString(c, "name"));
						return 1;
					})))
				.then(ClientCommands.literal("ziel")
					.then(ClientCommands.argument("name", StringArgumentType.greedyString()).suggests(wpNames).executes(c -> {
						Waypoints.target(Minecraft.getInstance(), StringArgumentType.getString(c, "name"));
						return 1;
					})))
				.then(ClientCommands.literal("weg")
					.then(ClientCommands.argument("name", StringArgumentType.greedyString()).suggests(wpNames).executes(c -> {
						Waypoints.remove(Minecraft.getInstance(), StringArgumentType.getString(c, "name"));
						return 1;
					})))
				.then(ClientCommands.literal("aus").executes(c -> {
					Waypoints.clearTarget(Minecraft.getInstance());
					return 1;
				}))));
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx) -> dispatcher.register(
			ClientCommands.literal("licht").executes(c -> {
				LightOverlay.toggle(Minecraft.getInstance());
				return 1;
			})));
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx) -> dispatcher.register(
			ClientCommands.literal("fackeln")
				.then(ClientCommands.literal("draussen").executes(c -> setTorchOutdoor(true)))
				.then(ClientCommands.literal("hoehle").executes(c -> setTorchOutdoor(false)))));
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx) -> dispatcher.register(
			ClientCommands.literal("fuettern").executes(c -> {
				AutoFeed.toggle(Minecraft.getInstance());
				return 1;
			})));
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx) -> dispatcher.register(
			ClientCommands.literal("helfer").executes(c -> {
				Minecraft mc = Minecraft.getInstance();
				mc.schedule(() -> OptionsScreen.open(mc));
				return 1;
			})));
	}

	private static int setFeller(boolean on) {
		settings.treeFeller = on;
		settings.save();
		if (!on) TreeFeller.cancel();
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) mc.player.sendOverlayMessage(Component.literal("Auto-Fäller/Ader-Abbau " + (on ? "an (Schleichen + Axt/Spitzhacke)" : "aus")));
		return 1;
	}

	private static int setTorchOutdoor(boolean on) {
		settings.torchOutdoor = on;
		if (on) settings.autoTorch = true;
		settings.save();
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) mc.player.sendOverlayMessage(Component.literal(on ? "Auto-Fackel: auch draußen" : "Auto-Fackel: nur in Höhlen"));
		return 1;
	}

	/** Fuer CameraMixin. */
	public static boolean zooming() {
		return zoomKey != null && zoomKey.isDown() && Minecraft.getInstance().gui.screen() == null;
	}

	/** Fuer die Glow-Mixins. */
	public static boolean markedGlow(Entity e) {
		return settings != null && Finders.glows(e);
	}

	public static int glowColor(Entity e) {
		return Finders.glowColor(e);
	}

	/** Fuer MinecraftMixin: Vanilla soll den Abbau gerade nicht abbrechen. */
	public static boolean fellingBusy() {
		return TreeFeller.busy(Minecraft.getInstance());
	}

	/** Beim Server melden; der antwortet mit dem Guide-Stand. */
	static void hello() {
		if (ClientPlayNetworking.canSend(GuideNet.Hello.TYPE)) ClientPlayNetworking.send(new GuideNet.Hello());
		else if (!loggedNoChannel) {
			loggedNoChannel = true;
			GuideServer.LOG.info("Server kennt den Guide-Mod (noch) nicht");
		}
	}

	static String endMapKeyName() {
		return endMapKey.getTranslatedKeyMessage().getString();
	}

	static boolean endMapKeyMatches(KeyEvent event) {
		return endMapKey.matches(event);
	}

	static boolean openKeyMatches(KeyEvent event) {
		return openKey.matches(event);
	}

	static boolean isDone(String id) {
		return done.contains(id);
	}

	static Goals.Goal next() {
		for (Goals.Phase phase : Goals.PHASES)
			for (Goals.Goal g : phase.goals())
				if (!isDone(g.id())) return g;
		return null;
	}

	static void toggle(String id) {
		if (!ClientPlayNetworking.canSend(GuideNet.Toggle.TYPE)) return;
		ClientPlayNetworking.send(new GuideNet.Toggle(id));
		// sofort anzeigen, der Server bestaetigt mit einem Sync
		if (!done.remove(id)) done.add(id);
	}
}
