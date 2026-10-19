# awsm-minecraft-mod

Fabric-Mod für Minecraft **26.3** (Mod-ID `mcguide`). Ein Sammelsurium an Helfern für den Alltag im Spiel. Fast alles läuft rein auf dem Client und funktioniert deshalb auf jedem Server.

> **Fair Play:** Erz-Finder, Mob-Markierung durch Wände und die Automatiken sind auf vielen Servern verboten. Nur dort benutzen, wo es erlaubt ist.

## Funktionen

| Taste | Funktion |
|---|---|
| **J** | Helfer-Menü: alle Funktionen an/aus (auch `/helfer`) |
| F6 | Kisten-Finder: Truhen, Fässer, Shulker, Spawner, Tresore durch Wände |
| F7 | Auto-Ernte: reife Feldfrüchte ernten, neu pflanzen, Knochenmehl inkl. Nachfüllen |
| F8 | Erz-Finder (`/erze`, `/erze 32`, `/erze alle`) |
| F9 | Guide (nur mit Mod auf dem Server) |
| F10 | Mobs markieren (Leuchtumriss) |
| B | Fullbright |
| C (halten) | Zoom |
| M | Licht-Anzeige: wo Monster spawnen können |
| N | Wegpunkt setzen (`/wp`, `/wp ziel <Name>`, Todespunkt automatisch) |
| U | Auto-Füttern von Nutztieren |
| R (im Inventar) | Inventar / Kiste sortieren |

Außerdem automatisch bzw. im Menü schaltbar:

- **Auto-Fäller / Ader-Abbau:** Schleichen + Axt fällt den ganzen Baum (mit Setzling), Schleichen + Spitzhacke baut die ganze Erzader ab
- **Auto-Werkzeug, Auto-Essen, Auto-Fackel** (`/fackeln draussen` für die Oberfläche), **Werkzeug-Warnung**
- **Verzauberungs-Tipps:** Infobox neben dem Inventar mit den besten Verzauberungen für das Teil unter der Maus
- **Bastion-Finder:** setzt im Nether automatisch Wegpunkte an Bastionen
- **Koordinaten-Anzeige** mit Biom, Lichtlevel und Nether-Umrechnung

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
