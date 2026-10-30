package de.shoxi.mcguide;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;

import net.fabricmc.loader.api.FabricLoader;

/** Client-Einstellungen (config/mcguide.json). Der Fortschritt selbst liegt auf dem Server. */
public final class GuideState {
	public boolean hud = true;
	public boolean oreFinder = false;
	public int oreRadius = 10;
	public boolean oreCoal = false;
	public boolean oreCopper = false;
	public boolean treeFeller = true;
	public boolean autoCrops = false;
	public boolean saplings = true;
	public boolean chestFinder = false;
	public boolean mobGlow = false;
	public boolean autoTool = true;
	public boolean autoEat = true;
	public boolean autoTorch = false;
	public boolean toolWarn = true;
	public boolean coords = true;
	public boolean fullbright = false;
	public boolean bastionFinder = true;
	public boolean endCityFinder = true;
	public boolean enchantHelp = true;
	public boolean autoFeed = false;
	public boolean torchOutdoor = false;
	public boolean lightOverlay = false;
	public boolean boneMeal = true;

	private static final Gson GSON = new Gson();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("mcguide.json");

	public static GuideState load() {
		if (Files.exists(FILE)) {
			try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
				GuideState s = GSON.fromJson(r, GuideState.class);
				if (s != null) return s;
			} catch (IOException | RuntimeException e) {
				GuideServer.LOG.warn("mcguide.json konnte nicht gelesen werden", e);
			}
		}
		return new GuideState();
	}

	public void save() {
		try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
			GSON.toJson(this, w);
		} catch (IOException e) {
			GuideServer.LOG.warn("mcguide.json konnte nicht gespeichert werden", e);
		}
	}
}
