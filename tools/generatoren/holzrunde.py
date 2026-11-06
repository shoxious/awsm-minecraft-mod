#!/usr/bin/env python3
"""Holzrunde für die Fichtenfarm auf der Kuppe östlich vom Haus (siehe docs/gelaende.md).

Erzeugt einen Bauplan (JSON auf stdout):
  1. Im Lager Knochenmehl craften, Erde und Setzlinge sicherstellen
  2. Zur Farm (Gang hinter der Hobbit-Tür x -1395, z 1025)
  3. Leere Plätze bepflanzen, alle Setzlinge mit Knochenmehl wachsen lassen
  4. Alle 16 Bäume fällen mit einer unverzauberten Diamant-Axt (Netherit/Verzaubertes blockiert den Plan)
  5. Dreimal über das Feld laufen und Setzlinge/Stöcke einsammeln, neu pflanzen
  6. Fichtenstämme in die Kiste "Holstämme" legen
Abends geht die Figur selbst heim, verschließt den Gang mit 2 Bruchsteinen und öffnet ihn morgens wieder.
Bei Monstern in Sichtweite zieht sie sich ins Haus zurück.

Aufruf:  python3 -I tools/generatoren/holzrunde.py > /tmp/holz.json && tools/mc.sh POST /plan /tmp/holz.json
"""
import json

P = "minecraft:"
TREES = [(x, z) for z in (1017, 1020, 1024, 1027) for x in (-1387, -1383, -1379, -1376)]
BED = [-1400, 129, 1027]
SEAL_NACHT = [{"action": "lauf", "pos": [-1396, 119, 1025]},
              {"action": "place", "item": P + "cobblestone", "pos": [-1394, 119, 1025], "click": [-1394, 118, 1025], "face": "up"},
              {"action": "place", "item": P + "cobblestone", "pos": [-1394, 120, 1025], "click": [-1394, 119, 1025], "face": "up"},
              {"action": "lauf", "pos": [-1401, 129, 1027]}]
SEAL_MORGEN = [{"action": "lauf", "pos": [-1396, 119, 1025]},
               {"action": "break", "pos": [-1394, 120, 1025]}, {"action": "break", "pos": [-1394, 119, 1025]}]


def stand(x, z):
    return [x - 1, 126, z]


def sweep():
    out = []
    for i, z in enumerate((1016, 1019, 1022, 1025, 1028)):
        xs = (-1388, -1385, -1382, -1379, -1376, -1374)
        for x in (xs if i % 2 == 0 else xs[::-1]):
            out.append({"action": "lauf", "pos": [x, 126, z]})
    return out


def laub(x, z):
    return {"action": "laub", "pos": [x, 126, z], "count": 1}


def clear(x, z):
    # Reste vom Erdtürmchen auf dem Pflanzplatz (nur Erde/Gras auf Höhe 126/127, nie den Boden)
    return {"action": "baum", "pos": [x, 126, z]}


def plant(x, z):
    return {"action": "place", "item": P + "spruce_sapling", "pos": [x, 126, z], "click": [x, 125, z], "face": "up"}


st = [
    # Lager: Bruchstein für die Gangsperre, Erde fürs Türmchen, Knochenmehl
    {"action": "lauf", "pos": [-1443, 129, 1027]},
    {"action": "holen", "pos": [-1446, 130, 1029], "item": P + "cobblestone", "count": 4},
    {"action": "holen", "pos": [-1441, 129, 1024], "item": P + "cooked_beef", "count": 32},
    {"action": "holen", "pos": [-1441, 130, 1024], "item": P + "dirt", "count": 32},
    {"action": "holen", "pos": [-1439, 129, 1024], "item": P + "spruce_sapling", "count": 16},
    {"action": "holen", "pos": [-1443, 129, 1031], "item": P + "bone_block", "count": 16},
    {"action": "lauf", "pos": [-1435, 129, 1029]},
    {"action": "craft", "pos": [-1436, 129, 1031], "item": P + "bone_meal", "grid": [P + "bone_block"] + [""] * 8, "count": 16},
]
# Knochenmehl aus der Knochenmehlfarm (untere Halle) mitnehmen
st += [{"action": "lauf", "pos": [-1412, 119, 1035]},
       {"action": "holen", "pos": [-1413, 119, 1033], "item": P + "bone_meal", "count": 64}]
for (x, z) in TREES:
    st += [{"action": "lauf", "pos": stand(x, z)}, laub(x - 1, z), laub(x, z), clear(x, z), plant(x, z), {"action": "wachsen", "pos": [x, 126, z]}]
for (x, z) in TREES:
    st += [{"action": "lauf", "pos": stand(x, z)}, laub(x - 1, z), {"action": "baum", "pos": [x, 126, z]}]
st += sweep() + sweep() + sweep()
for (x, z) in TREES:
    st += [{"action": "lauf", "pos": stand(x, z)}, laub(x, z), clear(x, z), plant(x, z)]
st += [{"action": "lauf", "pos": [-1443, 130, 1029]},
       {"action": "ablegen", "pos": [-1446, 130, 1031], "item": P + "spruce_log", "grid": []},
       {"action": "ablegen", "pos": [-1439, 129, 1024], "item": P + "spruce_sapling", "grid": []},
       {"action": "ablegen", "pos": [-1446, 129, 1031], "item": P + "stick", "grid": []},
       {"action": "lauf", "pos": [-1442, 129, 1028]},
       {"action": "inhalt"}]

print(json.dumps({"steps": st, "nacht": SEAL_NACHT, "morgen": SEAL_MORGEN, "bett": BED}))
