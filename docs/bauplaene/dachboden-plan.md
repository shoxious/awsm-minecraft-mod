# Dachboden-Plan „Kirschblüten-Dachstube“ (Java 26.3, Vanilla)

Koordinaten: Man steht auf y133, d. h. alle Bodenmöbel stehen **auf y133**. Nutzfläche x -1446..-1399 (West→Ost), z 1024 (Nord) .. 1031 (Süd).

## 1. Ideen

1. **Gäste- & Lesestube**: Betten unter der Nordschräge, Leseecke mit Sofa am Westfenster, Teppiche, Kerzen. Sehr wohnlich, braucht aber nur einen Teil der 48 Blöcke.
2. **Ordnungs-Lager**: eine durchgehende Fassleiste in den 1 Block hohen Traufreihen z1024/z1031, beschriftet, nach Kategorien sortiert. Bis zu ca. 90 Fässer möglich, praktisch neben der Luke.
3. **Aussichts-Lounge & Schatzleiste**: unter dem Türgiebel (mehr Kopfhöhe) ein Sofa vor dem Südfenster, an der Nordschräge Smaragd- und Lapisblöcke als glänzende Sockelleiste mit Kerzen.

**Empfehlung: alle drei Ideen kombinieren, in drei Zonen.**
- A West (x -1446..-1428): Gäste- & Lesestube
- B Mitte (x -1427..-1418, unter dem Türgiebel): Lounge & Schatzleiste
- C Ost (x -1417..-1399, mit Luke): Ordnungs-Lager

Grundregel für die Höhen: Die Reihen mit 1 Block Höhe (z1024 und z1031) bekommen Fässer. Die 2 Blöcke hohen Reihen (z1025 und z1030) bleiben Gang vor den Fässern bzw. bekommen niedrige Möbel. In der Mitte (z1027/z1028, 4 Blöcke hoch) läuft ein 2 breiter rosa Teppich als Laufweg über die ganze Länge.

## 2. Grundriss

Die Zahl hinter z ist die Kopfhöhe in Blöcken. `*` in der Zeile „Lat.y136“ = Laterne hängt dort bei z1027, y136 direkt unter dem First.

```
### Zone A West: Gaeste- & Lesestube  (x -1446 .. -1428)
        6543210987654321098   <- letzte Ziffer von |x|
Lat.y136   *     *     *   
z1024 1 FFFFFWWWWWFFFFFFFFF
z1025 2 .....NbNbN.........
z1026 3 Rmmm..b.b..........
z1027 4 Smmmppppppppppppppp
z1028 4 Smmmppppppppppppppp
z1029 3 Tmmm...............
z1030 2 ...................
z1031 1 FFFFFFFFFFFFFUFFFFF

### Zone B Mitte: Aussichts-Lounge & Schatzleiste  (x -1427 .. -1418)
        7654321098   <- letzte Ziffer von |x|
Lat.y136  *    *  
z1024 1 ELELELELEL
z1025 2 .e.l..e.l.
z1026 3 ..........
z1027 4 pppppppppp
z1028 4 pppppppppp
z1029 3 ..mmmmmm..
z1030 2 ..ASSSSA..
z1031 1 FF.O..O.FF

### Zone C Ost: Ordnungs-Lager & Luke  (x -1417 .. -1399)
        7654321098765432109   <- letzte Ziffer von |x|
Lat.y136   *     *     *   
z1024 1 FFFFFFF...FFFFFFFFF
z1025 2 ........H..........
z1026 3 ........p.........C
z1027 4 ppppppppppppppppppp
z1028 4 ppppppppppppppppppp
z1029 3 ..................Z
z1030 2 ...................
z1031 1 FFFFFFFFFFFFFFFFFFF```

**Legende**
| Zeichen | Block | Hinweis |
|---|---|---|
| F | Fass (y133) | Beschriftung siehe Abschnitt 3 |
| W | rosa Wolle | Bett-Kopfteil |
| N | Fass als Nachttisch + 1 rosa Kerze auf y134 | |
| b | rosa Bett | Kopfende z1025, Fußende z1026, Blickrichtung Süd |
| R | gemeißeltes Bücherregal (Kirsche) | mit den 5 Büchern |
| S | Kirschtreppe als Sofa | A: Rückenlehne West; B: Rückenlehne Nord |
| T | obere Kirsch-Stufe als Tisch + 3 rosa Kerzen | |
| A | Kirsch-Falltür offen als Armlehne | |
| m | Magenta-Teppich | |
| p | rosa Teppich | Laufweg |
| E / L | Smaragd- / Lapisblock | Sockelleiste |
| e / l | Smaragd- / Lapisblock als Sockel + 1 Kerze auf y134 | |
| O | Laterne auf dem Boden | neben dem Südfenster |
| U | Webstuhl | Hobby: Banner gestalten |
| C | Werkbank | |
| Z | Fass „Zwischenablage“ | |
| H | Luke | **frei lassen** |
| . | frei | |

## 3. Blockliste (exakt)

**Zone A West**
- Fässer z1024: x -1446..-1442 und -1436..-1428 → **14**
- Rosa Wolle z1024: x -1441..-1437 → **5**
- Nachttisch-Fässer z1025: x -1441, -1439, -1437 → **3**, darauf je 1 rosa Kerze (y134)
- Rosa Betten: x -1440 und x -1438, jeweils z1025 (Kopf) + z1026 (Fuß) → **2**
- Leseecke:
  - Bücherregal (-1446,133,1026)
  - Sofa: Treppen (-1446,133,1027) und (-1446,133,1028), Rückenlehne West
  - Tisch: obere Stufe (-1446,133,1029) mit 3 rosa Kerzen
  - Magenta-Teppich x -1445..-1443, z1026..1029 → **12**
- Bilder: (-1446,135,1026) und (-1446,135,1029) an der Westgiebelwand → **2** (kleines 1×1-Motiv; neu setzen, bis eines gefällt)
- Fässer z1031: x -1446..-1428 außer -1433 → **18**; Webstuhl (-1433,133,1031)

**Zone B Mitte**
- z1024: x -1427, -1425, -1423, -1421, -1419 Smaragd, x -1426, -1424, -1422, -1420, -1418 Lapis → **5 + 5**
- Sockel z1025: Smaragd bei -1426 und -1421, Lapis bei -1424 und -1419, darauf je 1 Kerze (naturfarben) → **2 + 2**, 4 Kerzen
- Lounge:
  - Sofa: Treppen x -1424..-1421 auf z1030, Rückenlehne Nord, man schaut zum Südfenster → **4**
  - Armlehnen: offene Falltüren auf (-1425,133,1030) und (-1420,133,1030)
  - Magenta-Teppich x -1425..-1420 auf z1029 → **6**
  - Bodenlaternen (-1424,133,1031) und (-1421,133,1031); x -1423/-1422 bleiben vor dem Fenster frei
- Fässer z1031: x -1427, -1426, -1419, -1418 → **4**. Wenn dort unter dem Türgiebel keine Dachtreppe auf y134 liegt: Standschild direkt aufs Fass setzen.

**Zone C Ost**
- Fässer z1024: x -1417..-1411 und -1407..-1399 → **16**. x -1410..-1408 bleibt frei (Ankunft an der Luke).
- Fässer z1031: x -1417..-1399 → **19**
- Luke (-1409,1025) frei lassen. Rosa Teppich auf (-1409,133,1026) als Anschluss an den Laufweg.
- Ostfenster:
  - Werkbank (-1399,133,1026)
  - Fass „Zwischenablage“ (-1399,133,1029)
  - z1027/1028 bleiben frei zum Rausschauen
- Banner: Magenta-Banner an der Ostgiebelwand bei (-1399,135,1026) und (-1399,135,1029) → **2**
- Wegweiser-Schild (-1410,134,1025) mit „← Gäste | Lounge | Lager →“

**Laufweg**
- Rosa Teppich auf z1027 und z1028 von x -1442 bis -1399 (88) + 1 Anschluss an der Luke → **89**

**Beschriftung (ordentlich)**
- Für jedes Fass in z1024 kommt 1 Wandschild auf (x,134,1025), angebracht an der Südseite der Dachtreppe darüber.
- Für jedes Fass in z1031 kommt 1 Wandschild auf (x,134,1030).
- Die Schilder schweben genau über dem Fass, haben keine Kollision und sind von der Mitte aus lesbar.
- Anzahl: 30 + 41 = **71**, plus 1 Wegweiser = **72**.
- Vorschlag für die Belegung in Zone C, von West nach Ost: Holz | Stein | Erze/Metall | Redstone | Farbstoffe/Wolle | Essen | Pflanzen/Setzlinge | Mob-Drops | Werkzeug/Verzaubern.
- Zone A: Bettwäsche, Deko, saisonale Dinge.
- Fallback, falls ein Wandschild nicht hält: Kirsch-Hängeschild an derselben Stelle, unter die Dachtreppe y135 gehängt.

**Beleuchtung**
- Die 6 Fackeln (z1027 bei x -1443, -1435, -1427, -1418, -1410, -1402) **entfernen**.
- Stattdessen **8 Laternen** direkt unter die Firsttreppe hängen, bei y136 und z1027, auf x **-1443, -1437, -1431, -1425, -1420, -1414, -1408, -1402**. Darunter bleiben 3 Blöcke frei, man stößt nirgends an.
- Dazu 2 Bodenlaternen in der Lounge und 10 Kerzen.
- Lichtberechnung: Im ungünstigsten Fall (alle Möbel als lichtundurchlässig gezählt) hat jedes begehbare Bodenfeld mindestens **Lichtstufe 6**, die meisten 8–14.
- Monster spawnen in Java nur bei Blocklicht 0, also ist der ganze Dachboden spawnsicher.

## 4. Craft-Liste

| Ergebnis | Rezept | Rohstoff |
|---|---|---|
| 75 Fässer | je 6 Bretter + 2 Stufen → 75×6 = 450 Bretter, 150 Stufen (25×3 Bretter = 75) | **525 Fichtenbretter = 132 Fichtenstämme** |
| 8 Kirschtreppen (6 nötig) | 2× (6 Bretter → 4) | 12 Kirschbretter |
| 2 Kirschfalltüren | 6 Bretter → 2 | 6 Kirschbretter |
| 6 Kirschstufen (4 nötig: 3 Regal, 1 Tisch) | 3 Bretter → 6 | 3 Kirschbretter |
| 1 gemeißeltes Bücherregal | 6 Bretter + 3 Stufen | 6 Kirschbretter |
| 1 Webstuhl | 2 Faden + 2 Bretter | 2 Kirschbretter, 2 Faden |
| 1 Werkbank | 4 Bretter | 4 Kirschbretter |
| 2 Betten → rosa | je 3 weiße Wolle + 3 Bretter, dann + 1 rosa Farbstoff | 6 Wolle, 6 Kirschbretter, 2 rosa |
| **Summe Kirschbretter: 39** | 1 Stamm → 4 Bretter | **10 Kirschstämme** |
| 26 rosa Farbstoff (25 nötig) | 13× (Rot + Weiß → 2 Rosa) | 13 Rot, 13 Weiß |
| 96 rosa Teppiche (89 nötig) | 32× (2 Wolle → 3 Teppiche) = 96 weiß, dann 12× (8 Teppiche + 1 Rosa) | 64 Wolle, 12 rosa |
| 5 rosa Wolle | Wolle + Rosa | 5 Wolle, 5 rosa |
| 24 Magenta-Wolle | Wolle + Magenta | 24 Wolle, 24 Magenta |
| 18 Magenta-Teppiche | 6× (2 Magenta-Wolle → 3) | 12 Magenta-Wolle |
| 2 Magenta-Banner | je 6 Magenta-Wolle + 1 Stock | 12 Magenta-Wolle, 2 Stöcke |
| 2 Gemälde | je 8 Stöcke + 1 Wolle | 16 Stöcke, 2 Wolle |
| 10 Kerzen (6 → rosa) | je 1 Faden + 1 Wabe; Kerze + Rosa → rosa Kerze | 10 Faden, 10 Waben, 6 rosa |
| 10 Laternen | je 8 Eisennuggets + 1 Fackel → 80 Nuggets = 9 Barren | **1 Eisenblock**, 10 Fackeln |
| (optional) Schilder wachsen | Schild + Wabe | bis zu 45 Waben übrig |

## 5. Material-Abgleich

| Material | Bedarf | Vorrat | Ergebnis |
|---|---|---|---|
| Fichtenstämme | 132 | 384 | ✔ |
| Kirschstämme | 10 | ~860 | ✔ |
| Wolle weiß | 6+64+5+24+2 = **101** | ~120 (+ Faden 300 → bis 75 weitere) | ✔ knapp, Reserve über Faden |
| Rot / Weiß Farbstoff | 13 / 13 | 58 / 19 | ✔ |
| Magenta | 24 | 90 | ✔ |
| Faden | 12 | ~300 | ✔ |
| Waben | 10 (+ Wachs optional) | ~55 | ✔ |
| Stöcke | 18 | ~460 | ✔ |
| Eisenblöcke | 1 | ~440 | ✔ |
| Smaragd- / Lapisblöcke | 7 / 7 | 77 / 71 | ✔ |
| Bücher | 5 | 5 | ✔ |
| Schilder | 72 | viele | ✔ |
| Fackeln | 10 | viele (+6 zurück) | ✔ |

**Farm-Liste**
- Nur nötig, wenn **kein Feuerzeug** da ist: **1 Feuerstein** aus Kies (für Feuerzeug = Eisenbarren + Feuerstein). Ohne Anzünden bleiben die Kerzen aus; das Licht reicht trotzdem.
- Optional: ca. 30 Waben, um alle 72 Schilder zu wachsen (Schutz vor versehentlichem Bearbeiten).
- Optional für später: Bücher (Zuckerrohr + Leder) für weitere Bücherregale oder ein Lesepult.

## 6. Bau-Reihenfolge

1. Die Luke (-1409,1025) und den Bereich z1024–1026 bei x -1410..-1408 als Sperrfläche merken. Dort wird nichts abgestellt, auch kein Material.
2. Zuerst alles craften (Abschnitt 4) und nach Zonen sortiert in Kisten bzw. ins Inventar packen.
3. **Licht zuerst**: die 8 Laternen unter den First hängen, **danach** die 6 Fackeln abbauen. So ist es zu keinem Zeitpunkt dunkel.
4. Fassleisten z1024/z1031 setzen, Zone für Zone von West nach Ost. In Zone C die Lücke x -1410..-1408 auf z1024 auslassen.
5. Zone B: Smaragd/Lapis-Leiste, Sockel, Sofa, Armlehnen, Bodenlaternen.
6. Zone A: Wolle-Kopfteil, Nachttische, Betten, Leseecke, Webstuhl, Bilder.
7. Zone C: Werkbank, Zwischenablage-Fass, Banner, Wegweiser.
8. Ganz zum Schluss die Teppiche legen: den Laufweg z1027/1028 und den Anschluss bei (-1409,1026). Die Luke selbst nicht bedecken.
9. Fässer beschriften (72 Schilder), optional wachsen, und Kerzen anzünden.
