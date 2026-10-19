package de.shoxi.mcguide;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Der Notizblock: alle Phasen, erledigte Ziele durchgestrichen, Klick hakt manuell ab. */
public class GuideScreen extends Screen {
	private static final int W = 300;
	private static final int LINE = 12;
	private static final int TOP = 70;
	private static final int BOTTOM = 58;
	private static final int[] RADII = {6, 10, 16, 24};

	private record Row(Goals.Goal goal, String header, int done, int total) {}

	private final List<Row> rows = new ArrayList<>();
	private double scroll;

	public GuideScreen() {
		super(Component.literal("Minecraft-Guide"));
	}

	@Override
	protected void init() {
		int x = (width - W) / 2;
		GuideState s = GuideClient.settings;
		int w3 = (W - 4) / 3;
		addRenderableWidget(Button.builder(oreLabel(), b -> {
			OreFinder.toggle(Minecraft.getInstance());
			b.setMessage(oreLabel());
		}).bounds(x, height - 50, w3, 20).build());
		addRenderableWidget(Button.builder(radiusLabel(), b -> {
			int i = 0;
			while (i < RADII.length && RADII[i] <= s.oreRadius) i++;
			s.oreRadius = RADII[i % RADII.length];
			s.save();
			b.setMessage(radiusLabel());
		}).bounds(x + w3 + 2, height - 50, w3, 20).build());
		addRenderableWidget(Button.builder(extraLabel(), b -> {
			s.oreCoal = s.oreCopper = !(s.oreCoal || s.oreCopper);
			s.save();
			b.setMessage(extraLabel());
		}).bounds(x + 2 * (w3 + 2), height - 50, w3, 20).build());
		addRenderableWidget(Button.builder(hudLabel(), b -> {
			s.hud = !s.hud;
			s.save();
			b.setMessage(hudLabel());
		}).bounds(x, height - 26, W / 2 - 2, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Schließen (F9)"), b -> onClose())
			.bounds(x + W / 2 + 2, height - 26, W / 2 - 2, 20).build());
	}

	private static Component oreLabel() {
		return Component.literal("Erz-Finder (F8): " + (GuideClient.settings.oreFinder ? "an" : "aus"));
	}

	private static Component radiusLabel() {
		return Component.literal("Umkreis: " + GuideClient.settings.oreRadius);
	}

	private static Component extraLabel() {
		return Component.literal("Kohle/Kupfer: " + (GuideClient.settings.oreCoal ? "an" : "aus"));
	}

	private static Component hudLabel() {
		return Component.literal("Hinweis im Spiel: " + (GuideClient.settings.hud ? "an" : "aus"));
	}

	private void buildRows() {
		rows.clear();
		for (Goals.Phase phase : Goals.PHASES) {
			int done = 0;
			for (Goals.Goal g : phase.goals()) if (GuideClient.isDone(g.id())) done++;
			rows.add(new Row(null, phase.title(), done, phase.goals().size()));
			for (Goals.Goal g : phase.goals()) rows.add(new Row(g, null, 0, 0));
		}
	}

	private int listHeight() {
		return height - TOP - BOTTOM;
	}

	private int contentHeight() {
		int h = 0;
		for (Row r : rows) h += r.header() != null ? LINE + 6 : LINE;
		return h;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g, mouseX, mouseY, partialTick);
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		buildRows();

		int x = (width - W) / 2;
		g.fill(x - 8, 6, x + W + 8, height - 54, 0xC0101215);
		g.text(font, Component.literal("⛏ Minecraft-Guide").withStyle(ChatFormatting.BOLD), x, 12, 0xFFFFFFFF);
		Goals.Goal next = GuideClient.next();
		String hint = !GuideClient.synced ? "Dieser Server hat den Guide-Mod nicht."
			: next == null ? "Alles geschafft!" : "Als Nächstes: " + next.text();
		g.text(font, Component.literal(hint), x, 26, 0xFF7FD67F);
		if (GuideClient.synced && next != null) {
			g.textWithWordWrap(font, Component.literal(Goals.howTo(next)), x, 38, W, 0xFFB0B4BC);
		}

		scroll = Mth.clamp(scroll, 0, Math.max(0, contentHeight() - listHeight()));
		g.enableScissor(x - 8, TOP, x + W + 8, TOP + listHeight());
		int y = TOP - (int) scroll;
		for (Row r : rows) {
			if (r.header() != null) {
				y += 6;
				boolean complete = r.done() == r.total();
				g.text(font, Component.literal(r.header()).withStyle(ChatFormatting.BOLD), x, y, complete ? 0xFF8A8F98 : 0xFFFFFFFF);
				String count = r.done() + "/" + r.total();
				g.text(font, count, x + W - font.width(count), y, complete ? 0xFF5FB760 : 0xFF8A8F98);
			} else {
				boolean done = GuideClient.isDone(r.goal().id());
				boolean hover = mouseX >= x && mouseX <= x + W && mouseY >= y - 1 && mouseY < y + LINE - 1
					&& mouseY >= TOP && mouseY < TOP + listHeight();
				if (hover) g.fill(x - 2, y - 2, x + W, y + LINE - 2, 0x30FFFFFF);
				Component text = done
					? Component.literal("☑ " + r.goal().text()).withStyle(ChatFormatting.STRIKETHROUGH)
					: Component.literal("☐ " + r.goal().text());
				g.text(font, text, x + 6, y, done ? 0xFF7B7F88 : 0xFFE8E6E3);
			}
			y += LINE;
		}
		g.disableScissor();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true;
		Minecraft mc = Minecraft.getInstance();
		int x = (width - W) / 2;
		double mx = event.x(), my = event.y();
		if (mc.player == null || !GuideClient.synced || mx < x || mx > x + W || my < TOP || my >= TOP + listHeight()) return false;
		int y = TOP - (int) scroll;
		for (Row r : rows) {
			if (r.header() != null) {
				y += 6;
			} else if (my >= y - 1 && my < y + LINE - 1) {
				GuideClient.toggle(r.goal().id());
				return true;
			}
			y += LINE;
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scroll -= scrollY * LINE * 2;
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (GuideClient.openKeyMatches(event)) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
