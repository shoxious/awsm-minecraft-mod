# Übergabe – Stand 2026-11-03

Dieses Dokument ist für die nächste Claude-Code-Session. Es beschreibt, wie man die Spielfigur über die lokale
mcguide-Schnittstelle steuert, was schon gebaut ist und was noch offen ist.

## Umgebung

- **Server:** 62.178.218.163
- **Prism-Instanz:** `26.3` (Minecraft 26.3, Fabric, Java 25)
- **Mod:** dieses Repo. Gebaut wird mit `sh gradlew build -q`, das Ergebnis `build/libs/mcguide-2.8.0.jar` kommt nach
  `~/Library/Application Support/PrismLauncher/instances/26.3/minecraft/mods/`.
- **Neustart:** Spiel-JVM beenden (`pkill -f org.prismlauncher.EntryPoint`), Jar kopieren, dann
  `".../Prism Launcher 2.app/Contents/MacOS/prismlauncher" --launch 26.3 --server 62.178.218.163`.
  Danach auf `"imSpiel": true` in `/status` warten und `POST /weiter` senden.
- **JVM-Absturz-Workaround:** In der `instance.cfg` steht `JvmArgs="-XX:+UseG1GC -XX:TieredStopAtLevel=1"`.
  Prism muss geschlossen sein, bevor man das bearbeitet.
- **Vollbild:** ist aus (`options.txt` → `fullscreen:false`), weil das Schließen des Pausenmenüs sonst zum Vollbild-Space springt.

## Lokale Schnittstelle (127.0.0.1:47115)

Der Token steht in `config/mcguide-api.json`. Mit `tools/mc.sh METHOD PFAD [BODYDATEI]` geht es am einfachsten.

| Endpunkt | Zweck |
|---|---|
| `GET /status` | Position, Blick, `leben`, `hunger`, `zeit` (0–24000), `nacht`, Inventar, Bauplan-Fortschritt, letzte Meldungen |
| `GET /bereich?von=x,y,z&bis=x,y,z` | Blöcke im Quader, max. 100k (Luft wird weggelassen) |
| `GET /wesen` | Tiere, Items am Boden, Rüstungsständer im Umkreis 80 |
| `GET /inhalt` | zuletzt notierter Kisteninhalt (Bauplan-Schritt `inhalt` frischt ihn auf) |
| `POST /plan` | Bauplan (JSON) starten |
| `POST /stop` | Bauplan sofort abbrechen |
| `POST /screenshot` | Screenshot nach `minecraft/screenshots` |

Den Bauplan-Aufbau findest du in `Blueprint.java` (`record Step`). Wichtige Aktionen:
- **Laufen:** `lauf` (pos) = Wegsuche mit A*: um Wände herum, Treppen, Leitern, Holztüren auf und hinter sich zu, Eisentüren und Lava meiden. **Immer `lauf` statt `goto` nehmen**, `goto` läuft nur geradeaus.
- **Holz:** `wachsen` (pos = Setzling, Knochenmehl bis Stamm) und `baum` (pos = Stammfuß: fällen inkl. Erdtürmchen; stoppt den Plan, wenn die Axt unter 300 Haltbarkeit fällt).
- **Ablegen gezielt:** `ablegen` mit `item` legt nur dieses Item ab (auch aus der Schnellleiste, Werkzeug bleibt).
- **Sicherheit:** In Plänen mit `nacht`/`bett` geht die Figur schon in der Dämmerung (Zeit 11500) heim und morgens per `lauf` zurück an die Arbeitsstelle. Sieht sie ein Monster in ≤ 12 Blöcken, zieht sie sich ins Haus zurück (`warten`) und macht danach weiter. Kein Kampf. `POST /weiter` belebt nach dem Tod wieder.
- **Essen:** `essen` isst Essen aus der Schnellleiste, bis der Hunger voll ist. Fällt der Hunger während eines Bauplans unter 12, isst die Figur automatisch.
- **Bauen:** `goto` (nur gerade Strecken), `place` (mit `click`, `face`, `hit` relativ zum Klickblock, `yaw`/`pitch`, neu: `"schleichen": true`, um an Truhen und Trichter anzubauen), `break`.
- **Kisten:** `holen` nimmt immer ganze Stapel. `ablegen` legt das Hauptinventar ab, mit `"sortieren": true` auch die Schnellleiste, ohne Werkzeug, Essen und Fackeln. `leeren` nimmt den Inhalt eines Behälters ins Inventar.
- **Verarbeiten:** `craft` (Raster mit 9 IDs, `count` = Anzahl Crafts pro Feld, das Ergebnis braucht freien Platz!), `use`, `ofen`, `saege`.
- **Sonstiges:** `sortieren`, `inhalt`, `klettern`, `schlafen`, `anziehen`.
- **Nachtruhe:** Mit `nacht`, `morgen` und `bett` im Plan geht die Figur nachts automatisch schlafen.

Helfer in `tools/`:
- `shot.sh x y z yaw pitch name`: hinlaufen und Screenshot machen
- `guck.sh x y z name`: Behälter öffnen, Screenshot machen, schließen
- `walk.py`: Wegsuche auf y119 anhand von Scans
- `generatoren/`: die Python-Skripte, die die Baupläne erzeugt haben

## Regeln des Nutzers (auch im Claude-Memory)

- **Material:** Es wird nur Material aus den Kisten verwendet. Was fehlt, kommt als Farm-Liste zum Nutzer. Fenster nie zubauen.
- **Kirschholz:** Es ist zu schön für Fässer und Nutzblöcke, dafür immer Fichte nehmen.
- **Nachts:** Bei Automatik nachts schlafen gehen. Betten: Haus x -1400/-1399 z1027/1028 y129, Burg (-1425,119,1047).
- **Inventar:** In der Schnellleiste liegen immer 1 Stapel Essen und 1 Stapel Fackeln, im Inventar immer Pfeile.
  Nach jedem Sortieren nachprüfen!
- **Kaputt:** Es darf nichts kaputtgehen. Über der unteren Halle (y123+) liegen Farmen.
- **Südtor:** Durch das Südtor (Eisentür z1066) **nie** hinausgehen. Holztüren werden angeklickt, Eisentüren haben Knöpfe.
- **Gelände:** Die ganze Burg ist in `docs/gelaende.md` beschrieben, mit Ebenen, Treppen, Leitern und Türen.
- **Stopp:** Wenn der Nutzer „stop“ schreibt, sofort `POST /stop`.

## Was gebaut ist

- **Haus (y129, x -1446..-1398, z1024..1031):** Lager mit beschrifteten Kisten (Sortieren über Schilder + Claude-Zuordnung,
  feste Zuordnung `config/mcguide-zuordnung.json`), Werkstatt, Küche, Wohnen, Schlafzimmer, Rüstungsgalerie,
  Zaubertisch (-1432,129,1026) mit 15 Regalen.
- **Dach:** Variante B1, japanisch, Tiefenschiefer, Zwerchgiebel.
- **Dachboden (y133):** West Gästezimmer, Mitte Lounge, Ost 36 Fichtenfässer (noch ohne Schilder). Plan: `docs/bauplaene/dachboden-plan.md`.
- **Untere Halle (y119–122, x -1447..-1398, z1022..1043):**
  - Zum Hof hin geschlossen durch eine Hobbit-Front bei z1043 mit 5 Rundtüren (Mitte x -1443/-1433/-1423/-1413/-1403) und Sprossenfenstern.
  - 5 weitere Rundtüren in der Ostwand (x -1396), dahinter ist Fels.
  - Rausgehen über die Tür x -1413, dann die Stiege (-1419,119,1049) → (-1419,124,1042) → Haustür (-1423,129,1032).
- **Knochenmehl-Farm (x -1412/-1411, z1033):** Filtertrichter (y123, 40 Samen + 4 Bruchstein) → gesperrter Trichter (y122, nach Osten)
  → freier Trichter (-1411,122) → Komposter (-1411,121) → Trichter (y120, y119) → Kiste „Knochenmehl“.
- **Kisten-Suche (neu im Mod):** Taste K bzw. `/kisten [suchwort]`, `/kisten übernehmen [umkreis]`, `/kisten vergessen`.

## Offen

1. **Zaubertisch schön machen:** Lapis/Smaragd-Boden 5×5, Laterne an Kette, Magenta-Kerzen auf den Regalen, Banner, Amboss + Schleifstein.
   Dazu 3 echte Bücherregale für die Symmetrie: rechts (-1430,129/130,1026), links (-1434,130,1026).
   Leder und Papier sind jetzt im Lager (92 Leder, 151 Papier).
2. **Geheimes Lager (Variante A):**
   - Bücherregal-Geheimtür 2×2 am Zaubertisch, zur Seite weggezogen (zweistufig, ca. 8 klebrige Kolben + 2 Beobachter).
   - Auslöser: Stolperdraht auf y131 vor dem Regal (geht beim Springen auf), Druckplatte auf der obersten Stufe von unten.
   - Wieder zu: Verzögerung mit Verstärkern.
   - Kellerlager im Fels nördlich unter dem Haus (z1010–1021). Vorher nachmessen!
3. **Stein-/Erdlager:** eigentlich hinter den Osttüren geplant (x -1394..-1388, z1022..1043). Jetzt vielleicht Teil des geheimen Lagers.
   Im Hof stehen dafür 3 Doppelkisten (x -1417..-1410, z1050) mit Stein und Erde aus dem Inventar.
4. **Dorfbewohner-Ebene (y124):** Plan `docs/bauplaene/mitte-plan.md`, Variante B, aber **ohne** Umbau der Weizenfarm (die läuft schon).
5. **Später:** Ton für 4 Blumentöpfe, 2. Enderkiste (4 Obsidian + 1 Enderperle), Lederhose für die Rüstungsgalerie,
   Schilder für die Dachboden-Fässer.
