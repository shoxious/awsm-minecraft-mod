# Geländekarte der Burg (Stand 2026-11-04, abgelaufen und gescannt)

Ganze Anlage: **x -1456..-1372, z 1015..1077, y 95..139**. Außerhalb davon sind nur einzelne Fackeln.
Scans: `/bereich` in 3er-Schichten, ausgewertet nach Ebenen. Bilder vom Rundgang lagen im Scratchpad der Session.

> **Regeln beim Laufen**
> - Das **Südtor nie durchgehen** (Eisen-Doppeltür -1423/-1422, y119, z1066). Knöpfe: innen z1065, außen z1067.
> - Für Wege den Bauplan-Schritt **`lauf`** nehmen (A*-Wegsuche), nicht `goto`. `goto` läuft stur geradeaus gegen Wände,
>   dabei springt die Figur dauernd und verhungert (am 2026-11-04 bis auf 3,7 Leben).
> - Holztüren öffnet `lauf` selbst und schließt sie hinter sich. Vor der Haustür liegen innen Druckplatten, `lauf` wartet darauf.
> - Nachts ins Bett: `{"action":"lauf","pos":[-1401,129,1027]}`, dann `{"action":"schlafen","pos":[-1400,129,1027]}`.

## Holzfarm (Fichten) auf der Kuppe östlich vom Haus — gebaut 2026-11-04
- **Feld:** x -1389..-1366, z 1015..1029, Boden y125 (Stehhöhe 126). Am 2026-11-05 nach Osten erweitert.
  - Rundum eine 3 hohe Steinziegelmauer (x -1390/-1365, z 1014/1030, y126–128). Die Außenmauern sollen bleiben.
  - 17 Fackeln innen, kein Ausgang nach draußen.
  - Zurzeit leer: Die 16 Fichten wurden gefällt. Die TNT-Farm nach Schematic TheySix Nr. 502
    (`tools/generatoren/tntfarm_c.py`) wurde gebaut, getestet und auf Wunsch wieder abgebaut, weil das TNT nicht beim Baum ankam.
- **Zugang:** Hobbit-Tür (-1395,119,1025) in der Ostwand der unteren Halle.
  - Dahinter ein ebener Absatz (-1394), dann Fichtenstufen x -1393..-1388 (y119..124) hinauf aufs Feld bei (-1387,126,1025).
- **Abendsperre:** 2 Bruchsteine auf dem Absatz (-1394, 119/120, 1025). Setzt die Figur jeden Abend, nimmt sie morgens wieder weg.
- **16 Bäume:** x -1387/-1383/-1379/-1376 × z 1017/1020/1024/1027.
  - Mindestens 3 Blöcke Abstand zur Mauer, sonst wachsen Fichten nicht (sie brauchen ab 2 Höhe rundum 2 Blöcke Luft).
- **Holzrunde:** `python3 -I tools/generatoren/holzrunde.py > /tmp/holz.json && tools/mc.sh POST /plan /tmp/holz.json`
  - Ergebnis: etwa 85 Stämme pro Runde in die Kiste „Holstämme“. Die Axt verliert dabei nur etwa 20 Haltbarkeit (Unbreaking + Mending).

## Ebenen von oben nach unten

### Dach / Dachboden (y132–139)
- Tiefenschiefer-Knickdach, Zwerchgiebel.
- **Dachboden y133:** West Gästezimmer (4 Betten), Mitte Lounge, Ost 36 Fichtenfässer, gemeißeltes Bücherregal (-1446,133,1026).
- **Aufgang:** Leiter (-1409, 129..132, 1025) an der Westseite, oben eine Kirschfalltür (-1409,133,1025).

### Haus (y129, x -1447..-1397, z1023..1032)
Von West nach Ost:
- **Lager** x -1446..-1438: beschriftete Doppelkisten an Nord-, West- und Südwand, Fässer (-1443,129..131,1031).
- **Werkstatt / Zaubertisch** x -1436..-1430:
  - Zaubertisch (-1432,129,1026) mit 18 Regalen und 4 Magenta-Kerzen.
  - Amboss, Schmiedetisch.
  - Südwand: Werkbank (-1436,129,1031), Ofen, Schmelzofen, Steinsäge, Schleifstein, Webstuhl, Kartentisch.
- **Eingang** x -1425..-1420: Rüstungsgalerie.
  - Haustür ist eine Doppeltür (-1423/-1422,129,1032) mit Druckplatten innen (z1031).
- **Küche** um x -1417: Räucherofen (-1417,129,1024), Wasserkessel (-1416,129,1024), Fässer.
- **Schlafzimmer** x -1404..-1398: Bett (-1400/-1399,129,1027/1028), Jukebox, Lagerfeuer (-1405,129,1024).

### Freitreppe Haus → Terrasse → Hof
- **Haustür → Terrasse:** 9 breit (x -1427..-1419), von z1033 (y128) bis z1037 hinunter auf y124.
- **Terrasse → Hof:** 9 breit (x -1427..-1419), von z1044 (y123) bis z1048 hinunter auf y119.

### Mittlere Ebene (Stehhöhe y124)
- **Halle unter dem Haus** x -1448..-1408, z1022..1032:
  - leer, Grasboden, nach Süden zur Terrasse offen.
  - Hier soll die **Dorfbewohner-Ebene** hin, Plan in `docs/bauplaene/mitte-plan.md`.
- **Weizenkeller** x -1406..-1398, z1023..1031:
  - Feld mit Bauern-Dorfbewohnern, Komposter (-1402,125,1027).
  - Läuft schon, **nicht umbauen**.
  - Davor stehen die Berufsblöcke Braustand (-1408,124,1028) und Bognertisch (-1408,124,1030).
- **Schienen und Entladestation:** x -1415..-1408, z1031..1033, mit Trichtern, Komparatoren und 2 Kisten.
  - Schienen laufen über die Terrasse bei z1036 nach Osten.
  - Dann geht es bei x -1405 bzw. -1397 nach unten zur Halle und zum Tunnel.
- **Terrasse** z1033..1043: Grasfläche.
  - Ost-Ende: Hühnerfarm mit Glas, Lavakessel als Garer (-1402,124,1037..1039).

### Burghof (Stehhöhe y119, z1043..1066)
- Fünfeckige Steinziegelmauer mit Spitze im Süden am Südtor (z1066). Im Hof stehen Kirschbäume und Lamas.
- **Ausstattung:**
  - Enderkiste (-1424,119,1050) und Werkbank (-1425,119,1050).
  - 3 Doppelkisten für Stein und Erde bei x -1417..-1410, z1050.
  - Weitere Kisten an der Westseite (Lama-Ecke).
- **Automatische Weizenfarm** im Hof-Ost: Feld bei x -1410..-1400, z1049..1053.
  - Darunter auf y113–115 die Technik: Beobachter, Spender, Trichter.
  - Ernte-Kiste: Doppelkiste (-1406,113,1051/1052).
- **Versteckter Abstieg:** Loch (-1423,118,1046) direkt unter der Hoftreppe.
  - Leiter (-1423,117..101,1046) hinunter zum Tunnel y112.

### Untere Halle (Stehhöhe y119, x -1447..-1398, z1022..1043)
- **Zum Hof:** Hobbit-Front bei z1042/1043 mit 5 Rundtüren (x -1443/-1433/-1423/-1413/-1403) und Sprossenfenstern.
  - Die Tür bei x -1413 ist der normale Durchgang.
- **Ostwand x -1395:** 5 Rundtüren (z1025/1029/1033/1037/1041), dahinter Fels.
  - Davor liegen eine Schiene und 11 Hebel (-1402,121,1022..).
- **Knochenmehlfarm** x -1412/-1411, z1033: Filtertrichter y123 → Komposter (-1411,121,1033) → Kiste „Knochenmehl“ (-1413,119,1033).
- **Bambusfarm** x -1449..-1445, z1022..1031: Kolben und Beobachter auf y120/121, geschlossen, nicht begehbar.
  - Darunter sammelt eine Trichterlore auf y117.
- **Wasser-Aufzug** (Seelensand) bei (-1450, 114..118, 1033), von y114 hinauf zur Halle.
- Halle sonst noch leer (Steinboden). Hier sollen weitere automatische Farmen hin; **darüber (y123+) liegen Farmen, nichts abbauen**.

### Tunnel und Mine (y95–113)
- **Schienentunnel y112:**
  - verläuft von (-1433,112,1046) bei z1043/1044 nach Osten bis x -1397.
  - steigt dort bei x -1397 nach Norden auf y119 an und läuft als Schiene durch die Halle hinauf zur mittleren Ebene.
- **Bei x -1433:** führt eine Schienenrampe nach Süden ab z1047 hinunter auf y95 (z1064) und weiter in die Tiefe zur Mine.
  - Im Scan lagen dort unten **Zombies, Skelette und Creeper**. Nur mit vollem Leben hinein.
