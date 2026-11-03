# Auenland-Eingänge im Burghof – Bauplan (nur Planung)

Stand: Version 2, nach den Spieler-Vorgaben **keine Hügel**, **Fichtenholz** und **runde Fenster**.
Koordinaten sind absolut (x, y, z). Der Hofboden ist y118, man steht auf y119.

**Kurz gesagt:** Fünf freistehende, flache Fichtenholz-Fronten mit runder Tür und runden Fenstern stehen im Kreis um einen kleinen Festplatz vor dem Burghaus. Hinter jeder Tür liegt ein kleiner Vorraum. Zwei Eingänge haben eine Treppe, drei eine Leiter mit Falltür. Alle führen in **einen schon vorhandenen Ring-Gang auf y113–115**: Der Scan zeigt, dass dieser Ring bereits gegraben ist. Er wird nur begradigt und beleuchtet und ist dann der „gemeinsame Gang“. Der Hof bleibt flach. Nur hinter den Türen stehen kleine Fichten-Kellerhälse mit Satteldach, 5 Blöcke breit.

---

## 0. Was der Scan zusätzlich gezeigt hat (wichtig)

| Fund | Lage | Bedeutung |
|---|---|---|
| **Ring-Gang (bestehend)** | Nord z1046 (x -1441..-1405), West x -1441 (z1046..1062), Süd z1062 (x -1441..-1405), Ost x -1406/-1405 (z1046..1062); Laufhöhe y113, Boden y112 | wird der gemeinsame Gang, alle 5 Abstiege enden darauf |
| Burghaus-Leiter | x -1423, z1046, y105..118 (hängt an der Wand z1045) | verbindet das Burghaus schon mit dem Nordgang. **Wand z1045 bei x -1423 nicht abbauen** |
| Minecart-Strecke | Rampe x -1433, z1053 (y105) → z1046 (y112), dann Schienen z1043/1044 y112 nach Osten bis x -1397 und hoch | **Sperrzone R**: x -1434..-1431, z1044..1053 unterhalb y114 nicht graben oder füllen |
| Hohlraum Süd | x -1430..-1418, z1056..1061, y110–116 (teils Bruchstein-Flicken), offen zum Südgang | wird Farmraum „S“. **Unter x -1420..-1415, z1058..1062 hat y116 nur 2 Blöcke Decke** (y117 Erde, y118 Gras) → dort nicht graben |
| Höhle tief | y105: x -1447..-1439, z1044..1057 | Reserve für spätere Großfarmen (tiefste Ebene) |
| Bodenoberfläche „_“ im Norden | z1044–1046 | ist **Stein auf y118** (kein Gras), also normal bebaubar |

---

## 1. Lageplan (Draufsicht y119)

```
     x: -1452 (links) ... -1397 (rechts); Ziffern-Zeile = letzte Stelle von |x|
     21098765432109876543210987654321098765432109876543210987
1042 ^^^MM^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^M
1043 ^^^MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM^MMMMMMMM
1044 ^^^M_______________......M...ff.fM.....________^^^^_^___
1045 ^^^M_____KKKKK____.......MHHHHHHHM......_KKKKK_^^^^^^___
1046 ^^^M.~~_.KKLKK............HHHHHHHM.......KKLKK___..^..._
1047 ^^.M.~~_.KKKKK.............HHHHHHMl......KKKKK........._
1048 ^^^M..44444D44..l======....HHMHHH^.=====.55D55555......_
1049 ^^^M..44444444..=.11zzz=...HHHHHH.=zzz22=55555555.......
1050 ^^^M..zggggwwT==..11ggzl=..HHHHHHC=zgg22=Twwggggz.......
1051 .^^M..zzzzzzzz....11ggz.=..HHHHHHC=zgg22.zzzzzzzz.......
1052 ..^M.........KKKKK11ggz.=.iCHMHCC.=zgg22KKKKK...........
1053 ..^M........tKKKKK11ggz.=loooooool=zgg22KKKKKt..........
1054 ...MMMM.....tKKKKKD1wwT==ooooooooo=Tww2DKKKKKt.......MMM
1055 ...^^^MM....tKKKKK11ggz..loooooool.zgg22KKKKKt.....MMM..
1056 ....^^^......KKKKK11ggz.zzzzzTzzzzzzgg22KKKKK....MMM....
1057 _...^^^^^.........11ggz.zggggwggggzzgg22.......MMM......
1058 __....^^^.._MMM...11ggz.zggggwggggzzgg22...._MMM........
1059 ___....^......MMM.11zzz.33333333333zzz22...MMM_____.....
1060  __.....^.......MMM.....33333D33333......MMM_____...__..
1061   ___...._........MMM......KKKKK.......MMM._..._.....___
1062     __ .............MMM....KKLKK.....MMM.............___
1063          .........._  MMM..KKKKK...MMM...............___
1064            ......_     .MMM......MMM__.^........_...____
1065              ..._     _.._MMM..MMM________....._________
1066                      ._____ MMMM    _______...__________
```
Legende: `1..5` Front des Eingangs H1..H5 (2 Blöcke tief) · `D` Tür · `K` Fichten-Kellerhals (Vorraum + Abstieg) · `L` Leiterschacht · `t` Treppe unter der Erde (zur Orientierung) · `z` Fichtenzaun · `T` Zauntor · `g` Beet · `w` Gartenweg (Kies) · `=` Trampelpfad (mit der Schaufel) · `o` Festplatz (Kies) · `l` Laternenpfahl · `H` Burghaus · `C` Kiste · `M` Mauer · `~` Wasser · `f` Feld · `^` höher · `_` Steinboden y118

Anordnung: Die Eingänge liegen wie ein kleines Dorf **um den Festplatz** vor der Burghaus-Tür und schauen alle zur Mitte. H1 schaut nach Osten, H2 nach Westen, H3 nach Norden genau auf die Burghaus-Tür, H4 und H5 nach Süden. Die Wege schwingen sich in Bögen um die Gärten herum.

### Kernkoordinaten

| | Front schaut | Front d0 (Ebene) von–bis | Tür (y119/120) | Vorraum (y119) | Kellerhals bis | Abstieg | landet auf | Kamin (y125/126) |
|---|---|---|---|---|---|---|---|---|
| **H1** „Beutelsend“ | Ost | x -1433, z1049..1059 | (-1434, 1054) | x -1435, z1053..1055 | x -1439, z1052..1056 | Treppe x -1436..-1440, z1053..1055 | Westgang (-1441, 113, 1054) | (-1434, 1050) |
| **H2** „Tukhaus“ | West | x -1414, z1049..1059 | (-1413, 1054) | x -1412, z1053..1055 | x -1408, z1052..1056 | Treppe x -1411..-1407, z1053..1055 | Ostgang (-1406, 113, 1054) | (-1413, 1050) |
| **H3** „Festwiesen-Tor“ | Nord | z1059, x -1428..-1418 | (-1423, 1060) | z1061, x -1424..-1422 | z1063, x -1425..-1421 | Leiter, Schacht (-1423, 1062) | Südgang (-1423, 113, 1062) | (-1427, 1060) |
| **H4** „Hornbläser“ | Süd | z1049, x -1446..-1439 | (-1441, 1048) | z1047, x -1442..-1440 | z1045, x -1443..-1439 | Leiter, Schacht (-1441, 1046) | Nord/West-Ecke (-1441, 113, 1046) | (-1445, 1048) |
| **H5** „Brandyhaus“ | Süd | z1049, x -1411..-1404 | (-1409, 1048) | z1047, x -1410..-1408 | z1045, x -1411..-1407 | Leiter, Schacht (-1409, 1046) | Nordgang (-1409, 113, 1046) | (-1405, 1048) |

Gartentore: H1 (-1430, 1054) · H2 (-1417, 1054) · H3 (-1423, 1056) · H4 (-1439, 1050), seitlich nach Osten · H5 (-1411, 1050), seitlich nach Westen.

**Relative Achsen** für den Standard-Bauplan: `d` = Tiefe ab der Front nach hinten (d0 Frontebene, d1 Türebene, d2 Vorraum …), `u` = quer. +u ist „rechts“ aus Sicht eines Betrachters, der vor der Front steht.

| | +d (nach hinten) | +u (rechts) |
|---|---|---|
| H1 | West (−x) | Nord (−z) |
| H2 | Ost (+x) | Süd (+z) |
| H3 | Süd (+z) | West (−x) |
| H4, H5 | Nord (−z) | Ost (+x) |

---

## 2. Standard-Eingang (Fichte)

### 2.1 Frontansicht (Front 11 breit, u -5..+5, Höhe y119..y124 plus Kamin)

```
         u: -5 -4 -3 -2 -1  0 +1 +2 +3 +4 +5
  y126                                  *        * Lagerfeuer (auf Kamin, d1 u+4)
  y125                                  K        K Bruchstein (oder Heuballen = Signalfeuer)
  y124      ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^     Dach-Treppen (2 Reihen, First zwischen d0/d1)
  y123      =  =  =  =  =  =  =  =  =  =  =     Fichtenstamm liegend (Balken)
  y122      I  F  +  7  #  #  #  F  +  7  I     davor bei d-1: Vordach  -  -  -  (u-1..+1)
  y121      I  +  +  +  F  #  7  +  +  +  I     davor bei d-1: Laternen o  .  o  hängend (u±1)
  y120      I  L  +  J  #  D  #  L  +  J  I
  y119      I  #  #  #  L  D  J  #  #  #  I
            ^^^^^^^^^^^ Fenster ^^^  Tür  ^^^ Fenster ^^^^^^^^
```
- **Rundtür:** In d0 bilden 4 Fichtentreppen (L, J unten, F, 7 oben) einen runden 3×3-Ring. Einen Block dahinter (d1) steht die **Fichtentür** (u0, y119+120), umgeben von einer „Scheibe“ aus Fichtenbrettern (`#` in d1). Von vorn wirkt das wie eine runde Tür.
- **Runde Fenster** (links u-3 und rechts u+3, Mitte y121): Ein Kreuz aus 5 Glasscheiben, die 4 Ecken sind Fichtentreppen (L, J, F, 7). Direkt dahinter (d1) liegen 9 **Woll-Blöcke als „Vorhang“**. So braucht es keinen Raum hinter den Fenstern.
- **Rahmen:** Pfosten `I` = Fichtenstamm senkrecht (in d0 und d1, y119..y122). Balken `=` = Fichtenstamm liegend (Achse quer zur Front) in d0 und d1.
- **Vordach:** 3 Fichtenstufen (obere Hälfte) bei d-1, y122, u-1..+1. Darunter hängen 2 Laternen bei d-1, y121, u±1 (Weg in der Mitte bleibt frei).
- **Schmale Variante (H4/H5), 8 breit:** nur ein Fenster. H4: u -5..+2 (Fenster links, Pfosten u-5 und u+2). H5: u -2..+5 (Fenster rechts, Pfosten u-2 und u+5).

Symbole der Treppen (aus Sicht vor der Front):

| Zeichen | Treppe | Setzen (Bot) |
|---|---|---|
| `L` | aufrecht, hohe Seite links (−u) | Blick nach −u, Klick auf die **Oberseite** des Blocks darunter |
| `J` | aufrecht, hohe Seite rechts (+u) | Blick nach +u, Klick Oberseite darunter |
| `F` | kopfüber, volle Seite links | Blick nach −u, Klick auf die **Unterseite** des Blocks darüber |
| `7` | kopfüber, volle Seite rechts | Blick nach +u, Klick Unterseite darüber |
| `^` Dach d0 | hohe Seite nach hinten (+d) | Blick +d |
| `^` Dach d1 | hohe Seite nach vorn (−d) | Blick −d (Bot steht hinter der Front) |

(Die Treppe zeigt mit ihrer hohen Seite in die Blickrichtung des Spielers beim Setzen.)

### 2.2 Grundriss pro Höhenebene – Treppen-Eingang (H1, H2)

Oben = vorn (Garten), unten = hinten. Spalten u -5..+5.
Zeichen: `I` Stamm senkrecht · `=` Stamm liegend · `#` Fichtenbretter · `+` Glasscheibe · `w` Wolle · `D` Tür · `L J F 7` Rund-Treppen (s. o.) · `^` Dachtreppe · `_` Fichtenstufe unten · `-` Fichtenstufe oben · `z` Zaun · `T` Tor · `o` Laterne · `k` Kies · `g` Beet · `.` Luft · `x` Bestand (bleibt) · `K` Bruchstein · `*` Lagerfeuer

```
y118 (Boden)                    y119                            y120
     -5 . . . . 0 . . . . +5         -5 . . . . 0 . . . . +5         -5 . . . . 0 . . . . +5
d-3   x x x x x k x x x x x     d-3   z z z z z T z z z z z     d-3   . . . . o . o . . . .   Laternen auf Zaunpfosten
d-2   x g g g g k g g g g x     d-2   z . . . . . . . . . z     d-2
d-1   x g g g g k g g g g x     d-1   z . . . . . . . . . z     d-1
d0    x x x x x k x x x x x     d0    I # # # L . J # # # I     d0    I L + J . . . L + J I
d1    x x x x x # x x x x x     d1    I # # # # D # # # # I     d1    I w w w # D # w w w I
d2            x # # # x         d2            # . . . #         d2            # . . . #
d3            x . . . x         d3            # . . . #         d3            # . . . #
d4            x . . . x         d4            # . . . #         d4            + . . . +   kleine Seitenfenster
d5            x . . . x         d5            # . . . #         d5            # . . . #
d6            x x x x x         d6            I # # # I         d6            I # # # I
(d3..d5 u-1..+1: Gras/Erde ausheben)

y121                            y122                            y123
d-1   . . . . o . o . . . .     d-1   . . . . - - - . . . .     d0    = = = = = = = = = = =
d0    I + + + F . 7 + + + I     d0    I F + 7 # # # F + 7 I     d1    = = = = = = = = = = =
d1    I w w w # # # w w w I     d1    I w w w # # # w w w I     d2..d6        . . _ . .    Firststufe u0
d2            ^ . . . ^         d2              ^ o ^           (Laterne o hängt an der Firststufe)
d3            ^ . . . ^         d3              ^ . ^
d4            ^ . . . ^         d4              ^ . ^
d5            ^ . . . ^         d5              ^ o ^
d6            ^ # # # ^         d6              ^ # ^           Giebel hinten
Dach Kellerhals: u-2 Treppe hohe Seite +u, u+2 hohe Seite -u (y121); u-1 hohe Seite +u, u+1 hohe Seite -u (y122)

y124                            y125 / y126
d0    ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^     d1, u+4 (bzw. u-4, s. Abweichungen): y124 K statt Dachtreppe, y125 K, y126 *
d1    ^ ^ ^ ^ ^ ^ ^ ^ ^ K ^
```

### 2.3 Längsschnitt durch die Mitte (u0) – Treppe

```
 d:   -3 -2 -1  0  1  2  3  4  5  6  7  8
y124             ^  ^
y123             =  =  _  _  _  _  _
y122          -  #  #  o  .  .  o  #
y121             .  #  .  .  .  .  #
y120             .  D  .  .  .  .  #
y119   T         .  D  .  .  .  .  #
y118   k  k  k   k  #  #  .  .  .  x          <- d3..d5 ausgehoben, d6 Boden bleibt als Decke
y117                      t  .  .  .  x
y116                         t  .  .  .  x
y115                            t  .  .  .   Gang (3 hoch)
y114                               t  .  .   Gang
y113                                  t  .   Gang
y112                                     #   Gangboden
```
- 5 Stufen à 3 breit (u-1..+1) aus **Fichtentreppen**, hohe Seite nach −d (zur Tür hin). Über jeder Stufe sind ≥3 Blöcke Luft, so stößt man beim Abstieg nirgends an.
- Ausheben je Spalte: d3 y118–117, d4 y118–116, d5 y118–115, d6 y117–114, d7 y116–113 → **51 Blöcke** pro Treppe (meist Erde).
- Licht: 2 Laternen im Kellerhals (oben), 1 Wandfackel bei d7 an der Seitenwand u-2 (y115).
- Optional: Erdwände der Treppe (u±2, d3..d7) durch Bruchstein ersetzen (~28 Blöcke).

### 2.4 Leiter-Eingang (H3, H4, H5) – Unterschiede

Front und Garten wie oben. Der Kellerhals ist **kürzer: d2..d4** (5 breit, 3 tief).
```
 d:   -2 -1  0  1  2  3  4             Grundriss y119:      Grundriss y118:
y123          =  =  _  _  _            d2   # . . . #        d2   x # # # x
y122       -  #  #  o  .  #            d3   # . H . #        d3   x # F # x
y121          .  #  .  .  #            d4   I # # # I        d4   x x x x x
y120          .  D  .  .  #
y119          .  D  .  .  #            H = Leiter (an der Rückwand d4)
y118    k  k  k  #  #  F  x            F = Fichtenfalltür (obere Hälfte, an d4)
y117                   H  x
y116                   H  x            Schacht u0/d3: y118..y116 ausheben (3 Blöcke)
y115..y113             H  x  = Gang    Leitern y113..y117 (5 Stück) an der Wand d4
```
- Die Falltür ist geschlossen Boden, geöffnet über der Leiter kletterbar (gleiche Wand wie die Leiter).
- Dach: y121 u±2 Treppen und y122 u±1 Treppen (je d2..d4), y123 u0 Stufe. Giebel hinten d4: y119–y120 u-1..+1 Bretter, y121 u-1..+1, y122 u0.
- 1 Laterne hängend bei d2, u0, y122.
- Die Leiter steht im Gang an der Außenwand (z1045 bzw. z1063). Den Gang dort nicht nach außen verbreitern.

### 2.5 Vorgarten

- **Groß (H1, H2, H3):** 3 tief (d-1..d-3) × 11 breit. Zaunreihe d-3 mit Tor bei u0, Seitenzäune u±5 (d-1, d-2) → 14 Zäune und 1 Tor. Beete: 2 × 8 Felder (u±1..±4, d-1/d-2). Mittelweg u0 aus Kies (d-1, d-2 und unter der Rundtür d0). Auf den Zaunpfosten u±1 an der Torreihe stehen Laternen.
- **Klein (H4, H5):** 1 Beetreihe unter dem Fenster (4 Felder), Zaunreihe dahinter (8), 1 Seitenzaun, Tor seitlich (s. Lageplan) → 9 Zäune, 1 Tor, 1 Laterne.
- Bepflanzung: Knochenmehl auf die Gras-Beete (Gras und Biom-Blumen wachsen von selbst). Dazu je nach Eingang (s. 2.7) Weizen auf Ackerland oder 1–2 Bambus (auf 2–3 Blöcke kürzen).

### 2.6 Blockliste pro Eingang

| Bauteil | Fichtenstamm | Fichtenbretter | Fichtentreppen | Fichtenstufen | Glasscheiben | Wolle | sonst |
|---|---|---|---|---|---|---|---|
| Front 11 breit (H1–H3) | 38 | 25 | 33 (12 Rund + 21 Dach) | 3 | 10 | 18 | Tür 1, Laterne 2, Bruchstein 2, Lagerfeuer 1 |
| Front 8 breit (H4, H5) | 32 | 19 | 23 (8 + 15) | 3 | 5 | 9 | Tür 1, Laterne 2, Bruchstein 2, Lagerfeuer 1 |
| Kellerhals + Treppe (H1, H2) | 4 | 28 | 35 (20 Dach + 15 Stufen) | 5 | 2 | – | Laterne 2, Fackel 2, (Bruchstein 28 opt.) |
| Kellerhals + Leiter (H3–H5) | 4 | 24 | 12 | 3 | – | – | Leiter 5, Falltür 1, Laterne 1 |
| Garten groß (H1–H3) | – | – | – | – | – | – | Zaun 14, Tor 1, Laterne 2, Kies 3 |
| Garten klein (H4, H5) | – | – | – | – | – | – | Zaun 9, Tor 1, Laterne 1, Kies 3 |

### 2.7 Abweichungen je Eingang (damit keiner gleich aussieht)

| | Breite / Abstieg | Vorhang (Wolle) | Türscheibe (d1 um die Tür) | Kamin | Garten |
|---|---|---|---|---|---|
| **H1 Beutelsend** | 11 / Treppe | **Rot** | Fichtenbretter | u+4 (Nordende), **Signalfeuer** (Heuballen statt oberem Bruchstein → hoher Rauch) | links Weizenbeet (Ackerland), rechts Blumen; Namensschild am Tor |
| **H2 Tukhaus** | 11 / Treppe | **Gelb** | entrindete Fichtenstämme (Axt) | u-4 (Nordende) | Blumenwiese, 2 Bambus in den Ecken |
| **H3 Festwiesen-Tor** | 11 / Leiter | **Magenta** | Fichtenbretter | u+4 (Westende), **Signalfeuer** | nur Blumen, Blick auf die Burghaus-Tür |
| **H4 Hornbläser** | 8, Fenster links (West) / Leiter | **Orange** (9) | Fichtenstamm mit Rinde | u-4 (über dem Fenster) | Kräuterbeet unter dem Fenster |
| **H5 Brandyhaus** | 8, Fenster rechts (Ost) / Leiter | **Weiß** | entrindete Fichtenstämme | u+4 (über dem Fenster) | Blumen unter dem Fenster |

Weitere freie Varianten: Vordach bei H4/H5 aus 3 Fichtentreppen statt Stufen. Ein Knopf (Fichte) als „Türknauf“ auf die Scheibe neben der Tür.

### 2.8 Festplatz und Wege

- **Festplatz** vor der Burghaus-Tür: x -1427..-1419, z1053..1055, aus Kies (23 Blöcke). Das Feld (-1423, 1053) mit der Stufe vor der Tür bleibt, wie es ist. An den 4 Ecken steht je ein Laternenpfahl (2 Zäune übereinander + Laterne).
- **2 Bänke** am Platz: je 2 Fichtentreppen, an den Seiten Schilder als Armlehnen.
- **Wege** (`=`, ca. 30 Felder) als Trampelpfad: Rechtsklick mit der Schaufel auf Gras, kein Material nötig. Sie führen im Bogen:
  - Platz → Westgasse x -1428 → Bogen über z1048 (nördlich von H1) → H4-Tor
  - Platz → Ostgasse x -1418 (an den Kisten vorbei) → Bogen über z1048 → H5-Tor
  - Platz → H1-Tor, Platz → H2-Tor, Platz → H3-Tor
- Weitere Laternenpfähle an den Wegbögen: (-1429, 1050), (-1436, 1048), (-1418, 1047), (-1411, 1048).

---

## 3. Untergrund: Abstiege und gemeinsamer Ring-Gang

```
     x -1446 ... -1400  (Ebene y113, Gang y113-115, Boden y112)
1043              ..................................
1044             RRRR...............................
1045             RRRR                               
1046      4=================h=============5====     
1047      =++++++RRRR++++++++++++++++++++++++==     
1048      =+eeee RRRR     ...........       .==     
1049      =+eeee RRRR aaaaaa bbbbbbbbbb      ==     
1050      =+eeee RRRR aaaaaa bbbbbbbbbb      ==     
1051      =+eeee RRRR aaaaaa bbbbbbbbbb      ==     
1052      =+     RRRR aaaaaa bbbbbbbbbb      ==     
1053      =11111 RRRR aaaaaa bbbbbbbbbb 22222==     
1054      =11111      aaaaaa bbbbbbbbbb 22222==     
1055      =11111                        22222==     
1056      =+                                 ==     
1057      =+ccccccc          SSSS      ddddd ==     
1058      =+ccccccc     SSS SSSSSS     ddddd ==     
1059      =+ccccccc    SSSS SSSSSSS    ddddd ==     
1060      =+ccccccc    SSSS  SSSSSSS   ddddd ==     
1061 ..   =++++++++++++++++++++++++++++++++++==     
1062 .... ==================3==================     
1063 ....                                           
1064 ......                                   .     
1065 .......          ........                .     
```
Legende: `=` bestehender Ring-Gang · `+` optionale Verbreiterung auf 2 (Innenseite) · `1`/`2` Treppen von H1/H2 · `3`/`4`/`5` Leiterschächte von H3/H4/H5 · `h` Burghaus-Leiter · `R` Schienen-Sperrzone · `S` bestehender Hohlraum (Farmraum S) · `a b c d e` Plätze für spätere Farmräume · `.` sonstige vorhandene Hohlräume (Zeile 1043/44 = Minecart-Tunnel)

**Ring-Gang (Ziel: einheitlich 3 hoch, y113–115, Boden y112):**
- Nord- und Westgang sind schon 3 hoch. Süd- und Ostgang sind meist nur 2 hoch → y115 nachgraben (ca. 60 Blöcke; nötig, damit man von den Treppen H2 und den Leitern H3 bequem in den Gang kommt).
- Bodenlöcher auf y112 mit Bruchstein füllen (ca. 20), **außer** bei x -1433, z1045/1046: Dort liegen Schienen im Boden.
- **Licht:** 20 Laternen hängend an der Decke (y115, an y116), alle ~6 Blöcke: Nordgang 7, Südgang 7, Westgang 3, Ostgang 3. Die vorhandenen Fackeln bleiben.
- **Optional:** Gang auf 2 breit verbreitern (`+`, nur nach innen; ca. 210 Blöcke Abbau, das bringt Bruchstein und Erde). Ausnahmen: Sperrzone R (x -1434..-1431), die Stufen von H1 bei z1053..1055 und alle Wände, an denen eine Leiter hängt.

**Abstiege:** siehe Tabelle in Abschnitt 1. H1 und H2 kommen auf Gang-Höhe direkt im West- bzw. Ostgang an, H3 im Südgang, H4 und H5 im Nordgang (H4 genau an der NW-Ecke).

**Platz für spätere Farmräume** (nur andeuten: je ein Fichtenschild an der Gang-Innenwand. Räume y113–116, Decke y117 bleibt, damit der Hof oben 2 Blöcke Boden behält):

| Platz | Bereich (x / z) | Zugang |
|---|---|---|
| a | -1429..-1424 / 1049..1054 | vom Nordgang |
| b | -1422..-1413 / 1049..1054 (größter) | vom Nordgang |
| c | -1439..-1433 / 1057..1060 | vom Westgang |
| d | -1412..-1408 / 1057..1060 | vom Ostgang |
| e | -1439..-1436 / 1048..1051 | vom Westgang |
| S | Hohlraum x -1430..-1418 / z1056..1061 (y110–116) | schon offen zum Südgang; nur begradigen, **nicht nach oben öffnen** |
| tief | Höhle y105 (x -1447..-1439, z1044..1057) und alles unter y112 | über die Burghaus-Leiter (geht bis y105) |

---

## 4. Materialbilanz

### 4.1 Gesamtbedarf (alle 5 Eingänge, Platz, Wege, Ring)

| Material | Bedarf | Vorrat | Ergebnis |
|---|---|---|---|
| Fichtenstamm (gesetzt) | 198 (+21 für die Stamm-Türscheiben H2/H4/H5) | ~450 (zusammen mit dem Craft-Bedarf unten) | s. 4.2 |
| Fichtenbretter (gesetzt) | ~230 | ~100 | craften |
| Fichtentreppen | 255 | – | craften |
| Fichtenstufen | 34 | – | craften |
| Fichtenzaun | 76 | 2 | 74 craften |
| Fichtenzauntor | 5 | – | craften |
| Fichtentür | 5 | – | craften |
| Fichtenfalltür | 3 | – | craften |
| Leitern | 15 | ~145 | ok |
| Glasscheiben | 44 | ~120 | ok |
| Wolle | 72 (Rot 18, Gelb 18, Magenta 18, Orange 9, Weiß 9) | ~120 weiß | ok (63 färben) |
| Farbstoff | Rot 18 / Gelb 18 / Magenta 18 / Orange 9 | 58 / 25 / 90 / 16 | ok |
| Laternen | 53 (Front 10, Kellerhals 7, Garten 8, Pfähle 8, Ring 20) | – | craften |
| Fackeln | 57 (53 für Laternen + 4 Treppe) | ~250 | ok |
| Eisen | 48 Barren (→ 424 Nuggets für Laternen) | reichlich | ok |
| Lagerfeuer | 5 | – | craften (Kohle 5, Stöcke 15, **Kirschstämme 15**) |
| Heuballen (Signalfeuer H1, H3) | 2 | – | 18 Weizen (Vorrat ~400) |
| Bruchstein | ~30 (Kamine 8–10, Gangboden ~20); +28 je Treppe für die optionale Wandverkleidung | ~1300 | ok |
| Kies | ~38 (Platz 23, Gartenwege 15) | ~200 | ok |
| Schilder | ~15 (Namen 5, Farmplätze 6, Bänke 4) | viele | ok |
| Knochenmehl | ~60 | 247 Knochenblöcke | ok (7 Blöcke reichen) |
| Stöcke | ~85 (Zäune 50, Tore 20, Lagerfeuer 15) | ~460 | ok |
| Erde / Gras | nur zum Flicken (Baumlöcher) | 1950 / 490 | ok. Durch das Ausheben kommen ~120 Erde dazu |

**Nicht gebraucht** (bleibt im Lager): Stein, Steinziegel, Steinziegeltreppen, Steinstufen, Bruchsteinmauer, Kirschstämme bis auf 15, Bambusbretter, Glas (Blöcke), Kirsch- und Fichtensetzlinge.

### 4.2 Craft-Liste

| Craft | Rezept | Anzahl Crafts | Bretter |
|---|---|---|---|
| Fichtentreppen 256 | 6 Bretter → 4 | 64 | 384 |
| Fichtenstufen 36 | 3 Bretter → 6 | 6 | 18 |
| Fichtenzaun 75 | 4 Bretter + 2 Stöcke → 3 | 25 | 100 (+50 Stöcke) |
| Fichtenzauntor 5 | 2 Bretter + 4 Stöcke → 1 | 5 | 10 (+20 Stöcke) |
| Fichtentür 6 | 6 Bretter → 3 | 2 | 12 |
| Fichtenfalltür 4 | 6 Bretter → 2 | 2 | 12 |
| Bretter gesetzt | – | – | ~230 |
| **Summe Bretter** | | | **≈ 766** → minus 100 im Vorrat = **~670 Bretter = 168 Fichtenstämme** |
| Entrindete Stämme (Türscheiben H2, H5) | Axt-Rechtsklick auf Stamm | 14 | – |
| Laternen 53 | 8 Eisennuggets + 1 Fackel | 53 | (48 Eisenbarren → Nuggets) |
| Lagerfeuer 5 | 3 Stöcke + 1 Kohle + 3 Stämme | 5 | (Kirschstämme) |
| Heuballen 2 | 9 Weizen | 2 | |
| Wolle färben 63 | 1 Wolle + 1 Farbstoff | 63 | |
| Knochenmehl ~63 | 1 Knochenblock → 9 | 7 | |

**Fichten-Bilanz:** 198 + 21 (gesetzt) + 168 (für Bretter) = **~387 Stämme bei ~450 Vorrat → Rest ~60.** Es reicht, ist aber knapp. Wird es eng, zuerst die Dachtreppen der Kellerhälse durch Fichtenstufen ersetzen (spart ~40 Treppen).

---

## 5. Bau-Reihenfolge (für den Bot)

1. **Vorbereiten:** Der Spieler hat die Bäume gefällt. Danach im Baufeld entfernen (und wiederverwenden):
   - Fackeln bei (-1444,1050), (-1438,1052), (-1432,1053), (-1421..-1417,1054), (-1419,1053), (-1421,1060), (-1411,1049)
   - Fichtenzäune bei (-1429,1049), (-1429,1050), (-1428,1052) (Gehege? vorher klären)
   - Baumlöcher mit Erde/Gras füllen

   Danach alles craften (Liste 4.2) und eine Baukiste in der Hofmitte aufstellen.
2. **Abstecken:** Je Eingang die Türposition und die vier Front-Ecken mit Kies auf y119 markieren und die Kollisionen mit dem Lageplan prüfen.
3. **Untergrund zuerst** (Zugang über die Burghaus-Leiter x -1423 z1046 bis y113):
   - Ring auf 3 Höhe bringen, Boden flicken, Laternen setzen. Optional verbreitern.
   - **Leiterschächte H3, H4, H5:** von oben je 3 Blöcke (y118–116) ausheben. Leitern von oben nach unten an die Rückwand d4 setzen (y117 → y113), dann die Falltür (obere Hälfte) auf y118.
   - **Treppen H1, H2:** von oben stufenweise ausheben (Längsschnitt 2.3). Danach die Fichtentreppen **von unten nach oben** setzen: Der Bot steht im Gang bzw. auf der vorigen Stufe und blickt Richtung Tür (−d).
4. **Fronten** je Eingang in dieser Reihenfolge:
   1. Pfosten u±5 (d0 und d1, y119–122)
   2. Balken y123: Stamm liegend, dazu die Seitenfläche des Nachbarn in u-Richtung anklicken
   3. **Erst d1**: Bretter, Wolle-Vorhänge, Türscheibe. Der Bot steht davor, d0 ist ja noch leer.
   4. **Dann d0** von unten nach oben: y119 Bretter und L/J. y120 L/J der Fenster. y122 Bretter und die oberen F/7 (Klick auf die Balken-Unterseite). y121 F/7 der Tür (Klick auf die Unterseite der Bretter y122). Zuletzt die Glasscheiben.
   5. Die Tür von vorn setzen (Blick +d).
   6. Dachtreppen y124: d0 von vorn (Blick +d), d1 von hinten (Blick −d).
   7. Kamin (Bruchstein/Heuballen), Lagerfeuer zuletzt.
   8. Vordach-Stufen (obere Hälfte der Frontfläche y122 anklicken) und die Laternen darunter.
5. **Kellerhälse:** Bodenbretter, Seitenwände (Bretter, hinten Stamm-Ecken), Rückgiebel, Dach (y121 u±2 Treppen zur Mitte, y122 u±1 Treppen zur Mitte, y123 Firststufe), Laternen innen.
6. **Gärten:** Beete (Knochenmehl, Ackerland bei H1), Kies-Mittelweg, Zäune und Tore, Laternen auf die Pfosten am Tor.
7. **Festplatz** (Kies), Laternenpfähle, Bänke, dann die Trampelpfade mit der Schaufel.
8. **Abnahme:** Jede Tür öffnen und den Abstieg einmal ganz begehen (Kopffreiheit!). Prüfen, dass alle Lagerfeuer brennen und rauchen. Bei Nacht prüfen, dass Hof und Gang hell genug sind (keine Spawns bei Licht 0).

### Worauf achten
- **Nicht verbauen:** Burghaus (x -1426..-1419, z1044..1052 inkl. Nordgarten mit Mauerstücken), Kisten (z1050–1052), Eisentür (-1423, 1052) und die Stufe davor (-1423, 1053), Wasser/Feld NW (x -1447..-1445, z1046..1047), alle Mauern. Die Terrasse und das Haus nördlich der Mauer bleiben unberührt.
- **Unter der Erde nicht anfassen:** Burghaus-Leiter x -1423 z1046 (und ihre Wand z1045), Minecart-Rampe und -Tunnel (Sperrzone R, Zeilen z1043/44), die dünne Decke über dem Hohlraum Süd bei x -1420..-1415, z1058..1062.
- **H1-Front** steht über der Minecart-Rampe (x -1433). Das ist unkritisch (6 Blöcke Fels dazwischen), dort aber nicht tiefer als y118 graben.
- **Lagerfeuer:** Darüber muss alles frei sein, sonst gibt es keinen Rauch. Sie zünden kein Holz an, beim Bauen trotzdem nicht darauf treten.
- **Treppen und Türen** richten sich nach der Blickrichtung. Bei kopfüber-Treppen die Unterseite des Blocks darüber anklicken. Falltür an der Leiterwand und auf derselben Seite wie die Leiter, sonst kann man nicht hinausklettern.
- **Reichweite ~4,5:** Für Dach und Kamin (y124–126) auf das Kellerhals-Dach oder ein Gerüst (Erde, wieder abbauen) stellen.
- Die Glasscheiben verbinden sich optisch mit den Nachbarn. Das ist gewollt, das Fensterkreuz wirkt dadurch runder.

---

## 6. Farm-Liste (fehlt oder sollte noch beschafft werden)

| Was | Wofür | Woher |
|---|---|---|
| **Weizensamen** | Weizenbeet H1 | aus Gras (Knochenmehl → Gras abbauen) |
| **Ton → Blumentöpfe** | Blumen auf Fensterbänken und an den Toren | Ton aus Flussbett/Sumpf/Mangroven |
| **Grüner Farbstoff** (Kaktus) | klassisch grüne Beutelsend-Tür als Woll-Scheibe, grüne Vorhänge | Kaktus (Wüste) → Ofen |
| **Blauer Farbstoff** (Kornblume/Lapis) | blaue Vorhänge oder Tür | Kornblumen per Knochenmehl, Lapis aus dem Bergbau |
| **Mehr Blumen** (Flieder, Rosenbusch, Pfingstrose, Sonnenblume, Rosablüten) | üppige Vorgärten | Doppelblumen per Knochenmehl vermehrbar; Rosablüten im Kirschhain |
| **Laub** (Kirsch-/Fichtenlaub) | Hecken und Büsche an den Zäunen | Schere (2 Eisen) |
| **Moos / Azaleen** | bewachsene Optik an Pfosten und Wegen | Üppige Höhle, Wandernder Händler |
| **Kerzen** (Honigwabe) | Fensterbank und Vorraum | Bienennest |
| **Kürbis, Karotten, Kartoffeln** | Gemüsegarten | Dorf, Plündererbeute |
| **Fichtenstämme-Nachschub** (+100) | Reserve, Rest nach dem Bau nur ~60 | Fichtenfarm mit den vorhandenen Fichtensetzlingen (2×2 → Riesenfichte) |

Spätere **Farmen** (Untergrund, Plätze a–e, S und die tiefe Ebene) sind bewusst nicht Teil dieses Plans.
