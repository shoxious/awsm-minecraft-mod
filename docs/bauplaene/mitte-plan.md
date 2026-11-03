# Mittlere Ebene: Essen & Dorfbewohner (Plan, Stehhöhe y124)

Grundlage: `mitte-karte.txt`. **Wichtigste Erkenntnis aus der Karte:** Unter dem *ganzen* Haus ist bereits eine
leere, 3 Blöcke hohe Halle ausgehöhlt (y124–126, Boden y123, Decke y127), und zwar
**x -1448..-1408, z1022..1032**. Nach Süden zur Terrasse ist sie zwischen x -1448 und -1419 **offen** (z1033 ist Luft).
Der Weizenkeller (x -1406..-1398, Feld auf y124, Mauerring x -1407/-1397) schließt östlich an.
Schienen (bleiben **unverändert**): z1026 x -1413..-1411 → x -1413 bis z1029 → z1030 x -1418..-1413 → x -1418 z1030..1036
→ z1036 x -1417..-1405 → x -1405 nach Süden über z1043 hinaus. Dazu die Entlade-Station x -1415..-1408, z1031..1033
(Trichter, Redstone, 2 Kisten). Alle Pläne lassen Schienen und Entladestation stehen.

---

## Variante A – „Marktplatz-Terrasse“
- **Terrasse = Markt:** 2 Reihen Marktstände (je 3 breit, Kirschstamm-Pfosten, Markisen-Dach gestreift aus weißer
  und rosa Wolle) mit je 1–2 Dorfbewohnern hinter einer Theke; Brunnen in der Mitte; ca. 12 Händler.
- **Unterhalle unter dem Haus = Gewächshaus:** zwei 9×9-Bauernfelder mit Bauern-Villagern (Laternenlicht),
  Brutstation, Kornkammer.
- Vorteil: sehr hübsch und von der Haustreppe aus gut sichtbar.
- Nachteil: Händler stehen draußen. Gegen Zombies (Burghof-Treppe, Schienen-Durchgang x -1405) und Blitz/Hexen
  müsste die ganze Terrasse eingehaust werden (Glaswand an z1043, Tore). Es bleibt kaum Platz für Beete
  und Tiere, und Felder im Keller brauchen viele Laternen.

## Variante B – „Bauernhof-Terrasse“ (**Empfehlung**)
- **Unterhalle = Handelshalle** mit 22 Zellen + Brutraum. Sie ist geschlossen und hell, also zombiesicher.
  Fenster gehen auf die Terrasse.
- **Terrasse = Bauerngarten:** Kürbis-/Melonen-Hochbeet, Gemüse-Hochbeet mit Wasserspülung in Trichter und Kiste,
  Brunnenplatz mit Bänken, Kuh- und Schafweide, Kirschzaun-Geländer mit Laternen.
- **Weizenkeller** wird zur vollautomatischen Bauern-Villager-Farm (Trichterrinne, Kiste zur Terrasse hin).
- **Hühnerfarm** bleibt technisch unberührt und bekommt nur ein rosa Kirsch-Dach. Daneben entsteht eine Ausgabe-Ecke.

**Warum B:** Die Händler sind sicher (Zellen, Zauntore, die Dorfbewohner nicht öffnen können). Die vorhandene Halle
wird genutzt, statt auf der Terrasse neu zu bauen. Pflanzen und Tiere stehen unter freiem Himmel, und die Terrasse
bleibt ein „Garten“ vor dem Haus. Für den Bot gibt es klare Raster aus 1er-Zellen, 9er-Beeten und geraden Zäunen.

---

## Lageplan Variante B (Draufsicht, Ebene y124; x nach rechts, z nach unten)

```
        x: 987654321098765432109876543210987654321098765432109876
                   |         |         |         |         |       (| = x -1440, -1430, -1420, -1410, -1400)
z 1021  ssssssssssssssssssssssssssssssssssssssssssssssssssssss
z 1022  sbbbbk#w#w#w#w#w#w#w#w#w#w#w#w#FFFFF......iiiiiiiiiiis
z 1023  sbbbb:#v#v#v#v#v#v#v#v#v#v#v#v#..........gH%%%%%%%%%is
z 1024  s:::::|T|T|T|T|T|T|T|T|T|T|T|T|..........gH%%%%%%%%%is
z 1025  s:::::g:::::::::::::::::::::::g..........gH%%%%%%%%%is
z 1026  s:::::g:::::::::::::::::::::::g.....===.#gH%%%%%%%%%is
z 1027  s:::::dpppppppppppppppppppppppD.....=..ZEgH%%%%k%%%%is
z 1028  sfffffg:::::::::::::::::::::::g.....=...#gH%%%%%%%%%is
z 1029  sfffffg:::::::::::::::::::::::g.....=....gH%%%%%%%%%is
z 1030  sff~ff|T|T|T|T|T|:p:|T|T|T|T|T|======....gH%%%%%%%%%is
z 1031  sfffff#v#v#v#v#v#:p:#v#v#v#v#v#=..RRRRRRRgH%%%%%%%%%is
z 1032  sfffff#w#w#w#w#w#:p:#w#w#w#w#w#=..RRRRRRRCCiiiiDiiiiis
z 1033  sg|g|D|g|g|g|g|g|gDg|g|g|g|g|g|=..RRRRRRRR___________s
z 1034  s______________________________=_____________________s
z 1035  sooooooooo_ooYoYoYoo__^^^^^^^^^=________________CC___s
z 1036  somem~memo_offf~fffo__^^^^^^^^^==============__XXXXXXs
z 1037  somem~memo_offf~fffo__^^^^^^^^^nnnnnnnnnnnnn=__XXXXXXs
z 1038  somem~memo_offf~fffo___________nKKKKKnAAAAAn=__XXXXXXs
z 1039  somem~memo_offf~fffo___h_BBB_h_nKKKKKnAAAAAn=__XXXXXXs
z 1040  somem~memo_offf~fffo_Q_h_B~B_h_DKKKKKDAAAAAn=__XXXXXXs
z 1041  somem~memo_offf~fffo___h_BBB_h_nKKKKKnAAAAAn=________s
z 1042  soooooooooCCHHHHHHHo___________nnnnnnnnnnnnn=Q_______s
z 1043  sPPPPPPPPPPPPPPPPPPPPPPPPPPPPPDPPPPPPPPPPPPP=PPPPPPPPs
```
Erste Spalte = x -1449, letzte = x -1396.
Legende: `s` Fels/Bestand · `#` Kirschbretter-Wand · `|` Kirschstamm-Pfosten · `w` Arbeitsblock · `v` Villager-Platz ·
`T` Theke (y124 Kirsch-Falltür oben geschlossen, y125 frei = Handelsfenster, y126 Kirschstufe oben) · `:` Gang/Boden ·
`p` rosa Teppich · `g` Fenster (Steinziegel / Glasscheibe / Kirschbretter bzw. Vollglas im Weizenkeller) ·
`D` Zauntor/Eingang · `d` Baby-Durchschlupf · `b` rosa Bett · `k` Komposter · `f` Ackerboden · `~` Wasser ·
`F` Fass-Lager · `R` Entladestation (Bestand) · `=` Schiene (Bestand) · `%` Weizenfeld (Bestand) · `i` Mauer (Bestand) ·
`H` Trichter · `C` Kiste · `E` Empfänger-Villager · `Z` Braustand (versetzt) · `^` Haupttreppe (Bestand) ·
`o` Fichtenstamm-Rahmen · `Y` Spender (Wasser) · `m` Kürbis-/Melonenstiel · `e` Fruchtfeld (Gras) · `B` Brunnen ·
`h` Bank · `Q` Mini-Kirschbaum · `n` Fichtenzaun · `K` Kuhweide · `A` Schafweide (Gras) · `X` Hühnerfarm (Bestand) ·
`_` Weg (Steinziegel-Boden bleibt; Gras/Erde → Trampelpfad) · `P` Kirschzaun-Geländer auf der Burgmauer.

---

## Zonen im Detail (Variante B)

### Z1 Handelshalle (x -1448..-1419, z1022..1033, y124–126)
- **Raster:** Zellen auf geraden x (-1442 … -1420), Trennwände auf ungeraden x (-1441 … -1421).
- **Nordreihe:** 12 Zellen. Arbeitsblock z1022, Villager z1023, Theke z1024.
  Trennwand: z1022–1023 Kirschbretter (y124–126), z1024 Kirschstamm (3 hoch).
- **Südreihe:** 10 Zellen, spiegelbildlich: Theke z1030, Villager z1031, Arbeitsblock z1032.
  Bei x -1432..-1430 liegt das **Foyer** (keine Zellen, Trennwand -1431 entfällt).
- **Gang:** z1025..1029, 5 breit. Mittelstreifen rosa Teppich z1027 (23 + 3 im Foyer).
  6 Laternen an der Decke (y126) bei x -1441, -1437, -1433, -1429, -1425, -1421, z1027, dazu 1 Laterne im Foyer.
- **Südwand z1033 (Fassade zur Terrasse):** Auf ungeraden x Kirschstamm (y124–126). Auf geraden x Fenster:
  y124 Steinziegel, y125 Glasscheibe, y126 Kirschbretter.
  **Haupteingang** x -1431: Kirsch-Zauntor (y124), y125 frei, y126 Kirschbretter.
  **Brutraum-Tor** x -1444: gleich gebaut.
- **Ostwand x -1419:** Kirschstamm bei z1024/1030/1033, Kirschbretter an den Zellen, Fenster am Gang,
  **Zauntor z1027** zum Bahnhof/Weizenkeller.
- **Warum Zauntore:** Dorfbewohner öffnen Holztüren, aber keine Zauntore. Zombies können Zauntore nicht einschlagen.
- **Zellen füllen:** Falltür an der Theke öffnen und nur in *dieser* Zelle den Arbeitsblock setzen.
  Ein arbeitsloser Villager aus dem Gang läuft hinein und beansprucht ihn. Danach die Falltür schließen.
  Stufe oben (y126) und Falltür (y124) verhindern das Herausspringen; gehandelt wird durch y125.
- **Ordnung:** Über jeder Theke ein Kirsch-Schild mit dem Beruf (am Kirschstufen-Sturz y126). Alle Wandfackeln an der
  Nordwand und die Bodenfackeln in der Halle entfernen, das Licht kommt nur von Laternen.

**Belegung (West → Ost):**

| x | Nordreihe (Arbeitsblock z1022) | Südreihe (Arbeitsblock z1032) |
|---|---|---|
| -1442 | Bibliothekar – Lesepult | Bauer – Komposter |
| -1440 | Bibliothekar – Lesepult | Bauer – Komposter |
| -1438 | Bibliothekar – Lesepult | Fischer – Fass |
| -1436 | Bibliothekar – Lesepult | Schäfer – Webstuhl |
| -1434 | Bibliothekar – Lesepult | Kleriker – Braustand (später, Lohenrute) |
| -1432 | Bibliothekar – Lesepult | *Foyer* |
| -1430 | Rüstungsschmied – Schmelzofen | *Foyer* |
| -1428 | Werkzeugschmied – Schmiedetisch | Steinmetz – Steinsäge |
| -1426 | Waffenschmied – Schleifstein | Gerber – Kessel |
| -1424 | Metzger – Räucherofen | Kartograph – Kartentisch (wenn 2 Papier) |
| -1422 | Pfeilmacher – Bognertisch | frei (Reserve) |
| -1420 | Pfeilmacher – Bognertisch | frei (Reserve) |

**Handelstipps passend zum Vorrat** (Ankauf, ca.-Werte):
- Pfeilmacher: 32 Stöcke → 1 Smaragd. Kirschholz macht das zur Dauerquelle. Später auch Faden und Federn.
- Bauer: 20 Weizen, 22 Karotten, 26 Kartoffeln, 15 Rote Bete, später 6 Kürbisse oder 4 Melonen → 1 Smaragd.
- Metzger: 14 rohe Hühnchen (Hühnerfarm!) und 15 Kohle.
- Schmiede: 15 Kohle bzw. Eisenbarren.
- Fischer: 10 Kohle oder 20 Faden.
- Schäfer: 18 weiße Wolle.
- Bibliothekar: 24 Papier. Wichtig: Er **verkauft Bücherregale (9 Smaragde)**. So entstehen die übrigen 5 Lesepulte
  aus den 77 Smaragdblöcken, ohne Leder.

### Z2 Brutraum (x -1448..-1444, z1022..1032), Westende der Halle
- 4 rosa Betten (x -1448..-1445, Kopfende z1022) und 1 Komposter (x -1444, z1022).
- Hochbeet 5×5 (x -1448..-1444, z1028..1032): Erde auf den Boden setzen (y124), Wasser in der Mitte (x -1446, z1030).
  Ergibt 24 Felder.
- 2 Laternen an der Decke. Trennwand zur Halle x -1443: Glasscheiben, Kirschstamm, Kirschbretter.
- **Baby-Durchschlupf** (x -1443, z1027): y124 frei, y125 Kirsch-Stufe *unten*. Das lässt nur 1 Block Höhe:
  Babys passen durch, Erwachsene nicht.
- **Ablauf:** 1 Bauer und 1 weiterer Villager kommen hinein. Den Transport erledigt eine Lore auf temporären Schienen
  durch das Tor x -1444; danach die Schienen zurückbauen.
  Der Bauer erntet, backt Brot und versorgt den Partner. So vermehren sich beide automatisch.
  Babys laufen in den Gang, wachsen dort heran und beziehen die jeweils offene Zelle.
  Zum Start 6 Brot pro Villager einwerfen.
- Hinweis: Ab etwa 10 Villagern kann im Gang ein Eisengolem erscheinen (harmlos).

### Z3 Bahnhof & Kornkammer (x -1418..-1408, z1022..1030)
- Schienen und Entladestation bleiben.
- An der Nordwand x -1418..-1414, z1022: Fass-Wand, 5 breit × 3 hoch = 15 Fässer (Fichte), mit Schildern
  (Weizen / Karotten / Kartoffeln / Kürbis / Melone / Eier / Fleisch / Wolle / Leder …).
- 2 Laternen und ein paar Heuballen als Deko.

### Z4 Weizenkeller → Villager-Gemüsefarm (x -1407..-1397)
- Das Feld 9×9, der Komposter, der Leuchtpilz und die Bambus-Falltüren bleiben. 2–3 Bauern arbeiten im Feld.
  Ihnen Karotten-, Kartoffel- und Weizensamen geben, dann pflanzen sie gemischt.
- **Sammlung:** Grasrand x -1407, z1023..1031 (y124) → **9 Trichter** in Richtung Süden. Die Bruchsteinmauer darüber
  (y125) entfernen. Der letzte Trichter führt in eine **Doppelkiste x -1408..-1407, z1032**: Die Mauer darüber
  entfernen, damit sich die Kiste öffnen lässt. Die Kiste ist von der Terrasse (z1033) aus erreichbar.
- **Glaswand** x -1408, z1023..1031, y124–125: 18 Vollglas.
- Dahinter in einer Nische (x -1409, z1027) steht ein **Empfänger-Villager**. Dafür eignet sich der vorhandene
  Kleriker; den Braustand nach x -1410, z1027 versetzen.
- So funktioniert es: Die Bauern werfen überschüssiges Essen zum Empfänger. Die Würfe landen vor dem Glas auf
  den Trichtern und gehen in die Kiste.
- 4 zusätzliche Laternen an der Decke über den Feldecken.
- Zauntor in der Südmauer bei x -1402, z1032 als Zugang von der Terrasse.

### Z5 Terrasse West: Hochbeete (z1035..1042)
- **W1 Kürbis/Melone** (x -1448..-1440):
  - Rahmen aus Fichtenstamm (y124).
  - Innen 7×6, Spalten von West nach Ost: Stiel / Frucht / Stiel / **Wasser** / Stiel / Frucht / Stiel
    (x -1447 … -1441). Das ergibt 24 Stiele (12 Kürbis im Westen, 12 Melone im Osten) und 2 Fruchtspalten aus Gras.
  - Ernte von Hand mit der Axt.
- **Weg** x -1439, an seinem Ende die Doppelkiste x -1439..-1438, z1042.
- **W2 Gemüse mit Wasserspülung** (x -1438..-1430):
  - Innen 7×6 (z1036..1041) mit Wasserrinne x -1434 auf Ackerhöhe. Das ergibt **36 Pflanzen**
    (Karotten / Kartoffeln / Rote Bete).
  - Nordkante z1035, y125: 3 Spender mit Wassereimer (x -1436, -1434, -1432) im Wechsel mit Fichtenbrettern.
    Darauf liegt eine Redstone-Linie (y126, 7 Staub), die Steinknopf-Säule steht bei x -1430.
  - Seiten y125: Fichtenzaun, damit das Spülwasser nicht ausläuft.
  - Südkante z1042: **7 Trichter** nach Westen in die Kiste.
  - Davor bei z1043 eine Steinziegel-Brüstung (y124) mit Kirschzaun (y125), die das Wasser stoppt.
  - Bedienung: Knopf 1× drücken, das Wasser spült die Ernte in die Trichter. Knopf ein 2. Mal drücken, das Wasser
    ist weg. Danach neu pflanzen. Knochenmehl ist reichlich da (Knochenblöcke).

### Z6 Brunnenplatz (x -1429..-1419, z1038..1042), am Fuß der Haupttreppe
- Landefläche z1038 bleibt frei.
- **Brunnen** 3×3 (x -1424..-1422, z1039..1041): Rand aus 8 Steinziegeltreppen, Wasser in der Mitte.
  An den Ecken je 2 Fichtenzäune hoch, darüber ein Dach aus 9 Kirschstufen (rosa), in der Mitte eine Laterne.
- 2 Bänke aus je 3 Kirschtreppen (x -1426 und x -1420, z1039..1041), dem Brunnen zugewandt.
- Mini-Kirschbaum Q bei x -1428, z1040.
- Laternen auf die beiden Treppenpfeiler (x -1427 / -1419).
- Weg x -1419 nach Süden zur Osttreppe, oben ein **Zauntor (x -1419, z1043)** als Zombiesperre zum Burghof.

### Z7 Weide (x -1418..-1406, z1037..1042, eingerahmt von den Schienen)
- Fichtenzaun-Ring, Teiler bei x -1412, 2 Zauntore (x -1418 und x -1412, jeweils z1040).
- Kuhweide 5×4 und Schafweide 5×4. Die Schafweide braucht Gras, damit die Wolle nachwächst.
- 2 Heuballen und 1 Kessel als Tränke-Deko.
- Zweck: Kühe liefern **Leder** für Bücher und Rindfleisch, Schafe Wolle für Betten und Schäfer-Handel.

### Z8 Hühnerfarm-Einbindung (x -1402..-1397)
- An der Technik nichts ändern.
- Wenn die Oberseite frei ist, ein Kirschstufen-Dach (y127, ca. 7×7) aufsetzen. Dazu ein Schild „Hühnerstall“.
- Die Ausgabekisten (x -1401/-1400, z1035) werden über den Weg z1034 erreicht.
- Mini-Kirschbaum Q bei x -1404, z1042.

### Z9 Wege & Geländer
- **Promenade z1034** von x -1448 bis -1403. Sie läuft unter der Treppe durch: bei z1034 sind y124–125 frei.
- Querwege x -1439 und x -1429.
- **Geländer z1043:** Kirschzaun auf der Burgmauer, alle ~6 Blöcke eine Laterne auf einem Zaunpfosten.
  Lücke bleibt für die Schiene x -1405, Zauntor an der Osttreppe x -1419.
- Die Bodenfackeln an z1041 entfernen (sie kollidieren mit Beeten und Weide). Ersatz: Laternen.
  Auf der Terrasse genügt Lichtstufe ≥1 gegen Spawns.
- Optional: rosa Blütenblätter auf Grasflecken. Bezug siehe Farm-Liste.

---

## Materialbilanz Variante B (gerundet, inkl. Verschnitt)

| Material | Bedarf | Vorrat | Rest | Wofür |
|---|---|---|---|---|
| Kirschstamm | ~240 (123 gesetzt + 114 zu Brettern) | ~850 | ~610 | Pfosten, Trennwände, Falltüren, Stufen, Schilder, Zaun |
| Fichtenstamm + Fichtenbretter | 53 Stämme + 224 Bretter (100 vorhanden + ~31 Stämme) | 450 / 100 | ~365 / 0 | Beet-Rahmen, 59 Zäune, 2 Tore, 16 Fässer |
| Bambusbretter | ~15 | ~350 | ~335 | 3 Komposter, Lesepult-Stufen |
| Steinziegel | 43 (+124 optional: Gangboden) | ~740 | ~570 | Fenster-Brüstung, Brunnen, W2-Brüstung |
| Bruchstein / Stein | 37 / ~10 | 1300 / 4200 | | 3 Spender, 2 Öfen, Glattstein, Steinsäge, Knopf |
| Glas / Glasscheiben | 18 / 27 | 33 / ~120 | 15 / ~93 | Glaswand Weizenkeller / Hallenfenster |
| Eisen | ~136 Barren ≈ 15 Blöcke | ~440 Blöcke | | 28 Laternen, 16 Trichter, 4 Eimer, Schmelzofen, Schmiedetisch, Steinsäge, Kessel, Schere, Hacke |
| Kisten (Inventar) | 20 (16 in Trichtern + 4) | 40 | 20 | |
| Fackeln | 28 | ~250 | | in Laternen |
| Redstone | 10 | 37 (+68 Blöcke) | | Spülung W2 |
| Faden | ~11 | ~300 | | 3 Bögen (Spender), Webstuhl |
| Wolle weiß | 30 (12 Betten + 18 für 26 Teppiche) | ~120 | ~90 | rosa gefärbt |
| Farbstoff | 15 Rot + 15 Weiß (aus Knochenmehl) → 30 Rosa | Rot 58 | Rot 43 | Betten, Teppich |
| Erde / Gras | ~84 / ~52 | 1950 / 490 | | Hochbeete, Brutbeet / Schafweide, Fruchtfelder |
| Weizen | ~72 | ~400 | ~330 | 12 Brot Start-Zucht, 4 Heuballen |
| Karotten / Goldkarotten | 19 Start-Saat / 0 | 19 / 64 | | Goldkarotten bleiben Spieler-Essen |
| Knochenblöcke | ~20 | ~250 | | Weißfarbe + Knochenmehl |
| Kirschsetzlinge | 2 (zum Laub-Gewinnen) | ~340 | | |
| Bücher | 3 (1 Bücherregal) | ~5 | 2 | erstes Lesepult |
| Smaragde | 45 (5 Regale beim Bibliothekar) | 6 + 77 Bl. | | übrige 5 Lesepulte |
| Kies → Feuerstein | 2 Feuerstein | ~200 Kies | | 2. Bognertisch (Kies setzen/abbauen) |
| Schienen | ~30 temporär | ~180 | zurück | Villager-Transport per Lore |

**Fazit:** Alles für Bau und Grundbetrieb ist vorhanden. Es fehlen nur Saatgut, Tiere und zwei Spezialteile
(siehe Farm-Liste).

## Craft-Liste (Variante B)
**Bau-Elemente**
- 22 Kirsch-Falltüren (11×)
- 81 Kirschstufen (14×), 6 Kirschtreppen (2×)
- 24 Kirschschilder (8×)
- 51 Kirschzäune (17×), 4 Kirsch-Zauntore
- 60 Fichtenzäune (20×), 2 Fichten-Zauntore
- 4 rosa Betten (zuerst 12 Wolle rosa färben)
- 26 rosa Teppiche (aus 18 rosa Wolle)
- 30 rosa Farbstoff (15× Rot + Weiß)
- 28 Laternen, 16 Trichter (mit Inventar-Kisten)
- 4 Eimer, 3 Bögen, 3 Spender, 1 Steinknopf
- 16 Fässer, 4 Heuballen, 1 Schere, 1 Hacke

**Arbeitsblöcke**
- 1 Bücherregal → 1 Lesepult (die übrigen 5 Regale kaufen)
- 3 Komposter
- 1 Bognertisch (2 Feuerstein; einer ist schon vorhanden)
- 1 Schmelzofen (Ofen + 5 Eisen + 3 Glattstein)
- 1 Schmiedetisch, 1 Schleifstein
- 1 Räucherofen (Ofen + 4 Stämme)
- 1 Fass, 1 Webstuhl, 1 Steinsäge, 1 Kessel
- optional 1 Kartentisch (nur mit 2 Papier)

## Farm-Liste (nicht im Vorrat)
1. **Kürbiskerne, Melone/Melonenkerne** für Beet W1. Rote-Bete-Samen für W2.
2. **Kartoffeln** (≥10 zum Pflanzen). Weizensamen fallen aus dem Weizenkeller ab.
3. **2 Kühe und 2 Schafe**. Mit Weizen in der Hand anlocken. Eine Leine bräuchte einen Schleimball.
4. **Leder** (über die Kühe) und **Zuckerrohr** für Papier: Kartentisch, Bibliothekar-Handel, eigene Bücher.
5. **Lohenrute** (Nether) für einen 2. Braustand (Kleriker-Zelle -1434).
   Alternativ den vorhandenen Braustand nehmen und dem Empfänger einen anderen Block geben.
6. **Kirschlaub**: Einen Setzling irgendwo pflanzen, mit Knochenmehl wachsen lassen und das Laub mit der Schere
   ernten (für 2 Mini-Bäume, ~24 Laub). **Rosa Blütenblätter** gibt es im Kirschhain oder durch Knochenmehl auf
   vorhandene Blütenblätter.
7. **Weitere Dorfbewohner**: kommen über den Brutraum. Wer schneller sein will, bringt 1–2 aus einem Dorf per Lore.
8. Optional: Ton (Blumentöpfe), mehr Papier.

## Bau-Reihenfolge (Bot-freundlich, alles in geraden Linien, Reichweite ≤4,5)
1. Halle Rohbau: Trennwände Nord, dann Süd (Spalte für Spalte vom Gang aus), danach Süd- und Ostwand mit Toren.
   Erst dann ist die Halle dicht.
2. Laternen, Teppich und Schilder setzen, alte Fackeln entfernen.
3. Brutraum: Betten, Beet, Komposter, Durchschlupf. 2 Villager per Lore hinein, Brot geben.
4. Weizenkeller: Trichterrinne, Kiste, Glaswand, Empfänger-Nische, Braustand versetzen.
5. Terrasse: Geländer z1043 mit Toren, dann W2 (Rahmen → Trichter → Spender und Redstone → Erde/Wasser),
   dann W1, Brunnen, Bänke, Weide.
6. Arbeitsblöcke einzeln setzen (immer nur 1 offene Zelle) und Zellen füllen.
   Zuerst Bibliothekar Nr. 1 → Regale kaufen → die übrigen Lesepulte.
7. Deko: Hühnerstall-Dach, Mini-Bäume, Blütenblätter.

**Vor Ort prüfen:**
- Höhe und Form der Burgmauer-Krone z1043.
- Breite der Osttreppe bei x -1419.
- Ob die Oberseite der Hühnerfarm frei ist.
- Welche Berufe die 4 vorhandenen Villager haben.
- Bodenmaterial y123 in der Halle.
