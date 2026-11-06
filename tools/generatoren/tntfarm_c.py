#!/usr/bin/env python3
"""TNT-Holzfarm nach Schematic TheySix Nr. 502 ("TLM TF V1") auf dem erweiterten Holzfarm-Feld.

Die Schematic ist um 90° gegen den Uhrzeigersinn gedreht:  welt_x = -1386 + lz,  welt_z = 1030 - lx,  welt_y = 122 + ly
(lokal y3 = Grasboden = Feldboden y125).

Aufruf:
  python3 -I tools/generatoren/tntfarm_c.py SCHEM_GEDREHT.json SCAN.txt PHASE > plan.json
    PHASE = boden  : alles bis y128 (Graben, Kisten, Trichter, Rinne, Spender ...), ohne Wasser/TNT
    PHASE = oben   : TNT-Teil y129..135 mit Hilfssäulen, ohne TNT
    PHASE = liste  : nur ausgeben, was zu tun ist
Wasser, TNT und Hebel kommen in einem eigenen Schritt (mit dem Nutzer).
"""
import json, math, sys

P = "minecraft:"
schem_file, scan_file, phase = sys.argv[1], sys.argv[2], sys.argv[3]
X0, X1, Z0, Z1, Y0, Y1 = -1386, -1366, 1015, 1029, 123, 135

GROUND = {'grass_block', 'dirt', 'podzol', 'stone', 'coal_ore'}
TERRAIN = {'stone', 'dirt', 'grass_block', 'coal_ore', 'podzol', 'iron_ore', 'gravel', 'andesite', 'diorite', 'granite', 'tuff'}
IGNORE = {'short_grass', 'tall_grass', 'oak_log', 'piston_head'}
ITEM = {'oak_trapdoor': 'spruce_trapdoor', 'oak_pressure_plate': 'spruce_pressure_plate', 'oak_fence': 'spruce_fence',
        'redstone_wire': 'redstone', 'jungle_leaves': 'cherry_leaves', 'dirt_path': 'dirt'}
BLOCKNAME = {'oak_trapdoor': 'spruce_trapdoor', 'oak_pressure_plate': 'spruce_pressure_plate', 'oak_fence': 'spruce_fence',
             'jungle_leaves': 'cherry_leaves'}
INTERACTIVE = {'chest', 'hopper', 'dropper', 'spruce_trapdoor', 'lever', 'crafting_table', 'furnace'}
NOT_CLICKABLE = {'air', 'water', 'redstone_wire', 'lever', 'spruce_pressure_plate', 'spruce_trapdoor', 'short_grass', 'torch'}
DIRS = {'up': (0, 1, 0), 'down': (0, -1, 0), 'north': (0, 0, -1), 'south': (0, 0, 1), 'west': (-1, 0, 0), 'east': (1, 0, 0)}
OPP = {'up': 'down', 'down': 'up', 'north': 'south', 'south': 'north', 'west': 'east', 'east': 'west'}
YAW = {'south': 0.0, 'west': 90.0, 'north': 180.0, 'east': -90.0}


def add(p, d):
    return (p[0] + d[0], p[1] + d[1], p[2] + d[2])


# ---- Ist-Zustand
W = {}
for l in open(scan_file):
    x, y, z, b = l.split()[:4]
    W[(int(x), int(y), int(z))] = b.split('[')[0]


def cur(p):
    return W.get(p, 'air')


def solid(p):
    return cur(p) not in NOT_CLICKABLE


def free(p):  # Figur kann hier sein
    return cur(p) in ('air', 'short_grass', 'torch', 'water') or cur(p) in ('spruce_pressure_plate', 'redstone_wire')


# ---- Soll-Zustand
S = {}
for x, y, z, n, props, loc in json.load(open(schem_file)):
    S[(x, y, z)] = (n, props)
T = {}
if phase in ('abriss', 'fuellen'):
    Y1 = 140
for x in range(X0, X1 + 1):
    for z in range(Z0, Z1 + 1):
        for y in range(Y0, Y1 + 1):
            n, props = S.get((x, y, z), ('air', {}))
            if n in IGNORE:
                n, props = 'air', {}
            if phase in ('abriss', 'fuellen'):
                T[(x, y, z)] = ('SOLID', {}) if y <= 125 else (('torch', {}) if W.get((x, y, z)) == 'torch' else ('air', {}))
                continue
            if y <= 125 and n in TERRAIN:
                T[(x, y, z)] = ('SOLID', {})
            elif n in TERRAIN:
                T[(x, y, z)] = ('dirt', {})
            else:
                T[(x, y, z)] = (n, props)


def target_name(n):
    return BLOCKNAME.get(n, n)


def matches(p):
    n, props = T[p]
    c = cur(p)
    if n == 'SOLID':
        return c not in ('air', 'water', 'short_grass', 'torch')
    if n in ('air', 'water'):
        return c in ('air', 'short_grass') or (n == 'water' and c == 'water')
    if n == 'dirt_path':
        return c == 'dirt_path'
    return c == target_name(n)


# ---- Plan
TEMPSET = set()
steps = []
stand_now = None
log = []
temps = []


ELEV = [(-1386, 133, 1021), (-1380, 133, 1021), (-1386, 133, 1024), (-1380, 133, 1024), (-1383, 133, 1017)] if phase in ('oben', 'abriss') else []


def pillar(st):
    return [(st[0], y, st[2]) for y in range(126, st[1])]


def go(stand):
    global stand_now
    if stand == stand_now:
        return
    if stand_now in ELEV:
        steps.append({"action": "abtragen", "pos": [stand_now[0], 126, stand_now[2]]})
        for c in pillar(stand_now):
            W[c] = 'air'
            TEMPSET.discard(c)
    if stand in ELEV:
        steps.append({"action": "lauf", "pos": [stand[0], 126, stand[2]]})
        steps.append({"action": "saeule", "pos": list(stand), "item": P + "dirt"})
        for c in pillar(stand):
            W[c] = 'dirt'
            TEMPSET.add(c)
    else:
        steps.append({"action": "lauf", "pos": list(stand)})
    stand_now = stand


def stands():
    """mögliche Standplätze (Fuesse) am Boden bzw. in Gruben."""
    out = []
    for x in range(-1389, -1365):
        for z in range(1015, 1030):
            for y in (125, 126):  # nur am Boden (Gruben/Aufbauten sind oft nicht erreichbar)
                f = (x, y, z)
                if free(f) and free(add(f, (0, 1, 0))) and cur(add(f, (0, -1, 0))) in GROUND and add(f, (0, -1, 0)) not in TEMPSET:
                    out.append(f)
    return out


def eye(f):
    return (f[0] + 0.5, f[1] + 1.62, f[2] + 0.5)


def elev_ok(f):
    if f == stand_now:
        return True
    return all(cur(c) in ('air', 'short_grass') for c in pillar(f)) and free(f) and free(add(f, (0, 1, 0)))


def pick_stand(point, avoid, maxd=4.3, extra=()):
    best = None
    for f in list(stands()) + [e for e in ELEV if elev_ok(e)] + list(extra):
        if f in avoid or add(f, (0, 1, 0)) in avoid:
            continue
        d = math.dist(eye(f), point)
        if d > maxd:
            continue
        cost = d + (0 if f == stand_now else (8.0 if f in ELEV else 2.0))
        if best is None or cost < best[0]:
            best = (cost, f)
    return best[1] if best else None


# 1) Abbauen: von oben nach unten
todo = [p for p in T if not matches(p) and cur(p) not in ('air', 'short_grass', 'water', 'torch')]
if phase == 'fuellen':
    todo = []
if phase == 'oben':
    todo = [p for p in todo if p[1] >= 129]
elif phase == 'boden':
    todo = [p for p in todo if p[1] <= 128]
todo.sort(key=lambda p: -p[1])
while todo:
    progress = False
    for p in list(todo):
        f = pick_stand((p[0] + .5, p[1] + .5, p[2] + .5), avoid={p})
        if f is None:
            continue
        if f[1] - 1 == p[1] and f[0] == p[0] and f[2] == p[2]:
            continue
        go(f)
        steps.append({"action": "break", "pos": list(p)})
        W[p] = 'air'
        todo.remove(p)
        progress = True
        break
    if not progress:
        log.append(f"nicht erreichbar zum Abbauen: {todo}")
        break


# 2) Setzen
def options(p, n, props):
    """(click, face, hit, yaw, pitch, sneak) Möglichkeiten für Block n an p."""
    res = []
    fac = props.get('facing')
    for dname, d in DIRS.items():
        nb = (p[0] - d[0], p[1] - d[1], p[2] - d[2])  # Nachbar, auf dessen Fläche dname wir klicken
        if not solid(nb):
            continue
        cn = cur(nb)
        hit = None
        yaw = pitch = None
        sneak = True
        if n == 'chest':
            if cn in INTERACTIVE:
                continue
            sneak = False
            yaw = YAW[OPP[fac]]
        elif n == 'smooth_stone_slab':
            half = props.get('type')
            if dname == 'up' and half != 'bottom':
                continue
            if dname == 'down' and half != 'top':
                continue
            if dname not in ('up', 'down'):
                hit = 0.25 if half == 'bottom' else 0.75
        elif n == 'cobblestone_stairs':
            if dname == 'down':
                continue
            if dname != 'up':
                hit = 0.25
            yaw = YAW[fac]
        elif n == 'oak_trapdoor':
            half = props.get('half')
            if dname in ('up', 'down'):
                if (dname == 'up') != (half == 'bottom'):
                    continue
                yaw = YAW[OPP[fac]]
            else:
                if dname != fac:
                    continue
                hit = 0.25 if half == 'bottom' else 0.75
        elif n == 'hopper':
            want = OPP[fac] if fac != 'down' else 'up'
            if dname != want:
                continue
        elif n == 'lever':
            if props.get('face') == 'floor':
                if dname != 'up':
                    continue
                yaw = YAW[fac]
            elif props.get('face') == 'wall':
                if dname != fac:
                    continue
            if nb in TEMPSET:
                continue
        elif n in ('redstone_wire', 'oak_pressure_plate'):
            if dname != 'up' or nb in TEMPSET:
                continue
        elif n == 'observer':
            yaw = YAW.get(fac, 0.0)
            pitch = 90.0 if fac == 'down' else (-90.0 if fac == 'up' else 0.0)
        elif n in ('piston', 'dropper'):
            o = OPP[fac]
            yaw = YAW.get(o, 0.0)
            pitch = 90.0 if o == 'down' else (-90.0 if o == 'up' else 0.0)
        res.append((nb, dname, hit, yaw, pitch, sneak))
    return res


place_list = [p for p in T if T[p][0] not in ('air', 'water', 'SOLID', 'tnt') and not matches(p)]
place_list += [p for p in T if T[p][0] == 'SOLID' and not matches(p)]
if phase == 'fuellen':
    place_list = [p for p in place_list if p[1] <= 125]
if phase == 'oben':
    place_list = [p for p in place_list if p[1] >= 129]
elif phase == 'boden':
    place_list = [p for p in place_list if p[1] <= 128]
# Hebel zuletzt (bleiben aus); Redstone nach seinen Bloecken
prio = {'lever': 3, 'redstone_wire': 2, 'oak_pressure_plate': 2, 'hopper': 1}
place_list.sort(key=lambda p: (p[1], prio.get(T[p][0], 0)))

placed = []
pending = list(place_list)
guard = 0
while pending and guard < 2000:
    guard += 1
    done_one = False
    for p in sorted(pending, key=lambda q: (q[1], prio.get(T[q][0], 0))):
        n, props = T[p]
        if n == 'SOLID':
            n, props = 'dirt', {}
        opts = options(p, n, props)
        if not opts:
            continue
        best = None
        for (nb, dname, hit, yaw, pitch, sneak) in opts:
            d = DIRS[dname]
            pt = (nb[0] + 0.5 + d[0] * 0.5, nb[1] + (hit if hit is not None else 0.5 + d[1] * 0.5), nb[2] + 0.5 + d[2] * 0.5)
            f = pick_stand(pt, avoid={p})
            if f is None:
                continue
            c = math.dist(eye(f), pt) + (0 if f == stand_now else 2)
            if best is None or c < best[0]:
                best = (c, f, nb, dname, hit, yaw, pitch, sneak)
        if best is None:
            continue
        _, f, nb, dname, hit, yaw, pitch, sneak = best
        go(f)
        item = ITEM.get(n, n)
        stp = {"action": "place", "item": P + item, "pos": list(p), "click": list(nb), "face": dname, "schleichen": sneak}
        if hit is not None:
            d = DIRS[dname]
            stp["hit"] = [0.5 + d[0] * 0.5, hit, 0.5 + d[2] * 0.5]
        if yaw is not None:
            stp["yaw"] = yaw
        if pitch is not None:
            stp["pitch"] = pitch
        steps.append(stp)
        W[p] = target_name(n) if T[p][0] != 'dirt_path' else 'dirt'
        if T[p][0] == 'dirt_path':
            steps.append({"action": "use", "item": P + "iron_shovel", "pos": list(p), "click": list(p), "face": "up"})
            W[p] = 'dirt_path'
        if n == 'oak_trapdoor' and props.get('open') == 'true' and props.get('powered') != 'true':
            steps.append({"action": "use", "pos": list(p), "click": list(p), "face": "up", "hit": [0.5, 0.5, 0.5]})
        pending.remove(p)
        placed.append(p)
        done_one = True
        break
    if not done_one:
        # Hilfsstütze: unter dem tiefsten offenen Block eine Erdsäule bis zum Boden
        made = False
        for p in sorted(pending, key=lambda q: (q[1], prio.get(T[q][0], 0))):
            col = []
            q = (p[0], p[1] - 1, p[2])
            n0, pr0 = T[p]
            if n0 == 'oak_trapdoor' and pr0.get('half') == 'top':
                d0 = DIRS[pr0['facing']]
                q = (p[0] - d0[0], p[1], p[2] - d0[2])
                if solid(q) or T.get(q, ('x',))[0] not in ('air',):
                    continue
            while not solid(q) and T.get(q, ('air',))[0] in ('air', 'water') and q[1] > 120:
                col.append(q)
                q = (q[0], q[1] - 1, q[2])
            if not col or not solid(q):
                continue
            ok = True
            for c in reversed(col):
                f = pick_stand((c[0] + .5, c[1] + .5, c[2] + .5), avoid={c})
                if f is None:
                    ok = False
                    break
                go(f)
                steps.append({"action": "place", "item": P + "dirt", "pos": list(c), "click": [c[0], c[1] - 1, c[2]], "face": "up", "schleichen": True})
                W[c] = 'dirt'
                TEMPSET.add(c)
                temps.append(c)
            if ok:
                made = True
                break
        if not made:
            break
if pending:
    log.append("NICHT GESETZT: " + ", ".join(f"{T[p][0]}@{p}" for p in pending))

for c in sorted(temps, key=lambda q: -q[1]):
    f = pick_stand((c[0] + .5, c[1] + .5, c[2] + .5), avoid={c})
    if f is None:
        log.append(f"Hilfsstütze {c} nicht erreichbar")
        continue
    go(f)
    steps.append({"action": "break", "pos": list(c)})
    W[c] = 'air'
go((-1387, 126, 1023))
P_ = "minecraft:"
SEAL_NACHT = [{"action": "abtragen", "pos": [0, 126, 0]}, {"action": "lauf", "pos": [-1396, 119, 1025]},
              {"action": "place", "item": P_ + "cobblestone", "pos": [-1394, 119, 1025], "click": [-1394, 118, 1025], "face": "up"},
              {"action": "place", "item": P_ + "cobblestone", "pos": [-1394, 120, 1025], "click": [-1394, 119, 1025], "face": "up"},
              {"action": "lauf", "pos": [-1401, 129, 1027]}]
SEAL_MORGEN = [{"action": "lauf", "pos": [-1396, 119, 1025]},
               {"action": "break", "pos": [-1394, 120, 1025]}, {"action": "break", "pos": [-1394, 119, 1025]}]
for l in log:
    print(l, file=sys.stderr)
from collections import Counter
print(Counter(s["action"] for s in steps), file=sys.stderr)
print(Counter(s.get("item", "") for s in steps if s["action"] == "place"), file=sys.stderr)
if phase != 'liste':
    print(json.dumps({"steps": steps, "nacht": SEAL_NACHT, "morgen": SEAL_MORGEN, "bett": [-1400, 129, 1027]}))
