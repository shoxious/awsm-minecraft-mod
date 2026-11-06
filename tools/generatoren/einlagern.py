#!/usr/bin/env python3
"""Inventar gezielt in die beschrifteten Hauskisten legen (die Schilder-Sortierung erkennt manche Schilder nicht).
Werkzeug, Essen, Fackeln, Pfeile bleiben im Inventar.  Aufruf: python3 -I einlagern.py [extra_vorher.json] > plan.json"""
import json, sys
P = "minecraft:"
KISTEN = {
    (-1446, 130, 1025): "piston observer dropper hopper redstone tnt lever spruce_pressure_plate comparator repeater redstone_torch sticky_piston slime_block",
    (-1446, 130, 1029): "cobblestone stone",
    (-1446, 129, 1029): "smooth_stone smooth_stone_slab cobblestone_stairs stone_bricks",
    (-1446, 129, 1031): "spruce_planks stick ladder spruce_fence spruce_trapdoor",
    (-1446, 130, 1031): "spruce_log",
    (-1439, 129, 1024): "bone_meal cherry_leaves spruce_sapling spruce_leaves cherry_sapling",
    (-1441, 130, 1024): "dirt grass_block podzol sand gravel",
    (-1445, 129, 1024): "bucket water_bucket shears",
    (-1446, 130, 1027): "iron_ingot",
    (-1446, 129, 1027): "coal diamond",
    (-1444, 129, 1031): "chest",
}
st = [{"action": "lauf", "pos": [-1443, 129, 1027]}]
for pos, items in KISTEN.items():
    for i in items.split():
        st.append({"action": "ablegen", "pos": list(pos), "item": P + i, "grid": []})
st.append({"action": "inhalt"})
pre = json.load(open(sys.argv[1]))["steps"] if len(sys.argv) > 1 else []
print(json.dumps({"steps": pre + st}))
