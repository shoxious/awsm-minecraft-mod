# mcguide – Hinweise für Claude

Fabric-Mod für Minecraft 26.3 mit lokaler Schnittstelle, über die Claude die Spielfigur steuert.

- **Zuerst lesen:** `docs/HANDOFF.md` (Server, Start, Schnittstelle, Stand) und `docs/gelaende.md` (Wege, Burg, Farmen).
- **Schnittstelle:** `tools/mc.sh METHOD PFAD [BODYDATEI]`; URL und Token stehen in
  `<Minecraft-Instanz>/config/mcguide-api.json` (wird beim Spielstart erzeugt, nie einchecken).
- **Baupläne:** Generatoren in `tools/generatoren/`, fertige Pläne in `docs/bauplaene/`.

## Feste Regeln (Details in `claude/memory/`)
- Nie durchs Südtor der Burg (Eisentür z1066) und nie zur Haustür hinaus.
- Materialien nur aus den Kisten; Fehlendes als Farm-Liste melden. Fenster nie zubauen.
- Nachts bei Automatik ins Haus schlafen gehen.
- Immer 1 Stapel Essen + 1 Stapel Fackeln in der Leiste, Pfeile im Inventar.
- Fässer und andere Nutzblöcke aus Fichte, nie aus Kirschholz.
- Wege mit `lauf`, nicht mit `goto`.
- Neue Abläufe erst beaufsichtigt testen; bei Automatik nichts Wertvolles mitnehmen.
- Bei „stop“ sofort anhalten. Commits nur auf Nachfrage.

## Auf einem neuen Rechner
1. Repo klonen, `./gradlew build`, JAR aus `build/libs/` in den `mods`-Ordner der Prism-Instanz 26.3.
2. `./claude/setup.sh` ausführen, damit Claude Code die Erinnerungen kennt.
