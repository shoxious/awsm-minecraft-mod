package de.shoxi.mcguide;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.stats.Stat;

/** Fortschritt eines Spielers, gespeichert in der Welt unter mcguide/&lt;uuid&gt;.json. */
public final class GuideData {
	/** Statistikwerte beim ersten Start des Guides fuer diesen Spieler. */
	public Map<String, Integer> baseline;
	public Set<String> auto = new LinkedHashSet<>();
	public Set<String> manual = new LinkedHashSet<>();

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static String key(Stat<?> stat) {
		return stat.getName();
	}

	public boolean isDone(String id) {
		return auto.contains(id) || manual.contains(id);
	}

	/** Klick auf ein Ziel: manuell abhaken oder Haken (auch automatischen) entfernen. */
	public void toggle(String id) {
		if (manual.remove(id)) return;
		if (auto.remove(id)) return;
		manual.add(id);
	}

	public Goals.Goal next() {
		for (Goals.Phase phase : Goals.PHASES)
			for (Goals.Goal g : phase.goals())
				if (!isDone(g.id())) return g;
		return null;
	}

	static GuideData load(Path file) {
		if (Files.exists(file)) {
			try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				GuideData d = GSON.fromJson(r, GuideData.class);
				if (d != null) return d;
			} catch (IOException | RuntimeException e) {
				GuideServer.LOG.warn("{} konnte nicht gelesen werden", file, e);
			}
		}
		return new GuideData();
	}

	void save(Path file) {
		try {
			Files.createDirectories(file.getParent());
			try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				GSON.toJson(this, w);
			}
		} catch (IOException e) {
			GuideServer.LOG.warn("{} konnte nicht gespeichert werden", file, e);
		}
	}
}
