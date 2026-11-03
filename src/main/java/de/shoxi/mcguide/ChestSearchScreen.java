package de.shoxi.mcguide;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Taste K bzw. /kisten [suchwort]: Suche über alle eigenen Kisten. Links die Items mit Gesamtzahl,
 * rechts die Fundorte des gewählten Items. Klick auf einen Fundort setzt einen Wegpunkt dorthin.
 */
public class ChestSearchScreen extends Screen {
	private static final int ROW = 20;
	private static final int TOP = 44;

	private final String initial;
	private EditBox search;
	private List<ChestTracker.Result> results = new ArrayList<>();
	private ChestTracker.Result selected;
	private double scrollLeft;
	private double scrollRight;
	private int listX, listW, detailX, detailW, bottom;

	public ChestSearchScreen(String initial) {
		super(Component.literal("Kisten-Suche"));
		this.initial = initial == null ? "" : initial;
	}

	static void open(Minecraft mc, String query) {
		mc.gui.setScreen(new ChestSearchScreen(query));
	}

	@Override
	protected void init() {
		int w = Math.min(width - 20, 560);
		listX = (width - w) / 2;
		listW = w * 45 / 100;
		detailX = listX + listW + 8;
		detailW = w - listW - 8;
		bottom = height - 30;
		search = new EditBox(font, listX, 22, w, 16, Component.literal("Suche"));
		search.setHint(Component.literal("Item suchen, z. B. Eisen, Fackel, Shulker …").withStyle(ChatFormatting.DARK_GRAY));
		search.setValue(search.getValue().isEmpty() ? initial : search.getValue());
		search.setResponder(s -> refresh());
		addRenderableWidget(search);
		setInitialFocus(search);
		addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> onClose()).bounds(width / 2 - 50, height - 24, 100, 18).build());
		refresh();
	}

	private void refresh() {
		results = ChestTracker.search(minecraft, search.getValue());
		selected = results.isEmpty() ? null : (selected != null && results.stream().anyMatch(r -> r.id().equals(selected.id()))
			? results.stream().filter(r -> r.id().equals(selected.id())).findFirst().get() : results.get(0));
		scrollLeft = 0;
		scrollRight = 0;
	}

	@Override
	public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x(), my = event.y();
		if (my >= TOP && my < bottom) {
			if (mx >= listX && mx < listX + listW) {
				int i = (int) ((my - TOP + scrollLeft) / ROW);
				if (i >= 0 && i < results.size()) {
					selected = results.get(i);
					scrollRight = 0;
					return true;
				}
			} else if (mx >= detailX && mx < detailX + detailW && selected != null) {
				int i = (int) ((my - TOP - 14 + scrollRight) / 12);
				if (my >= TOP + 14 && i >= 0 && i < selected.where().size()) {
					ChestTracker.Box b = selected.where().get(i).box();
					if (!b.dim.isEmpty()) {
						Waypoints.point(minecraft, "Kiste: " + ChestTracker.name(selected.id()), b.x, b.y, b.z, b.dim);
						onClose();
					}
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (mouseX < detailX) {
			scrollLeft = Mth.clamp(scrollLeft - scrollY * ROW, 0, Math.max(0, results.size() * ROW - (bottom - TOP)));
		} else if (selected != null) {
			scrollRight = Mth.clamp(scrollRight - scrollY * 12, 0, Math.max(0, selected.where().size() * 12 + 14 - (bottom - TOP)));
		}
		return true;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g, mouseX, mouseY, partialTick);
		int n = ChestTracker.count(minecraft), un = ChestTracker.unopened(minecraft);
		String head = "🔍 Kisten-Suche · " + n + " eigene Behälter" + (un > 0 ? " (" + un + " noch nie geöffnet)" : "");
		g.text(font, Component.literal(head).withStyle(ChatFormatting.BOLD), listX, 8, 0xFFFFFFFF);

		// links: Items
		g.fill(listX, TOP, listX + listW, bottom, 0x60000000);
		g.enableScissor(listX, TOP, listX + listW, bottom);
		for (int i = 0; i < results.size(); i++) {
			int y = TOP + i * ROW - (int) scrollLeft;
			if (y + ROW < TOP || y > bottom) continue;
			ChestTracker.Result r = results.get(i);
			boolean sel = r == selected;
			boolean hover = mouseX >= listX && mouseX < listX + listW && mouseY >= y && mouseY < y + ROW && mouseY >= TOP && mouseY < bottom;
			if (sel || hover) g.fill(listX, y, listX + listW, y + ROW, sel ? 0x704080FF : 0x30FFFFFF);
			g.item(r.icon(), listX + 2, y + 2);
			String count = fmt(r.total());
			g.text(font, count, listX + listW - 4 - font.width(count), y + 6, 0xFFFFD27F);
			String name = font.plainSubstrByWidth(ChestTracker.name(r.id()), listW - 30 - font.width(count));
			g.text(font, name, listX + 22, y + 6, 0xFFFFFFFF);
		}
		if (results.isEmpty()) {
			g.textWithWordWrap(font, Component.literal(n == 0
				? "Noch keine eigenen Kisten bekannt. Neu aufgestellte Kisten werden automatisch erfasst; bestehende Kisten mit /kisten übernehmen (im Lager stehen). Inhalte werden beim Öffnen gemerkt."
				: "Nichts gefunden.").withStyle(ChatFormatting.GRAY), listX + 4, TOP + 4, listW - 8, 0xFFB0B4BC);
		}
		g.disableScissor();

		// rechts: Fundorte
		g.fill(detailX, TOP, detailX + detailW, bottom, 0x60000000);
		if (selected != null) {
			g.enableScissor(detailX, TOP, detailX + detailW, bottom);
			int y0 = TOP + 2 - (int) scrollRight;
			g.text(font, Component.literal(ChestTracker.name(selected.id()) + ": " + fmt(selected.total()) + " in " + selected.where().size()
				+ (selected.where().size() == 1 ? " Behälter" : " Behältern")).withStyle(ChatFormatting.BOLD), detailX + 4, y0, 0xFFFFD27F);
			for (int i = 0; i < selected.where().size(); i++) {
				int y = y0 + 14 + i * 12;
				ChestTracker.Hit h = selected.where().get(i);
				boolean hover = mouseX >= detailX && mouseX < detailX + detailW && mouseY >= y && mouseY < y + 12 && mouseY >= TOP && mouseY < bottom;
				if (hover) g.fill(detailX, y - 1, detailX + detailW, y + 11, 0x30FFFFFF);
				String c = fmt(h.count());
				g.text(font, c, detailX + 4, y, 0xFFFFFFFF);
				g.text(font, font.plainSubstrByWidth(ChestTracker.where(h.box()) + dist(h.box()), detailW - 50), detailX + 46, y, 0xFFB0B4BC);
			}
			g.disableScissor();
		}
		Component hint = Component.literal("Klick auf einen Fundort = Wegpunkt dorthin · Inhalte aktualisieren sich beim Öffnen der Kiste");
		g.text(font, hint, (width - font.width(hint)) / 2, bottom + 2, 0xFF808890);
	}

	private String dist(ChestTracker.Box b) {
		if (b.dim.isEmpty() || minecraft.player == null || minecraft.level == null || !b.dim.equals(minecraft.level.dimension().identifier().getPath())) return "";
		double dx = b.x + 0.5 - minecraft.player.getX(), dz = b.z + 0.5 - minecraft.player.getZ();
		return " · " + (int) Math.sqrt(dx * dx + dz * dz) + " m";
	}

	/** 1234 -> "1234 (19 St. + 18)" bei großen Mengen. */
	private static String fmt(int n) {
		return n >= 64 ? n + " (" + n / 64 + "×64" + (n % 64 > 0 ? "+" + n % 64 : "") + ")" : String.valueOf(n);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
