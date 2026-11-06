#!/usr/bin/env python3
"""TNT-Holzfarm, Abschnitt A: Holzfarm-Feld nach Osten erweitern (siehe docs/gelaende.md).

- alte Ostmauer x -1373 abbauen (Steinziegel einsammeln)
- Hügel (y126) im neuen Teil abtragen, Mulden Schicht für Schicht mit Erde auf y125 auffüllen
- neue Mauer: Nord z1014 und Süd z1030 (x -1372..-1365), Ost x -1365; Fundament und Mauer bis y128 aus Steinziegeln
Aufruf: python3 -I tools/generatoren/tntfarm_a.py scan.txt > plan.json   (scan = /bereich -1376,115,1013 .. -1363,132,1031)
"""
import json, sys, math

P = "minecraft:"
SOFT = {'short_grass', 'tall_grass', 'fern', 'bush', 'dandelion', 'poppy', 'torch', 'air'}
g = {}
for l in open(sys.argv[1]):
    x, y, z, b = l.split()[:4]
    g[(int(x), int(y), int(z))] = b.split('[')[0]


def top(x, z):
    for y in range(132, 114, -1):
        b = g.get((x, y, z))
        if b and b not in SOFT:
            return y
    return 114


INTERIOR = [(x, z) for x in range(-1373, -1365) for z in range(1015, 1030)]
WALL = [(x, 1014) for x in range(-1372, -1364)] + [(x, 1030) for x in range(-1372, -1364)] + [(-1365, z) for z in range(1015, 1030)]
FIELD = [(x, z) for x in range(-1376, -1373) for z in range(1015, 1030)]
h = {c: top(*c) for c in INTERIOR + WALL + FIELD}
for z in range(1015, 1030):
    h[(-1373, z)] = 125  # nach dem Abbau der alten Mauer

st = [{"action": "lauf", "pos": [-1443, 129, 1027]},
      {"action": "ablegen", "pos": [-1446, 130, 1031], "item": P + "spruce_log", "grid": []},
      {"action": "holen", "pos": [-1446, 129, 1027], "item": P + "diamond", "count": 3},
      {"action": "holen", "pos": [-1441, 130, 1024], "item": P + "dirt", "count": 64},
      {"action": "holen", "pos": [-1445, 129, 1024], "item": P + "iron_shovel", "count": 1},
      {"action": "lauf", "pos": [-1436, 129, 1029]},
      {"action": "craft", "pos": [-1436, 129, 1031], "item": P + "diamond_pickaxe",
       "grid": [P + "diamond", P + "diamond", P + "diamond", "", P + "stick", "", "", P + "stick", ""], "count": 1},
      # Hof: Steinziegel und Stein (Reserve) holen
      {"action": "lauf", "pos": [-1416, 119, 1053]},
      {"action": "holen", "pos": [-1417, 119, 1050], "item": P + "stone_bricks", "count": 64},
      {"action": "lauf", "pos": [-1414, 119, 1053]},
      {"action": "holen", "pos": [-1414, 119, 1051], "item": P + "stone_bricks", "count": 31},
      {"action": "holen", "pos": [-1414, 119, 1051], "item": P + "stone", "count": 16},
      {"action": "lauf", "pos": [-1424, 119, 1052]},
      {"action": "craft", "pos": [-1425, 119, 1050], "item": P + "stone_bricks",
       "grid": [P + "stone", P + "stone", "", P + "stone", P + "stone", "", "", "", ""], "count": 4},
      ]

stand_now = None


def go(stand):
    global stand_now
    if stand != stand_now:
        st.append({"action": "lauf", "pos": [stand[0], h[stand] + 1, stand[1]]})
        stand_now = stand


def pick_stand(tx, ty, tz, avoid=()):
    best = None
    for c in INTERIOR + FIELD:
        if c == (tx, tz) or c in avoid:
            continue
        fy = h[c] + 1
        eye = (c[0] + 0.5, fy + 1.62, c[1] + 0.5)
        d = math.dist(eye, (tx + 0.5, ty + 0.5, tz + 0.5))
        if d > 4.2 or fy > ty + 3 or fy < ty - 3:
            continue
        cost = d + (0 if c == stand_now else 1.5)
        if best is None or cost < best[0]:
            best = (cost, c)
    if best is None:
        raise SystemExit(f"kein Standplatz für {tx},{ty},{tz}")
    return best[1]


# 1) alte Ostmauer abbauen
for z in range(1015, 1030):
    go((-1374, z))
    for y in (128, 127, 126):
        st.append({"action": "break", "pos": [-1373, y, z]})
for z in range(1029, 1014, -2):
    st.append({"action": "lauf", "pos": [-1373, 126, z]})
stand_now = None

# 2) Hügel abtragen
for c in INTERIOR:
    if h[c] >= 126:
        for y in range(h[c], 125, -1):
            s = pick_stand(c[0], y, c[1])
            go(s)
            st.append({"action": "break", "pos": [c[0], y, c[1]]})
        h[c] = 125

# 3) auffüllen bis y125, Schicht für Schicht von unten
cells = INTERIOR + WALL
lo = min(h[c] for c in cells)
for L in range(lo + 1, 126):
    todo = sorted([c for c in cells if h[c] == L - 1], key=lambda c: (c[1], c[0]))
    for c in todo:
        s = pick_stand(c[0], L, c[1])
        go(s)
        item = "stone_bricks" if c in WALL else "dirt"
        st.append({"action": "place", "item": P + item, "pos": [c[0], L, c[1]], "click": [c[0], L - 1, c[1]], "face": "up"})
        h[c] = L

# 4) Mauer y126..128
for L in (126, 127, 128):
    for c in WALL:
        if h[c] == L - 1:
            s = pick_stand(c[0], L, c[1])
            go(s)
            st.append({"action": "place", "item": P + "stone_bricks", "pos": [c[0], L, c[1]], "click": [c[0], L - 1, c[1]], "face": "up"})
            h[c] = L

st += [{"action": "lauf", "pos": [-1370, 126, 1022]}, {"action": "lauf", "pos": [-1384, 126, 1024]}]
SEAL_NACHT = [{"action": "lauf", "pos": [-1396, 119, 1025]},
              {"action": "place", "item": P + "cobblestone", "pos": [-1394, 119, 1025], "click": [-1394, 118, 1025], "face": "up"},
              {"action": "place", "item": P + "cobblestone", "pos": [-1394, 120, 1025], "click": [-1394, 119, 1025], "face": "up"},
              {"action": "lauf", "pos": [-1401, 129, 1027]}]
SEAL_MORGEN = [{"action": "lauf", "pos": [-1396, 119, 1025]},
               {"action": "break", "pos": [-1394, 120, 1025]}, {"action": "break", "pos": [-1394, 119, 1025]}]
print(json.dumps({"steps": st, "nacht": SEAL_NACHT, "morgen": SEAL_MORGEN, "bett": [-1400, 129, 1027]}))
print(f"{len(st)} Schritte", file=sys.stderr)
