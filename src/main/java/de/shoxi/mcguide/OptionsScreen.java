package de.shoxi.mcguide;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Taste J: alle Helfer an-/ausschalten, nach Bereichen sortiert, mit Erklärung beim Drüberfahren. */
public class OptionsScreen extends Screen {
	private static final int MAX_W = 420;
	private static final int TOP = 32;
	private static final int BTN = 16;
	private static final int ROW = 18;
	private static final int HEADER = 12;
	private static final int GAP = 4;
	private final List<OptionsScreen.Placed> placed = new ArrayList<>();
	private final List<OptionsScreen.Header> headers = new ArrayList<>();
	private final List<OptionsScreen.Note> notes = new ArrayList<>();
	private int w;
	private int contentHeight;
	private int bottom;
	private double scroll;

	private static List<OptionsScreen.Section> sections() {
		return List.of(
			new OptionsScreen.Section(
				"Finder",
				List.of(
					new OptionsScreen.Toggle(
						"Erz-Finder", "Erze in der Nähe durch Wände anzeigen (/erze)", () -> GuideClient.settings.oreFinder, s -> s.oreFinder = !s.oreFinder
					),
					new OptionsScreen.Toggle(
						"Loot-Finder",
						"Spawner, Tresore, verdächtigen Sand und Kisten markieren – mit Server-Mod nur ungeöffnete",
						() -> GuideClient.settings.chestFinder,
						s -> s.chestFinder = !s.chestFinder
					),
					new OptionsScreen.Toggle(
						"Mobs markieren", "Mobs leuchten: Monster rot, Tiere grün, Spieler blau", () -> GuideClient.settings.mobGlow, s -> s.mobGlow = !s.mobGlow
					),
					new OptionsScreen.Toggle(
						"Bastion-Finder",
						"Im Nether automatisch Wegpunkte an Bastionen setzen",
						() -> GuideClient.settings.bastionFinder,
						s -> s.bastionFinder = !s.bastionFinder
					),
					new OptionsScreen.Toggle(
						"Außenposten-Finder",
						"Erkennt Außenposten und Anwesen in Sichtweite und setzt einen Wegpunkt – läuft ohne Server-Mod",
						() -> GuideClient.settings.outpostFinder,
						s -> s.outpostFinder = !s.outpostFinder
					),
					new OptionsScreen.Toggle(
						"Allay-Finder",
						"Allays leuchten türkis und bekommen einen Wegpunkt",
						() -> GuideClient.settings.allayFinder,
						s -> s.allayFinder = !s.allayFinder
					),
					new OptionsScreen.Toggle(
						"Endstadt-/Elytra-Finder",
						"Im End Endstädte, Endschiffe mit Elytra und Endtore finden – Karte mit Taste H",
						() -> GuideClient.settings.endCityFinder,
						s -> s.endCityFinder = !s.endCityFinder
					)
				)
			),
			new OptionsScreen.Section(
				"Abbau & Farm",
				List.of(
					new OptionsScreen.Toggle(
						"Fäller/Ader-Abbau",
						"Schleichen + Axt fällt den ganzen Baum, Schleichen + Spitzhacke die ganze Erzader",
						() -> GuideClient.settings.treeFeller,
						s -> s.treeFeller = !s.treeFeller
					),
					new OptionsScreen.Toggle(
						"Setzlinge", "Beim Baumfällen einen Setzling nachpflanzen", () -> GuideClient.settings.saplings, s -> s.saplings = !s.saplings
					),
					new OptionsScreen.Toggle(
						"Auto-Ernte", "Reife Feldfrüchte ernten und neu anpflanzen", () -> GuideClient.settings.autoCrops, s -> s.autoCrops = !s.autoCrops
					),
					new OptionsScreen.Toggle(
						"Knochenmehl", "Auto-Ernte düngt mit Knochenmehl und füllt es nach", () -> GuideClient.settings.boneMeal, s -> s.boneMeal = !s.boneMeal
					),
					new OptionsScreen.Toggle(
						"Auto-Füttern", "Nutztiere in der Nähe automatisch füttern", () -> GuideClient.settings.autoFeed, s -> s.autoFeed = !s.autoFeed
					),
					new OptionsScreen.Toggle(
						"Auto-Handel",
						"Im Handelsfenster Feldfrüchte automatisch gegen Smaragde verkaufen",
						() -> GuideClient.settings.autoTrade,
						s -> s.autoTrade = !s.autoTrade
					)
				)
			),
			new OptionsScreen.Section(
				"Bauen",
				List.of(
					new OptionsScreen.Toggle(
						"Gelände-Modus",
						"Ein gesetzter Block (Erde, Stein, Sand ...) wird zum Hügel bzw. zur runden Fläche. Größe: /gelaende 1-4. Große Flächen: /gelaende ecke1/ecke2",
						() -> GuideClient.settings.terrainMode,
						s -> s.terrainMode = !s.terrainMode
					),
					new OptionsScreen.Toggle(
						"Hügel statt flach",
						"An: oben auf den Boden gesetzt wächst ein runder Hügel. Aus: nur eine flache Lage",
						() -> GuideClient.settings.terrainHill,
						s -> s.terrainHill = !s.terrainHill
					)
				)
			),
			new OptionsScreen.Section(
				"Automatik",
				List.of(
					new OptionsScreen.Toggle(
						"Auto-Werkzeug", "Passendes Werkzeug für den Block in die Hand nehmen", () -> GuideClient.settings.autoTool, s -> s.autoTool = !s.autoTool
					),
					new OptionsScreen.Toggle(
						"Auto-Essen", "Automatisch essen, wenn der Hunger sinkt", () -> GuideClient.settings.autoEat, s -> s.autoEat = !s.autoEat
					),
					new OptionsScreen.Toggle(
						"Auto-Fackel", "Im Dunkeln automatisch Fackeln setzen", () -> GuideClient.settings.autoTorch, s -> s.autoTorch = !s.autoTorch
					),
					new OptionsScreen.Toggle(
						"Fackeln draußen",
						"Auto-Fackel auch an der Oberfläche (/fackeln draussen)",
						() -> GuideClient.settings.torchOutdoor,
						s -> s.torchOutdoor = !s.torchOutdoor
					)
				)
			),
			new OptionsScreen.Section(
				"Anzeige",
				List.of(
					new OptionsScreen.Toggle(
						"Fullbright", "Alles hell, auch in Höhlen und nachts", () -> GuideClient.settings.fullbright, s -> s.fullbright = !s.fullbright
					),
					new OptionsScreen.Toggle(
						"Licht-Anzeige",
						"Stellen markieren, an denen Monster spawnen können",
						() -> GuideClient.settings.lightOverlay,
						s -> s.lightOverlay = !s.lightOverlay
					),
					new OptionsScreen.Toggle(
						"Koordinaten", "Koordinaten, Biom, Lichtlevel und Nether-Umrechnung", () -> GuideClient.settings.coords, s -> s.coords = !s.coords
					),
					new OptionsScreen.Toggle(
						"Werkzeug-Warnung", "Warnen, bevor ein Werkzeug kaputtgeht", () -> GuideClient.settings.toolWarn, s -> s.toolWarn = !s.toolWarn
					),
					new OptionsScreen.Toggle(
						"Verzauber-Tipps",
						"Beste Verzauberungen neben dem Inventar anzeigen",
						() -> GuideClient.settings.enchantHelp,
						s -> s.enchantHelp = !s.enchantHelp
					)
				)
			)
		);
	}

	public OptionsScreen() {
		super(Component.literal("Helfer"));
	}

	protected void init() {
		this.placed.clear();
		this.headers.clear();
		this.notes.clear();
		this.w = Math.min(this.width - 24, 420);
		int cols = this.w >= 338 ? 3 : 2;
		int x = (this.width - this.w) / 2;
		int colW = (this.w - (cols - 1) * 4) / cols;
		int y = 0;

		for (OptionsScreen.Section sec : sections()) {
			this.headers.add(new OptionsScreen.Header(sec.title(), y));
			y += 12;
			List<OptionsScreen.Toggle> list = sec.toggles();

			for (int i = 0; i < list.size(); i++) {
				OptionsScreen.Toggle t = list.get(i);
				Button b = (Button)this.addRenderableWidget(Button.builder(label(t), btn -> {
					t.flip().accept(GuideClient.settings);
					GuideClient.settings.save();
					btn.setMessage(label(t));
				}).bounds(x + i % cols * (colW + 4), 0, colW, 16).tooltip(Tooltip.create(Component.literal(t.help()))).build());
				this.placed.add(new OptionsScreen.Placed(b, y + i / cols * 18));
			}

			y += (list.size() + cols - 1) / cols * 18 + 4;
		}

		List<Component> conflicts = conflicts(this.minecraft);
		if (!conflicts.isEmpty()) {
			this.headers.add(new OptionsScreen.Header("Tastenkonflikte", y));
			y += 12;

			for (Component c : conflicts) {
				this.notes.add(new OptionsScreen.Note(c, y));
				y += this.font.split(c, this.w).size() * 10 + 2;
			}

			y += 4;
		}

		this.contentHeight = y - 4;
		int by = this.height - 16 - 6;
		int half = (this.w - 4) / 2;
		this.addRenderableWidget(
			Button.builder(Component.literal("Guide"), bx -> this.minecraft.gui.setScreen(new GuideScreen())).bounds(x, by, half, 16).build()
		);
		this.addRenderableWidget(Button.builder(Component.literal("Fertig"), bx -> this.onClose()).bounds(x + half + 4, by, half, 16).build());
		this.bottom = by - 4;
		this.layout();
	}

	private int viewHeight() {
		return Math.max(0, this.bottom - 32);
	}

	private void layout() {
		this.scroll = Mth.clamp(this.scroll, 0.0, Math.max(0, this.contentHeight - this.viewHeight()));

		for (OptionsScreen.Placed p : this.placed) {
			int y = 32 + p.y() - (int)this.scroll;
			p.widget().setY(y);
			p.widget().visible = y >= 32 && y + 16 <= this.bottom;
		}
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		this.scroll -= scrollY * 18.0;
		this.layout();
		return true;
	}

	private static List<Component> conflicts(Minecraft mc) {
		List<Component> out = new ArrayList<>();

		for (KeyMapping own : mc.options.keyMappings) {
			if (own.getName().startsWith("key.mcguide.") && !own.isUnbound()) {
				for (KeyMapping other : mc.options.keyMappings) {
					if (other != own
						&& own.same(other)
						&& !other.getName().startsWith("key.debug.")
						&& (!other.getName().startsWith("key.mcguide.") || other.getName().compareTo(own.getName()) >= 0)) {
						out.add(
							Component.literal("⚠ ")
								.append(Component.translatable(own.getName()))
								.append(" und ")
								.append(Component.translatable(other.getName()))
								.append(": beide auf ")
								.append(own.getTranslatedKeyMessage())
						);
					}
				}
			}
		}

		return out;
	}

	private static Component label(OptionsScreen.Toggle t) {
		boolean on = t.get().getAsBoolean();
		return Component.literal((on ? "✔ " : "✖ ") + t.label()).withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY);
	}

	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g, mouseX, mouseY, partialTick);
		Component title = Component.literal("⚙ Helfer").withStyle(ChatFormatting.BOLD);
		g.text(this.font, title, (this.width - this.font.width(title)) / 2, 8, -1);
		Component hint = Component.literal("Zoom: C halten · Wegpunkt: N · Sortieren: V · End-Karte: H · /wp · /gelaende");
		g.text(this.font, hint, (this.width - this.font.width(hint)) / 2, 19, -5196612);
		int x = (this.width - this.w) / 2;
		g.enableScissor(x - 8, 32, x + this.w + 8, this.bottom);

		for (OptionsScreen.Header h : this.headers) {
			int y = 32 + h.y() - (int)this.scroll + 2;
			g.text(this.font, Component.literal(h.title()).withStyle(ChatFormatting.BOLD), x, y, -11649);
			g.fill(x + this.font.width(h.title()) + 6, y + 4, x + this.w, y + 5, 1090519039);
		}

		for (OptionsScreen.Note n : this.notes) {
			g.textWithWordWrap(this.font, n.text(), x, 32 + n.y() - (int)this.scroll, this.w, -20416);
		}

		g.disableScissor();
		int max = this.contentHeight - this.viewHeight();
		if (max > 0) {
			int bar = Math.max(16, this.viewHeight() * this.viewHeight() / this.contentHeight);
			int top = 32 + (int)((this.viewHeight() - bar) * this.scroll / max);
			g.fill(x + this.w + 4, 32, x + this.w + 6, this.bottom, 822083583);
			g.fill(x + this.w + 4, top, x + this.w + 6, top + bar, -1056964609);
		}
	}

	public boolean isPauseScreen() {
		return false;
	}

	static void open(Minecraft mc) {
		mc.gui.setScreen(new OptionsScreen());
	}

	private record Header(String title, int y) {
	}

	private record Note(Component text, int y) {
	}

	private record Placed(AbstractWidget widget, int y) {
	}

	private record Section(String title, List<OptionsScreen.Toggle> toggles) {
	}

	private record Toggle(String label, String help, BooleanSupplier get, Consumer<GuideState> flip) {
	}
}
