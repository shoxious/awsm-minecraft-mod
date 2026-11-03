# awsm-minecraft-mod

Fabric-Mod für Minecraft **26.3** (Mod-ID `mcguide`). Ein Sammelsurium an Helfern für den Alltag im Spiel. Fast alles läuft rein auf dem Client und funktioniert deshalb auf jedem Server.

> **Fair Play:** Erz-Finder, Mob-Markierung durch Wände und die Automatiken sind auf vielen Servern verboten. Nur dort benutzen, wo es erlaubt ist.

## Funktionen

Fast alles schaltest du im **Helfer-Menü (Taste J** oder `/helfer`) an und aus – ganz ohne F-Tasten, praktisch am Mac. Die Schalter haben ab Werk keine eigene Taste, damit sie sich nicht mit anderen Mods beißen; wer will, legt sie unter Optionen → Steuerung selbst auf eine Taste. Belegt sich eine Mod-Taste doch mit etwas anderem, zeigt das Menü J eine Warnung.

| Taste | Funktion |
|---|---|
| **J** | Helfer-Menü: alle Funktionen an/aus, Guide öffnen |
| C (halten) | Zoom |
| N | Wegpunkt setzen (`/wp`, `/wp ziel <Name>`, Todespunkt automatisch) |
| ↓ Pfeil runter (im Inventar) | Inventar / Kiste sortieren: gleiche Items zusammenlegen, Reihenfolge wie im Kreativ-Inventar |

Im Menü J:

- **Loot-Finder:** nur Generiertes, nichts von Spielern Platziertes – ungeöffnete Loot-Kisten, -Fässer, -Krüge und Kistenloren (braucht die Mod auf dem Server bzw. Einzelspieler), dazu immer Spawner, Tresore, verdächtiger Sand/Kies und herumliegende Items
- **Erz-Finder** (`/erze`, `/erze 32`, `/erze alle`)
- **Auto-Ernte:** reife Feldfrüchte ernten, neu pflanzen, Knochenmehl inkl. Nachfüllen
- **Mobs markieren** (Leuchtumriss), **Fullbright**, **Licht-Anzeige** (wo Monster spawnen können), **Auto-Füttern** von Nutztieren
- **Guide** (nur mit Mod auf dem Server)

Außerdem automatisch bzw. im Menü schaltbar:

- **Auto-Fäller / Ader-Abbau:** Schleichen + Axt fällt den ganzen Baum (mit Setzling), Schleichen + Spitzhacke baut die ganze Erzader ab
- **Auto-Handel:** verkauft im Handelsfenster automatisch Kürbisse, Melonen, Weizen, Karotten, Kartoffeln, Rote Bete und Federn gegen Smaragde (im Menü J einschalten)
- **Umzugs-Modus:** Kisten in Shulkerkisten umladen, ohne Server-Mod: Kiste/Fass öffnen → bis zu 27 Stapel wandern ins Inventar; dann eine aufgestellte leere Shulkerkiste öffnen → genau diese Sachen wandern hinein. Shulker abbauen, nächste aufstellen, nächste Kiste (im Menü J oder mit `/umzug` umschalten)
- **Kisten sortieren** (`/sortieren`): ordnet alle Kisten und Fässer in Reichweite komplett neu nach Material (Reihenfolge wie im Kreativ-Inventar, oben links beginnend aus deiner Blickrichtung). Öffnet die Kisten selbst, Hauptinventar muss leer sein, Schnellleiste bleibt unberührt. Nochmal `/sortieren` oder Esc bricht ab
  - **Mit Schildern:** Hängt an einer Kiste ein Schild (oder steht eines darauf), z. B. „Redstone Zeug“, „Holz“, „Essen“, „Sonstiges“, dann räumt `/sortieren` alles Passende aus den Kisten in Reichweite *und* aus dem Hauptinventar dort hinein. Unbeschriftete Kisten werden geleert, soweit es eine passende Kiste gibt; was nirgends passt, bleibt liegen. Ist Claude Code installiert und angemeldet (Befehl `claude`), entscheidet Claude über `claude -p`, was zu welchem Schild gehört (kein API-Key nötig), sonst eingebaute Stichwort-Regeln. Zuordnungen werden in `config/mcguide-kategorien.json` gemerkt, nur neue Items werden nachgefragt. `/sortieren vergessen` löscht die Merkliste
- **Lager planen** (`/lager`): Leere Kisten aufstellen, Schilder in die Schnellleiste nehmen, dazwischen stellen und `/lager` eingeben. Die Mod schaut sich alle Kisten in Reichweite und das Inventar an, Claude Code entwirft daraus Kategorien (wie viele Kisten je Kategorie, Zusammengehöriges nebeneinander, Häufiges auf Augenhöhe), die Mod bringt die Schilder an und sortiert anschließend alles ein. Vorhandene Schilder bleiben. Braucht Claude Code (Befehl `claude`, angemeldet)
- **Auto-Werkzeug, Auto-Essen, Auto-Fackel** (`/fackeln draussen` für die Oberfläche), **Werkzeug-Warnung**
- **Verzauberungs-Tipps:** Infobox neben dem Inventar mit den besten Verzauberungen für das Teil unter der Maus
- **Bastion-Finder:** setzt im Nether automatisch Wegpunkte an Bastionen
- **Bauwerk-Finder** (`/finde aussenposten`, `/finde anwesen`, `/finde allay` = was näher ist): Wegpunkt zum nächsten Plünderer-Außenposten bzw. Waldanwesen – dort sitzen Allays (braucht die Mod auf dem Server bzw. Einzelspieler)
- **Außenposten-Finder:** erkennt Außenposten und Waldanwesen in Sichtweite an ihren Blöcken (Birkenbretter + Schwarzeichenstamm + Bruchstein) und setzt einen Wegpunkt – rein clientseitig, ohne Server-Mod
- **Allay-Finder:** Allays in Sichtweite leuchten türkis (auch durch Wände), beim ersten Sichten wird ein Wegpunkt gesetzt
- **Gelände-Modus** (`/gelaende`, Größe `/gelaende 1`–`4`, Form `/gelaende huegel` | `flach`): ein gesetzter voller Block (Erde, Stein, Sand …) wird oben auf dem Boden zu einem runden, leicht unregelmäßigen Hügel (bei abfallendem Boden bis unten aufgefüllt), flach bzw. an Wänden zu einer runden Lage
- **Koordinaten-Anzeige** mit Biom, Lichtlevel und Nether-Umrechnung
- **Kisten-Suche** (Taste `K` oder `/kisten [suchwort]`): merkt sich pro Server den Inhalt aller *eigenen* Kisten, Fässer, Shulker und der Enderkiste – selbst aufgestellte werden automatisch erfasst, schon stehende mit `/kisten übernehmen [umkreis]`. Inhalte werden bei jedem Öffnen aktualisiert (Shulker-Inhalte in Kisten zählen mit), abgebaute Kisten verschwinden. Suchfenster zeigt Gesamtmenge und alle Fundorte; Klick auf einen Fundort setzt einen Wegpunkt. `/kisten vergessen` löscht das Gedächtnis.

Mit der Mod auch auf dem **Server**: Guide mit Fortschritt pro Spieler (`/guide`) und Riesenbäume aus quadratischen Setzlingsfeldern.

## Installation

1. Fabric Loader ≥ 0.19.5 und Fabric API für 26.3 installieren
2. Die `.jar` aus den Releases in den `mods`-Ordner legen

## Bauen

Java 25 wird benötigt.

```sh
./gradlew build
```

Die fertige Mod liegt danach in `build/libs/`.

## AFK-Bot-Modus

Mit der JVM-Option `-Dmcguide.bot=host:port` verbindet sich der Client beim Start automatisch mit dem Server, bewegt sich alle paar Minuten minimal, steht nach dem Tod wieder auf und beendet sich bei jeder Trennung. Gedacht für einen Headless-Client (z. B. HeadlessMC) unter einem Supervisor, der entscheidet, wann der Bot online sein soll.

## Lizenz

CC0-1.0
