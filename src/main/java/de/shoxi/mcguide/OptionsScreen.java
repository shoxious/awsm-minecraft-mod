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

/** Taste J: alle Helfer an-/ausschalten, nach Bereichen gruppiert und scrollbar (auch ohne F-Tasten, z. B. am Mac). */
public class OptionsScreen extends Screen {
	private record Toggle(String label, String help, BooleanSupplier get, Consumer<GuideState> flip) {}
	private record Section(String title, List<Toggle> toggles) {}
	private record Placed(AbstractWidget widget, int y) {}
	private record Header(String title, int y) {}
	private record Note(Component text, int y) {}

	/** Kompakt, damit moeglichst alles ohne Scrollen passt; Scrollen bleibt fuer kleine Fenster. */
	private static final int MAX_W = 420;
	private static final int TOP = 32;
	private static final int BTN = 16;
	private static final int ROW = BTN + 2;
	private static final int HEADER = 12;
	private static final int GAP = 4;

	private final List<Placed> placed = new ArrayList<>();
	private final List<Header> headers = new ArrayList<>();
	private final List<Note> notes = new ArrayList<>();
	private int w;
	private int contentHeight;
	private int bottom;
	private double scroll;

	private static List<Section> sections() {
		return List.of(
			new Section("Finder", List.of(
				new Toggle("Erz-Finder", "Erze in der Nähe durch Wände anzeigen (/erze)", () -> GuideClient.settings.oreFinder, s -> s.oreFinder = !s.oreFinder),
				new Toggle("Loot-Finder", "Ungeöffnete Kisten, Spawner, Tresore und verdächtigen Sand markieren", () -> GuideClient.settings.chestFinder, s -> s.chestFinder = !s.chestFinder),
				new Toggle("Mobs markieren", "Mobs leuchten: Monster rot, Tiere grün, Spieler blau", () -> GuideClient.settings.mobGlow, s -> s.mobGlow = !s.mobGlow),
				new Toggle("Bastion-Finder", "Im Nether automatisch Wegpunkte an Bastionen setzen", () -> GuideClient.settings.bastionFinder, s -> s.bastionFinder = !s.bastionFinder),
				new Toggle("Außenposten-Finder", "Erkennt Außenposten und Anwesen in Sichtweite und setzt einen Wegpunkt – läuft ohne Server-Mod", () -> GuideClient.settings.outpostFinder, s -> s.outpostFinder = !s.outpostFinder),
				new Toggle("Allay-Finder", "Allays leuchten türkis und bekommen einen Wegpunkt", () -> GuideClient.settings.allayFinder, s -> s.allayFinder = !s.allayFinder))),
			new Section("Abbau & Farm", List.of(
				new Toggle("Fäller/Ader-Abbau", "Schleichen + Axt fällt den ganzen Baum, Schleichen + Spitzhacke die ganze Erzader", () -> GuideClient.settings.treeFeller, s -> s.treeFeller = !s.treeFeller),
				new Toggle("Setzlinge", "Beim Baumfällen einen Setzling nachpflanzen", () -> GuideClient.settings.saplings, s -> s.saplings = !s.saplings),
				new Toggle("Auto-Ernte", "Reife Feldfrüchte ernten und neu anpflanzen", () -> GuideClient.settings.autoCrops, s -> s.autoCrops = !s.autoCrops),
				new Toggle("Knochenmehl", "Auto-Ernte düngt mit Knochenmehl und füllt es nach", () -> GuideClient.settings.boneMeal, s -> s.boneMeal = !s.boneMeal),
				new Toggle("Auto-Füttern", "Nutztiere in der Nähe automatisch füttern", () -> GuideClient.settings.autoFeed, s -> s.autoFeed = !s.autoFeed),
				new Toggle("Auto-Handel", "Im Handelsfenster Feldfrüchte und Federn automatisch gegen Smaragde verkaufen", () -> GuideClient.settings.autoTrade, s -> s.autoTrade = !s.autoTrade),
				new Toggle("Umzugs-Modus", "Kiste öffnen: Inhalt ins Inventar. Danach aufgestellte Shulker öffnen: genau das kommt hinein", () -> GuideClient.settings.relocate, s -> s.relocate = !s.relocate))),
			new Section("Bauen", List.of(
				new Toggle("Gelände-Modus", "Ein gesetzter Block (Erde, Stein, Sand ...) wird zum Hügel bzw. zur runden Fläche. Größe: /gelaende 1-4", () -> GuideClient.settings.terrainMode, s -> s.terrainMode = !s.terrainMode),
				new Toggle("Hügel statt flach", "An: oben auf den Boden gesetzt wächst ein runder Hügel. Aus: nur eine flache Lage", () -> GuideClient.settings.terrainHill, s -> s.terrainHill = !s.terrainHill))),
			new Section("Automatik", List.of(
				new Toggle("Auto-Werkzeug", "Passendes Werkzeug für den Block in die Hand nehmen", () -> GuideClient.settings.autoTool, s -> s.autoTool = !s.autoTool),
				new Toggle("Auto-Essen", "Automatisch essen, wenn der Hunger sinkt", () -> GuideClient.settings.autoEat, s -> s.autoEat = !s.autoEat),
				new Toggle("Auto-Fackel", "Im Dunkeln automatisch Fackeln setzen", () -> GuideClient.settings.autoTorch, s -> s.autoTorch = !s.autoTorch),
				new Toggle("Fackeln draußen", "Auto-Fackel auch an der Oberfläche (/fackeln draussen)", () -> GuideClient.settings.torchOutdoor, s -> s.torchOutdoor = !s.torchOutdoor))),
			new Section("Anzeige", List.of(
				new Toggle("Fullbright", "Alles hell, auch in Höhlen und nachts", () -> GuideClient.settings.fullbright, s -> s.fullbright = !s.fullbright),
				new Toggle("Licht-Anzeige", "Stellen markieren, an denen Monster spawnen können", () -> GuideClient.settings.lightOverlay, s -> s.lightOverlay = !s.lightOverlay),
				new Toggle("Koordinaten", "Koordinaten, Biom, Lichtlevel und Nether-Umrechnung", () -> GuideClient.settings.coords, s -> s.coords = !s.coords),
				new Toggle("Werkzeug-Warnung", "Warnen, bevor ein Werkzeug kaputtgeht", () -> GuideClient.settings.toolWarn, s -> s.toolWarn = !s.toolWarn),
				new Toggle("Verzauber-Tipps", "Beste Verzauberungen neben dem Inventar anzeigen", () -> GuideClient.settings.enchantHelp, s -> s.enchantHelp = !s.enchantHelp))));
	}

	public OptionsScreen() {
		super(Component.literal("Helfer"));
	}

	@Override
	protected void init() {
		placed.clear();
		headers.clear();
		notes.clear();
		w = Math.min(width - 24, MAX_W);
		int cols = w >= 3 * 110 + 8 ? 3 : 2;
		int x = (width - w) / 2;
		int colW = (w - (cols - 1) * 4) / cols;
		int y = 0;
		for (Section sec : sections()) {
			headers.add(new Header(sec.title(), y));
			y += HEADER;
			List<Toggle> list = sec.toggles();
			for (int i = 0; i < list.size(); i++) {
				Toggle t = list.get(i);
				Button b = addRenderableWidget(Button.builder(label(t), btn -> {
					t.flip().accept(GuideClient.settings);
					GuideClient.settings.save();
					btn.setMessage(label(t));
				}).bounds(x + (i % cols) * (colW + 4), 0, colW, BTN).tooltip(Tooltip.create(Component.literal(t.help()))).build());
				placed.add(new Placed(b, y + (i / cols) * ROW));
			}
			y += ((list.size() + cols - 1) / cols) * ROW + GAP;
		}
		// Tastenkonflikte scrollen als letzter Abschnitt mit, statt der Liste den Platz zu nehmen
		List<Component> conflicts = conflicts(minecraft);
		if (!conflicts.isEmpty()) {
			headers.add(new Header("Tastenkonflikte", y));
			y += HEADER;
			for (Component c : conflicts) {
				notes.add(new Note(c, y));
				y += font.split(c, w).size() * 10 + 2;
			}
			y += GAP;
		}
		contentHeight = y - GAP;

		int by = height - BTN - 6;
		int half = (w - 4) / 2;
		addRenderableWidget(Button.builder(Component.literal("Guide"), b -> minecraft.gui.setScreen(new GuideScreen()))
			.bounds(x, by, half, BTN).build());
		addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> onClose())
			.bounds(x + half + 4, by, half, BTN).build());
		bottom = by - 4;
		layout();
	}

	private int viewHeight() {
		return Math.max(0, bottom - TOP);
	}

	/** Knoepfe an die Scroll-Position setzen; was nicht ganz sichtbar ist, ist auch nicht klickbar. */
	private void layout() {
		scroll = Mth.clamp(scroll, 0, Math.max(0, contentHeight - viewHeight()));
		for (Placed p : placed) {
			int y = TOP + p.y() - (int) scroll;
			p.widget().setY(y);
			p.widget().visible = y >= TOP && y + BTN <= bottom;
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scroll -= scrollY * ROW;
		layout();
		return true;
	}

	/** Eigene Tasten, die schon eine andere Funktion (Vanilla oder andere Mod) belegt. */
	private static List<Component> conflicts(Minecraft mc) {
		List<Component> out = new ArrayList<>();
		for (KeyMapping own : mc.options.keyMappings) {
			if (!own.getName().startsWith("key.mcguide.") || own.isUnbound()) continue;
			for (KeyMapping other : mc.options.keyMappings) {
				// Paare aus zwei eigenen Tasten nur einmal melden
				// Debug-Tasten gelten nur zusammen mit F3, kollidieren also nicht wirklich
				if (other == own || !own.same(other) || other.getName().startsWith("key.debug.") || other.getName().startsWith("key.mcguide.") && other.getName().compareTo(own.getName()) < 0) continue;
				out.add(Component.literal("⚠ ").append(Component.translatable(own.getName())).append(" und ")
					.append(Component.translatable(other.getName())).append(": beide auf ").append(own.getTranslatedKeyMessage()));
			}
		}
		return out;
	}

	private static Component label(Toggle t) {
		boolean on = t.get().getAsBoolean();
		return Component.literal((on ? "✔ " : "✖ ") + t.label()).withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g, mouseX, mouseY, partialTick);
		Component title = Component.literal("⚙ Helfer").withStyle(ChatFormatting.BOLD);
		g.text(font, title, (width - font.width(title)) / 2, 8, 0xFFFFFFFF);
		Component hint = Component.literal("Zoom: C halten · Wegpunkt: N · Sortieren: ↓ · /wp · /finde");
		g.text(font, hint, (width - font.width(hint)) / 2, 19, 0xFFB0B4BC);
		int x = (width - w) / 2;
		g.enableScissor(x - 8, TOP, x + w + 8, bottom);
		for (Header h : headers) {
			int y = TOP + h.y() - (int) scroll + 2;
			g.text(font, Component.literal(h.title()).withStyle(ChatFormatting.BOLD), x, y, 0xFFFFD27F);
			g.fill(x + font.width(h.title()) + 6, y + 4, x + w, y + 5, 0x40FFFFFF);
		}
		for (Note n : notes) {
			g.textWithWordWrap(font, n.text(), x, TOP + n.y() - (int) scroll, w, 0xFFFFB040);
		}
		g.disableScissor();
		int max = contentHeight - viewHeight();
		if (max > 0) {
			// Scrollleiste rechts neben der Liste
			int bar = Math.max(16, viewHeight() * viewHeight() / contentHeight);
			int top = TOP + (int) ((viewHeight() - bar) * scroll / max);
			g.fill(x + w + 4, TOP, x + w + 6, bottom, 0x30FFFFFF);
			g.fill(x + w + 4, top, x + w + 6, top + bar, 0xC0FFFFFF);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	static void open(Minecraft mc) {
		mc.gui.setScreen(new OptionsScreen());
	}
}
