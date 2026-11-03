# Dach für das Kirschholz-Langhaus – 3 Varianten (nur Planung)

**Haus:** Außenmaß x −1447…−1397 (51) × z 1023…1032 (10), Boden y 128, Wandkrone/Flachdecke y 132 (Kirschbretter, Kirschstamm-Balken auf z 1023 und z 1032).
**Grundsatz für alle Varianten:** Die Flachdecke y 132 bleibt unverändert als Decke und Dachbodenboden. Das Dach beginnt auf y 133. Der dunkle Kirschstamm-Balken auf y 132 bleibt unter der Traufe als dunkles Band sichtbar, weil die Traufe erst auf y 133 sitzt.
**Hauptmaterial:** Tiefenschieferfliesen (deepslate_tiles), die dunkelste und „sauberste“ Tiefenschiefer-Optik, die am besten zu den dunklen Kirschstamm-Pfeilern passt. Alles wird mit der Steinsäge aus Bruchtiefenschiefer (cobbled_deepslate) geschnitten.

## Angenommene Rezepte

| Ausgang | Werkzeug | Ergebnis | Sicherheit |
|---|---|---|---|
| 1 cobbled_deepslate | Steinsäge | 1 deepslate_tile_stairs | sicher (seit 1.17 Direktschnitt) |
| 1 cobbled_deepslate | Steinsäge | 2 deepslate_tile_slab | sicher |
| 1 cobbled_deepslate | Steinsäge | 1 deepslate_tile_wall / 1 deepslate_tiles | sicher |
| 1 cobbled_deepslate | Steinsäge | 1 cobbled_deepslate_stairs / 2 Stufen | sicher |
| 4 cobbled → 4 polished → 4 bricks → 4 tiles | Werkbank 2×2 | jeweils 1:1 | sicher, aber unnötig, weil die Steinsäge direkt geht |
| 6 Blöcke → 4 Treppen, 3 Blöcke → 6 Stufen | Werkbank | – | schlechter als die Steinsäge, also **nicht** verwenden |
| 8 Eisennuggets + 1 Fackel | Werkbank | 1 Laterne | sicher |
| 1 Eisenbarren + 2 Nuggets | Werkbank | 1 Kette | sicher. In neueren Versionen heißt sie „iron_chain“, das Rezept bleibt gleich (Name **unsicher** für 26.3) |
| 1 Kirschstamm | Werkbank | 4 Kirschbretter | sicher |

**Annahme (unsicher):** Java 26.3 hat dieselben Steinsägen-Rezepte wie die 1.21-Versionen.
Natürlichen Tiefenschiefer **ohne Behutsamkeit** abbauen, dann droppt direkt cobbled_deepslate.
Die 182 vorhandenen cobbled_deepslate_stairs lassen sich nicht zurückverwandeln. Sie werden nur dort eingesetzt, wo ihre gröbere Textur gewollt wirkt (Ortgang, Sparoptionen).

**Legende für alle Schnitte (Blick von Westen, Nord links ↔ Süd rechts):**
`/` Treppe, steigt nach Süden · `\` Treppe, steigt nach Norden · `_` untere Stufe · `¯` obere Stufe · `P` Kirschbretter · `L` Kirschstamm · `F` Glasscheibe · `B` bestehender Kirschstamm-Balken y132 · `=` bestehende Decke y132 · `W` Wand · `o` Laterne · `k` Kette.
Innerhalb des Hauses sind P/L/F in der Schnittdarstellung Luft (Dachboden). Die Buchstaben zeigen die **Giebelwand** bei x −1447 bzw. x −1397.

---

## Variante A – Klassisches 45°-Satteldach, First Ost-West, 1 Block Überstand

### 1. Beschreibung
- Volle Treppen-Neigung 45°, 6 Treppenreihen je Seite. Der First besteht aus zwei Rücken an Rücken liegenden Treppen auf y 138 (Oberkante y 139), also 6 Blöcke über der Decke.
- Überstand 1 Block rundum: Traufe z 1022 / z 1033, Ortgang x −1448 / x −1396. Dachlänge 53.
- Proportion: Bei 10 Breite ergibt das ein kräftiges, klassisches Dach. Bei 51 Länge wirkt es allerdings sehr lang und einförmig. Mit Quergiebel über der Tür (siehe Zusatzmodul unten) wird es deutlich besser.

### 2. Querschnitt und Giebel
```
  z →   22 23 24 25 26 27 28 29 30 31 32 33
y138                   /  \
y137                /  L  L  \
y136             /  L  F  F  L  \
y135          /  P  L  F  F  L  P  \
y134       /  P  P  P  L  L  P  P  P  \
y133    /  L  P  P  P  P  P  P  P  P  L  \
y132    o  B  =  =  =  =  =  =  =  =  B  o
y131       W                          W
```
Der Giebel ist als Kirsch-Fachwerk ausgeführt. Die Eckpfosten auf y133 (L bei z1023/z1032) setzen die Wandpfeiler fort. In der Mitte sitzt ein 2×2-Giebelfenster (Scheiben) mit Kirschstamm-Rahmen: Sturz und Brüstung liegen waagerecht, die Laibungen stehen senkrecht. Pro Giebel sind das 30 Felder: 10 L, 16 P und 4 F.

### 3. Blockliste A

| Position | Rechnung | Stück |
|---|---|---|
| Dachflächen deepslate_tile_stairs | 2 Seiten × 6 Reihen × 53 | **636** |
| Ortgang (umgedrehte Treppen unter der Überstandskante x −1448/−1396) | 2 × 12 | **24** (aus Lager-cobbled_deepslate_stairs) |
| Giebel cherry_log | 2 × 10 | **20** |
| Giebel cherry_planks | 2 × 16 | **32** |
| Giebel glass_pane | 2 × 4 | **8** |
| Laternen unter der Traufe (y132, z1022 und z1033, an allen 12 Pfeiler-x je Seite) | 2 × 12 | **24** |
| Fackeln auf dem Dachboden (y133, z1027, x −1442/−1432/−1422/−1412/−1402) | – | **5** |
| First | durch das Treppenpaar bereits geschlossen, kein Zusatzmaterial | 0 |

### 4. Lagerabgleich A
- Tiefenschiefer: 636 Fliesentreppen = **636 cobbled_deepslate farmen**. Ortgang aus dem Lager (24 von 182).
  - *Sparoption:* Die unterste Traufreihe beidseitig (2 × 53 = 106) aus Lager-Bruchtreppen als bewusst raues „Traufband“ setzen. Dann sind nur noch **530** zu farmen (Lagerverbrauch 130, Rest 52).
- Kirsche: 32 Bretter = 10 Lager + 22 → 6 Stämme. Insgesamt 26 von 33 Stämmen ✓
- Glasscheiben 8/128 ✓, Fackeln ✓
- Eisen: 24 Laternen = 192 Nuggets ≈ 22 Barren → **3 Eisenblöcke** ✓

---

## Variante B – „Japanisches“ Knickdach mit geschwungener Traufe (Kirschblüten-Stil) ★ Empfehlung

### 1. Beschreibung
- Unten ist die Traufe flach (2 Reihen Stufen, 2 Blöcke Überstand), oben läuft das Dach mit 45° steil zum First. Dieser Knick erzeugt die typische geschwungene Silhouette japanischer Dächer und passt damit perfekt zu Kirschholz und Kirschbäumen.
- Überstand: Traufe 2 Blöcke (z 1021 / z 1034), Ortgang 2 Blöcke (x −1449 / x −1395). Dachlänge 55.
- First wie bei A auf y 138, dazu ein **Firstband** aus 2 Reihen Fliesenstufen auf y 139 (Oberkante y 139,5) als dunkler „Munagi“-Akzent. An den Firstenden sitzen je 2 Fliesenmauern statt Stufen als kleine „Hörner“ (Onigawara-Andeutung).
- Eckaufschwung: An den 4 Ecken werden die 2 äußersten Traufstufen (z1021/z1034) als **obere** Stufe gesetzt. Die Eckstufe selbst kommt als untere Stufe eine Ebene höher auf y 134 (mit einem Hilfsblock setzen). Daraus ergeben sich hochgezogene Dachspitzen, und die Stückzahl bleibt gleich.
- Der tiefe Überstand beschattet die rosa Fassade und bricht die 51 Blöcke Länge optisch.

### 2. Querschnitt und Giebel
```
  z →   21 22 23 24 25 26 27 28 29 30 31 32 33 34
y139                      _  _
y138                      /  \
y137                   /  L  L  \
y136                /  L  F  F  L  \
y135             /  P  L  F  F  L  P  \
y134          /  P  P  P  L  L  P  P  P  \
y133    _  ¯  L  P  P  P  P  P  P  P  P  L  ¯  _
y132    k     B  =  =  =  =  =  =  =  =  B     k
y131    o     W                          W     o
```
Der Giebel ist identisch mit A (30 Felder pro Seite, Kirsch-Fachwerk mit Fenster). Er liegt 2 Blöcke hinter der Dachkante zurück und wirkt dadurch wie ein Teehaus-Giebel.

### 3. Blockliste B

| Position | Rechnung | Stück |
|---|---|---|
| deepslate_tile_stairs (Dachflächen) | 2 Seiten × 5 Reihen × 55 | **550** |
| deepslate_tile_slab Traufe (unten + oben) | 2 Seiten × 2 Reihen × 55 | **220** |
| deepslate_tile_slab Firstband | 2 × 55 − 4 | **106** |
| deepslate_tile_wall Firstenden | 2 Enden × 2 | **4** |
| Ortgang, umgedrehte Treppen unter x −1449 (nur unter den Treppenzellen) | 2 × 10 | **20** (Lager-cobbled_deepslate_stairs) |
| Giebel cherry_log / cherry_planks / glass_pane | wie A | **20 / 32 / 8** |
| Laternen an 1 Kette unter der Traufe (Kette y132, Laterne y131, z1021/z1034, an den 12 Pfeiler-x) | 2 × 12 | **24 Laternen + 24 Ketten** |
| Dachboden-Fackeln | wie A | **5** |

### 4. Lagerabgleich B
- Tiefenschiefer: 550 (Treppen) + 326 Stufen / 2 = 163 + 4 (Mauern) = **717 cobbled_deepslate farmen**.
  - *Sparoption 1:* Die Rückseite (Nord) mit den unteren 2 Treppenreihen (2 × 55 = 110) aus Lager-Bruchtreppen bauen. Dann sind es **607**.
  - *Sparoption 2:* Das Firstband weglassen (−106 Stufen = −53). Dann sind es **664**, mit beiden Optionen **554**.
- Kirsche, Glas und Fackeln wie bei A ✓
- Eisen: 192 + 48 Nuggets = 240 Nuggets ≈ 27 Barren, dazu 24 Barren für die Ketten, macht 51 Barren → **6 Eisenblöcke** ✓

---

## Variante C – Flaches Satteldach (Stufe/Treppe im Wechsel, ca. 23°), 1 Block Überstand

### 1. Beschreibung
- Untere Stufe und Treppe wechseln sich ab. Das Dach steigt pro 2 Blöcke um 1 Block, First auf y 135 (Oberkante y 136), also nur 3–4 Blöcke über der Decke.
- Die Optik ist niedrig, lang und ruhig, wie ein Bungalow oder Teehaus. Durch den Reihenwechsel entstehen feine Querstreifen.
- Spawnsicher, weil auf unteren Stufen und Treppen keine Mobs spawnen (bei oberen Stufen wäre das anders, daher dieses Muster).
- Überstand 1 rundum, Länge 53.

### 2. Querschnitt und Giebel
```
  z →   22 23 24 25 26 27 28 29 30 31 32 33
y135                _  /  \  _
y134          _  /  L  L  L  L  \  _
y133    _  /  P  P  L  F  F  L  P  P  \  _
y132    o  B  =  =  =  =  =  =  =  =  B  o
y131       W                          W
```
Der Giebel ist klein (12 Felder pro Seite): ein Kirschstamm-Sturzbalken und ein 2×1-Fenster.

### 3. Blockliste C

| Position | Rechnung | Stück |
|---|---|---|
| deepslate_tile_stairs | 2 Seiten × 3 Reihen × 53 | **318** |
| deepslate_tile_slab | 2 Seiten × 3 Reihen × 53 | **318** |
| Giebel cherry_log / cherry_planks / glass_pane | 2 × (6 / 4 / 2) | **12 / 8 / 4** |
| Laternen direkt unter der Traufstufe | 2 × 12 | **24** |
| Dachboden-Fackeln | – | **5** |

### 4. Lagerabgleich C
- Ganz in Fliesen: 318 + 159 = **477 cobbled_deepslate farmen**.
- *Zweiton-Variante:* Alle Treppenreihen aus cobbled_deepslate_stairs (182 aus dem Lager + 136 neu), die Stufenreihen aus Fliesen. Daraus ergeben sich bewusste Streifen. Zu farmen sind dann **295**.
- Kirsche: nur Lagerbestand (8 Bretter, 12 Stämme) ✓. Eisen: 3 Blöcke ✓

---

## Zusatzmodul: Quergiebel über der Haustür (für A oder B)
- Er überspannt x −1427…−1418 (10 breit, mittig über der Tür −1423/−1422, zwischen den Pfeilern). Der First läuft Nord-Süd und trifft genau auf den Hauptfirst y 138 bei z 1028. Die Giebelfront liegt in der Wandebene z 1032 mit 1 Block Überstand bis z 1033.
- Dachflächen: je Seite 6 Reihen, die hinten in den Hauptdach-Kehlen enden (Längen 0+1+2+3+4+5). Das ergibt **+30 Fliesentreppen**. Die Kehlen bilden sich als Eck-Treppen automatisch.
- Die Giebelfront über der Hauptdachfläche hat ca. **20 Felder Kirsche** (z. B. 4 Stämme Rahmen, 14 Bretter, 2 Scheiben).
- Hauptdach-Treppen unter dem Quergiebel können entfallen (ca. 20). **Netto etwa +10 Treppen** (Näherung, ±5, im Spiel nachzählen).
- Kirsche wird dann knapp: Nach A/B bleiben 7 Stämme und 2 Bretter. Fürs Modul braucht man ca. 5 Stämme → Rest 2. Mehr Kirsche gibt es von den Kirschbäumen, die sowieso gestutzt werden.

---

## 5. Bauhinweise (alle Varianten)
1. **Bäume und Blätter:** Die südlichen Kirschkronen (x −1449…−1425, z 1033–1037, y 130–137) kollidieren mit Traufe, Ortgang-Ecke und Laternen. Freischneiden:
   - A/C: z 1033–1034, y 131–137
   - B: z 1033–1035, y 131–137, dazu die Ecke x −1449
   Am besten mit der **Schere** abbauen, dann bleiben die Blätter als Deko erhalten. Kronen weiter draußen dürfen das Dach berühren, das sieht schön aus. Laternen aber nicht in Blättern verstecken.
2. **Material vorbereiten:** Alles an der Steinsäge schneiden. Ziel: Treppen, Stufen und Mauern komplett fertig, damit beim Bau nicht nachgeschnitten werden muss.
3. **Giebel zuerst** (x −1447 und x −1397): Sie dienen als Lehre. Die erste Treppensäule wird an jedem Ende auf den Giebel gesetzt, danach jede weitere Treppe gegen die **Seitenfläche** der Nachbartreppe in x-Richtung. So entsteht kein schwebendes Bauproblem.
4. **Dachboden beleuchten, bevor er geschlossen wird:** 5 Fackeln auf y 133. Ohne Licht spawnen im geschlossenen Dachboden Mobs (bei A/B ist er bis 5 hoch, bei C 2 hoch). Später kommt man nur noch durch das Giebelfenster hinein (Scheibe abbauen).
5. **Dachflächen von unten nach oben:** Pro Seite erst die Traufreihe, dann Reihe für Reihe bis zum First.
   - Beim Setzen in Richtung First blicken.
   - Auf die **untere Hälfte** der Nachbarfläche klicken, sonst wird die Treppe/Stufe kopfüber gesetzt.
   - Die oberen Stufen in B (z1022/z1033) dagegen bewusst auf die obere Hälfte klicken.
6. **Gerüst:** Kaum nötig. Die bestehende Flachdecke y 132 ist die perfekte Arbeitsbühne. Für Überstände und Ortgang an der Kante schleichen (Shift). Wo ein Hilfsblock gebraucht wird (Eckaufschwung B, Ortgang), Bambusbretter nehmen (400 im Lager) und danach wieder abbauen. Absturzhöhe ca. 5 Blöcke, ungefährlich.
7. **Zum Schluss:** First bzw. Firstband (B), Ortgang aus umgedrehten Treppen, dann Laternen bzw. Ketten unter der Traufe an den Pfeiler-x (−1447, −1442, −1437, −1432, −1427, −1425, −1420, −1418, −1413, −1408, −1403, −1397). Die Laternen bei −1425 und −1420 flankieren die Tür.
8. **Innenraum:** Er bleibt unverändert. Die Decke y 132 wird nicht angerührt.

---

## Empfehlung
**Variante B (Knickdach im japanischen Stil)**, optional mit Quergiebel über der Tür.
- Kirschholz plus Kirschblüten plus schwarze Fliesen ergibt einen japanisch-stimmigen Look. Die geschwungene Traufe und das dunkle Firstband geben dem sehr langen, schmalen Baukörper (51 × 10) Spannung, die ein glattes 45°-Dach (A) nicht hat.
- Der tiefe Überstand rahmt die rosa Fassade, und die Laternen an Ketten verstärken den Stil.
- Der Mehraufwand gegenüber A beträgt nur ca. 80 Bruchtiefenschiefer. Wenn das Farmen zu viel ist, ist **C** die Budgetwahl mit 295–477.

## Gesamt-Farm-Liste für B (Fliesen pur, Ortgang aus Lager)

| Was | Menge | Hinweis |
|---|---|---|
| **Bruchtiefenschiefer (cobbled_deepslate)** | **717** (≈ 11 Stacks + 13) | dazu ca. 5 % Reserve → **~750 abbauen**. Tiefe y < 0, ideal y −50…−58 (dabei auch Diamanten). Ohne Behutsamkeit abbauen, Effizienz-Spitzhacke empfohlen (Tiefenschiefer baut sich langsam ab) |
| davon an der Steinsäge | 550 → 550 Fliesentreppen, 163 → 326 Fliesenstufen, 4 → 4 Fliesenmauern | |
| mit Sparoptionen | 607 (Rückseite unten Bruchtreppen) / 554 (zusätzlich ohne Firstband) | |
| Kirschholz | 0 (26 von 33 Stämmen aus Lager) | mit Quergiebel +5 Stämme, noch im Lager. Mehr holt man von den gestutzten Bäumen |
| Eisen | 0 (6 von 435 Eisenblöcken → 24 Laternen + 24 Ketten) | |
| Fackeln | 29 (24 für Laternen + 5 Dachboden) aus Lager | |
| Glasscheiben | 8 aus Lager | |
| Bambusbretter | einige als Hilfsblöcke, aus Lager | |

---

# Variante B mit Fensterdächern (Update nach Besitzerwunsch)

**Wunsch:** „Bei jedem Fenster oberhalb so ein Seitendach, nicht alles durchgängig.“ Basis ist Variante B (Knickdach, Traufe 2 Blöcke Überstand, First y138, Firstband y139, Länge x −1449…−1395).

**Fenster (Südwand z 1032, Glas y130–131):** −1445/−1444, −1440/−1439, −1435/−1434, −1430/−1429, −1416/−1415, −1411/−1410, −1406/−1405, −1401/−1400. Jedes Fenster sitzt mittig in einem 4 Blöcke breiten Feld zwischen zwei Pfeilern.
**Tür:** x −1423/−1422 mit Glassäulen bei −1426 und −1419 (y129–131).
**Nordseite:** keine Fenster, sie bleibt in beiden Untervarianten durchgehend wie B.

**Lager neu:** 173 cobbled_deepslate, 198 cobbled_deepslate_stairs, 44 deepslate.
Für natürlichen Tiefenschiefer gibt es nach meinem Wissen keinen Steinsägen-Weg zu Fliesen (**unsicher**). Die 44 Blöcke eignen sich als verdeckte Hilfsblöcke. Kirsche, Glas und Eisen sind reichlich vorhanden.

Allgemeine Regel: Fenster (y130–131) und Glassäulen bleiben frei. Alle Zusatzdächer beginnen bei y132 oder höher, Konsolen sitzen nur **neben** den Fenstern.

Legende zusätzlich: `:` Hauptdachfläche (in der Ansicht) · `T` Vordach · `K` Kirsch-Konsole · `G` Glassäule · `D` Tür · `F` Fenster bzw. Giebelfenster · `m` Firsttreppe des Mini-Giebels (im Schnitt längs geschnitten) · `.` Kirschbrett-Wand.

---

## B1 – Zwerchgiebel über jedem Fenster und großer Türgiebel

### Beschreibung
- **Fenstergiebel:** Über jedem Fenster ragt ein kleiner Quergiebel aus der Südfläche.
  - Das Dach ist 4 breit (x = Fenster −1 … Fenster +2), mit Fliesentreppen. Die äußeren Reihen liegen auf y134, die Firstreihen auf y135. Die Firstreihen bilden ein Treppenpaar Rücken an Rücken, eine Firstkappe braucht es nicht.
  - Jede Dachreihe läuft von der Traufkante z1034 nach hinten bis in die Kehle mit dem Hauptdach. Die Reihen auf y134 enden an der Hauptdach-Treppe bei z1032, die auf y135 an der bei z1031.
  - Die Giebelfront liegt in der Wandebene z1032: auf y133 ein waagerechter Kirschstamm-Rähm (4 Stämme) direkt über dem Fenster, auf y134 zwei Kirschbretter.
  - Davor bleiben 2 Blöcke Dachüberstand, wie bei der Haupttraufe.
- **Türgiebel (groß):** Er spannt von x −1427 bis −1418 (10 breit, von Pfeiler zu Pfeiler) mit 45° bis y138 und trifft exakt den Hauptfirst bei z1028.
  - Die Front hat dasselbe Fachwerk wie die Endgiebel (30 Felder mit 2×2-Fenster über der Tür).
  - Die Eckpfosten setzen die Pfeiler −1427 und −1418 nach oben fort.
- **Traufe Süd:** Sie ist **unterbrochen**. Von der Haupttraufe bleiben nur schmale Stücke an den Pfeilern (−1442, −1437, −1432, −1413, −1408, −1403) und die Enden (West −1449…−1447, Ost −1398…−1395, inklusive Eckaufschwung).
  - Unter diesen Stücken hängen die Laternen an Ketten. So ergibt sich der Rhythmus Giebel – Laterne – Giebel.
- **Zwischen Fenster 4 und Tür (x −1428/−1427) sowie Tür und Fenster 5 (−1418/−1417)** stoßen zwei Giebel direkt aneinander. Dort entsteht eine kleine Kehle auf y134. Das ist gewollt.

### Ansicht Südfassade von außen (B1)
```
y139 _______________________________________________________
y138 ::::::::::::::::::::::::::/\:::::::::::::::::::::::::::
y137 :::::::::::::::::::::::::/LL\::::::::::::::::::::::::::
y136 ::::::::::::::::::::::::/LFFL\:::::::::::::::::::::::::
y135 ::::/\:::/\:::/\:::/\::/kLFFLk\::/\:::/\:::/\:::/\:::::
y134 :::/PP\:/PP\:/PP\:/PP\/PkPLLPkP\/PP\:/PP\:/PP\:/PP\::::
y133 ___LLLL_LLLL_LLLL_LLLLLPkPPPPkPLLLLL_LLLL_LLLL_LLLL____
y132   kBBBBkBBBBkBBBBkBBBBBBkBBBBkBBBBBBkBBBBkBBBBkBBBBBk
y131   o.FF.o.FF.o.FF.o.FF.LGo....oGL.FF.o.FF.o.FF.o.FF.Lo
y130   L.FF.L.FF.L.FF.L.FF.LGL.DD.LGL.FF.L.FF.L.FF.L.FF.LL
y129   L....L....L....L....LGL.DD.LGL....L....L....L....LL
y128   ===================================================
         |    |    |    |    |    |    |    |    |    |    |
              -1440     -1430     -1420     -1410     -1400 
```

### Querschnitt durch ein Fenster, x = −1445 (B1)
```
  z →   21 22 23 24 25 26 27 28 29 30 31 32 33 34
y139                      _  _
y138                      /  \
y137                   /        \
y136                /              \
y135             /                    \  m  m  m
y134          /                          P
y133    _  ¯                             L
y132    k     B  =  =  =  =  =  =  =  =  B
y131    o     W                          F
y130          W                          F
y129          W                          W
y128          #  #  #  #  #  #  #  #  #  #
```
Die Haupttraufe ist hier entfernt. Der Mini-Giebel (Firstreihe `m` auf y135) kragt bis z1034 vor. Rähm (L) und Brett (P) bilden die Giebelfront über dem Fenster.

### Blockliste B1

| Position | Rechnung | Stück |
|---|---|---|
| Hauptdach-Treppen | 550 − 16 (je Fenstergiebel 2 durch Front ersetzt) − 20 (Türgiebel: 8 Front + 12 überbaut) | 514 |
| Fenstergiebel-Treppen | 8 × (2 Reihen × 2 + 2 Reihen × 3) = 8 × 10 | 80 |
| Türgiebel-Treppen | je Seite Reihen y134…y138 mit 2+3+4+5+6 = 20, × 2 | 40 |
| **deepslate_tile_stairs gesamt** | | **634** |
| Traufstufen (untere + obere) | 220 − 84 (8 × 4 × 2 unter Fenstergiebeln + 10 × 2 unter Türgiebel) | 136 |
| Firstband-Stufen | wie B | 106 |
| **deepslate_tile_slab gesamt** | | **242** |
| deepslate_tile_wall (Firstenden) | | **4** |
| Ortgang Endgiebel (Lager-Bruchtreppen) | | 20 |
| cherry_log | Endgiebel 20 + Fenstergiebel 8 × 4 + Türgiebel 10 | **62** |
| cherry_planks | Endgiebel 32 + Fenstergiebel 8 × 2 + Türgiebel 16 | **64** (16 Stämme) |
| glass_pane | Endgiebel 8 + Türgiebel 4 | **12** |
| Laternen | Nord 12 + Süd 8 (Pfeilerstücke) + Tür 2 | **22** |
| Ketten | Nord 12 + Süd 8 + Tür 2 × 4 (hängen vom Türgiebel y132–135) | **28** |
| Dachboden-Fackeln | wie B, optional +1 im Türgiebel | 5 (+1) |

**Bruchtiefenschiefer:** 634 + 242 / 2 = 121 + 4 = **759**. Abzüglich Lager 173 bleiben **586 zu farmen**.
- *Sparoption:* Die Nordseite (Rückseite, ohne Fenster) bekommt in den unteren 3 Treppenreihen (3 × 55 = 165) Lager-Bruchtreppen. Dann sind es **421**. Das verbraucht 165 + 20 Ortgang = 185 von 198 Lager-Treppen, Rest 13.

**Eisen:** 22 × 8 + 28 × 2 = 232 Nuggets ≈ 26 Barren, plus 28 Barren für die Ketten = 54 Barren → **6 Eisenblöcke**.

---

## B2 – Hauptdach B durchgehend, zusätzlich Fenster-Vordächer

### Beschreibung
- Das Hauptdach bleibt exakt wie B.
- Über jedem Fenster sitzt ein eigenes Pultdach auf **y132** (direkt über dem Fensterglas y131, verdeckt also nichts):
  - 4 breit (Fenster −1 … +2)
  - z1033: Fliesentreppe, steigt zur Wand hin
  - z1034 und z1035: untere Fliesenstufen
  - Das Vordach ragt 1 Block vor die Haupttraufe hinaus, damit es als eigenes Dach erkennbar ist. Sonst läge es nur ½ Block unter der Traufe und würde wie eine verdickte Traufkante wirken.
- **Konsolen:** kopfüber gesetzte Kirschtreppen auf y131, z1033, links und rechts neben dem Fenster. Sie stehen vor der Wand, nicht vor dem Glas.
- **Tür:** größeres Vordach von x −1427 bis −1418 (10 breit, gleiche 3 Tiefen). Die Konsolen sitzen an den Pfeilern −1427 und −1418, die Glassäulen bleiben frei. 2 Laternen hängen direkt unter dem Vordach bei −1425 und −1420.
- Japanisch gesehen ist das eine „Mokoshi“-artige Schürze unter dem Hauptdach.

### Ansicht Südfassade von außen (B2)
```
y139 _______________________________________________________
y138 :::::::::::::::::::::::::::::::::::::::::::::::::::::::
y137 :::::::::::::::::::::::::::::::::::::::::::::::::::::::
y136 :::::::::::::::::::::::::::::::::::::::::::::::::::::::
y135 :::::::::::::::::::::::::::::::::::::::::::::::::::::::
y134 :::::::::::::::::::::::::::::::::::::::::::::::::::::::
y133 _______________________________________________________
y132   kTTTTkTTTTkTTTTkTTTTTTTTTTTTTTTTTTkTTTTkTTTTkTTTTBk
y131   oKFFKoKFFKoKFFKoKFFKKGo....oGKKFFKoKFFKoKFFKoKFFKLo
y130   L.FF.L.FF.L.FF.L.FF.LGL.DD.LGL.FF.L.FF.L.FF.L.FF.LL
y129   L....L....L....L....LGL.DD.LGL....L....L....L....LL
y128   ===================================================
         |    |    |    |    |    |    |    |    |    |    |
              -1440     -1430     -1420     -1410     -1400 
```

### Querschnitt durch ein Fenster, x = −1445 (B2)
```
  z →   21 22 23 24 25 26 27 28 29 30 31 32 33 34 35
y139                      _  _
y138                      /  \
y137                   /        \
y136                /              \
y135             /                    \
y134          /                          \
y133    _  ¯                                ¯  _
y132    k     B  =  =  =  =  =  =  =  =  B  \  _  _
y131    o     W                          F
y130          W                          F
y129          W                          W
y128          #  #  #  #  #  #  #  #  #  #
```
Die Konsole (kopfüber gesetzte Kirschtreppe) sitzt nicht in diesem Schnitt, sondern bei x −1446 und −1443 auf y131, z1033.

### Blockliste B2

| Position | Rechnung | Stück |
|---|---|---|
| Hauptdach B: deepslate_tile_stairs / slab / wall | wie B | 550 / 326 / 4 |
| Fenster-Vordächer | 8 × (4 Treppen + 8 Stufen) | 32 Treppen, 64 Stufen |
| Tür-Vordach | 10 Treppen + 20 Stufen | 10 Treppen, 20 Stufen |
| **deepslate_tile_stairs gesamt** | | **592** |
| **deepslate_tile_slab gesamt** | | **410** |
| deepslate_tile_wall | | **4** |
| Ortgang (Lager-Bruchtreppen) | | 20 |
| cherry_stairs (Konsolen) | 8 × 2 + 2 | **18** (5 Rezepte à 6 Bretter → 20 Treppen) |
| cherry_log / cherry_planks / glass_pane | Endgiebel 20 / 32 / 8, dazu 30 Bretter für die Konsolen | **36 Stämme gesamt / 8 Scheiben** |
| Laternen / Ketten | Nord 12 + 12, Süd-Pfeiler 8 + 8, Tür 2 ohne Kette | **22 / 20** |
| Dachboden-Fackeln | | 5 |

**Bruchtiefenschiefer:** 592 + 410 / 2 = 205 + 4 = **801**. Abzüglich 173 bleiben **628 zu farmen**. Mit Sparoption Nordseite (165 Lager-Treppen): **463**.

**Eisen:** 176 + 40 = 216 Nuggets ≈ 24 Barren, plus 20 Barren für die Ketten = 44 Barren → **5 Eisenblöcke**.

---

## Bauhinweise B1 / B2
1. **Blätter freischneiden** (Schere):
   - B1: z1033–1035
   - B2: z1033–1036 (das Vordach ragt bis z1035)
   jeweils y131–137, x −1449…−1425.
2. **Endgiebel bauen**, dann die Nordseite komplett und die Südseite des Hauptdachs.
   - B1: Die Südfläche wird von oben nach unten gebaut. In den Giebelbereichen die Traufstufen weglassen, ebenso bei jedem Fenstergiebel die zwei Hauptdach-Treppen auf z1032/y134 über den Fensterspalten.
3. **B1, Fenstergiebel:**
   1. Zuerst die Front setzen (4 Stämme y133 auf der Wandkrone, 2 Bretter y134). Sie dient als Halt.
   2. Dann die Treppenreihen von der Kehle nach vorn bis z1034.
   3. Die Treppen beim Setzen zur Fenstermitte ausrichten.
   4. Kehlen-Treppen nehmen automatisch Eckformen an. Falls eine falsch aussieht, neu setzen.
4. **B1, Türgiebel:**
   1. Front (30 Felder) wie bei einem Endgiebel aufbauen.
   2. Die 20 Hauptdach-Treppen im Türbereich weglassen.
   3. Die Treppenreihen y134…y138 von der Kehle bis z1034 legen; der First stößt bei z1028 auf den Hauptfirst.
   4. Vor dem Schließen eine Fackel innen setzen.
5. **B2:** Erst das Hauptdach fertig bauen. Die Vordächer kommen danach.
   1. Kirschkonsolen setzen (y131, z1033).
   2. Fliesentreppe z1033/y132 setzen, dabei Richtung Wand blicken und auf die untere Hälfte klicken.
   3. Danach die Stufen z1034/z1035 setzen.
6. **Gerüst:** wie B, die Decke y132 dient als Arbeitsbühne.
   - B2: Die Vordächer setzt man am besten vom Boden aus mit 1–2 Hilfsblöcken (Bambusbretter oder die 44 Tiefenschiefer).

---

## Empfehlung: **B1 (Zwerchgiebel)**
- Das erfüllt den Wunsch „nicht alles durchgängig“ am direktesten. Die Südtraufe wird wirklich aufgelöst, jedes Fenster bekommt ein eigenes kleines Giebeldach, und der große Türgiebel mit Fenster betont den Eingang.
- Der lange Baukörper (51 Blöcke) wird dadurch gegliedert.
- Sie braucht **weniger** Bruchtiefenschiefer als B2 (759 statt 801), weil die Giebel Haupttraufe ersetzen.
- Gegen B2 spricht: Die Vordächer liegen zwangsläufig auf y132, also nur ½ Block unter der 2-tiefen Haupttraufe. Von vorn ergeben sie fast ein durchgehendes zweites Band (siehe Ansicht: `kTTTTkTTTT…`). Das ist genau das Durchgängige, das der Besitzer nicht möchte.
- B2 ist allerdings einfacher zu bauen und lässt sich jederzeit nachrüsten.

## Gesamt-Farm-Liste B1

| Was | Menge |
|---|---|
| **Bruchtiefenschiefer netto** | **586** (759 − 173 Lager), mit ca. 5 % Reserve **~620 abbauen** (≈ 10 Stacks). Unter y 0 ohne Behutsamkeit abbauen |
| mit Sparoption Nord-Rückseite | **421** netto (~445 abbauen) |
| an der Steinsäge | 634 → Fliesentreppen, 121 → 242 Fliesenstufen, 4 → 4 Fliesenmauern |
| Kirsche | 0 farmen (62 Stämme + 16 Stämme für 64 Bretter = 78 von 924) |
| Eisen | 0 farmen (6 Eisenblöcke) |
| Glasscheiben / Fackeln | 12 / 28 + 5 aus Lager |
