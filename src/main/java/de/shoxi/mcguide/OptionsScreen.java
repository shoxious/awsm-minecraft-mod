package de.shoxi.mcguide;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Taste O: alle Helfer an-/ausschalten. */
public class OptionsScreen extends Screen {
	private record Toggle(String label, BooleanSupplier get, Consumer<GuideState> flip) {}

	private static final int W = 320;

	private static List<Toggle> toggles() {
		return List.of(
			new Toggle("Erz-Finder (F8)", () -> GuideClient.settings.oreFinder, s -> s.oreFinder = !s.oreFinder),
			new Toggle("Kisten-Finder (F6)", () -> GuideClient.settings.chestFinder, s -> s.chestFinder = !s.chestFinder),
			new Toggle("Mobs markieren (F10)", () -> GuideClient.settings.mobGlow, s -> s.mobGlow = !s.mobGlow),
			new Toggle("Bastion-Finder", () -> GuideClient.settings.bastionFinder, s -> s.bastionFinder = !s.bastionFinder),
			new Toggle("Endstadt-/Elytra-Finder", () -> GuideClient.settings.endCityFinder, s -> s.endCityFinder = !s.endCityFinder),
			new Toggle("Fullbright (,)", () -> GuideClient.settings.fullbright, s -> s.fullbright = !s.fullbright),
			new Toggle("Fäller/Ader-Abbau", () -> GuideClient.settings.treeFeller, s -> s.treeFeller = !s.treeFeller),
			new Toggle("Setzlinge nachpflanzen", () -> GuideClient.settings.saplings, s -> s.saplings = !s.saplings),
			new Toggle("Auto-Ernte (F7)", () -> GuideClient.settings.autoCrops, s -> s.autoCrops = !s.autoCrops),
			new Toggle("Knochenmehl (Ernte)", () -> GuideClient.settings.boneMeal, s -> s.boneMeal = !s.boneMeal),
			new Toggle("Auto-Füttern (F12)", () -> GuideClient.settings.autoFeed, s -> s.autoFeed = !s.autoFeed),
			new Toggle("Auto-Werkzeug", () -> GuideClient.settings.autoTool, s -> s.autoTool = !s.autoTool),
			new Toggle("Auto-Essen", () -> GuideClient.settings.autoEat, s -> s.autoEat = !s.autoEat),
			new Toggle("Auto-Fackel", () -> GuideClient.settings.autoTorch, s -> s.autoTorch = !s.autoTorch),
			new Toggle("Fackeln auch draußen", () -> GuideClient.settings.torchOutdoor, s -> s.torchOutdoor = !s.torchOutdoor),
			new Toggle("Licht-Anzeige (.)", () -> GuideClient.settings.lightOverlay, s -> s.lightOverlay = !s.lightOverlay),
			new Toggle("Werkzeug-Warnung", () -> GuideClient.settings.toolWarn, s -> s.toolWarn = !s.toolWarn),
			new Toggle("Verzauberungs-Tipps", () -> GuideClient.settings.enchantHelp, s -> s.enchantHelp = !s.enchantHelp),
			new Toggle("Koordinaten-Anzeige", () -> GuideClient.settings.coords, s -> s.coords = !s.coords));
	}

	public OptionsScreen() {
		super(Component.literal("Helfer"));
	}

	@Override
	protected void init() {
		int x = (width - W) / 2;
		int colW = (W - 4) / 2;
		List<Toggle> list = toggles();
		for (int i = 0; i < list.size(); i++) {
			Toggle t = list.get(i);
			int bx = x + (i % 2) * (colW + 4);
			int by = 40 + (i / 2) * 24;
			addRenderableWidget(Button.builder(label(t), b -> {
				t.flip().accept(GuideClient.settings);
				GuideClient.settings.save();
				b.setMessage(label(t));
			}).bounds(bx, by, colW, 20).build());
		}
		addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> onClose())
			.bounds(x + W / 4, 40 + ((list.size() + 1) / 2) * 24 + 8, W / 2, 20).build());
	}

	private static Component label(Toggle t) {
		boolean on = t.get().getAsBoolean();
		return Component.literal(t.label() + ": ").append(Component.literal(on ? "an" : "aus")
			.withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g, mouseX, mouseY, partialTick);
		Component title = Component.literal("⚙ Helfer").withStyle(ChatFormatting.BOLD);
		g.text(font, title, (width - font.width(title)) / 2, 16, 0xFFFFFFFF);
		Component hint = Component.literal("Menü: J · Zoom: C halten · Wegpunkt: N · Sortieren: V im Inventar · End-Karte: H · /wp");
		g.text(font, hint, (width - font.width(hint)) / 2, 28, 0xFFB0B4BC);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	static void open(Minecraft mc) {
		mc.gui.setScreen(new OptionsScreen());
	}
}
