#!/usr/bin/env python3
"""TNT-Holzfarm (Schematic TheySix 502), Material vorbereiten: glatter Stein brennen und sägen, Holzteile craften,
Redstone-Teile aus der Redstone-Kiste holen."""
import json
P = "minecraft:"
WB = [-1436, 129, 1031]
pl, st_, ck, cb = P + "spruce_planks", P + "stick", P + "chest", P + "cobblestone"
s = [
    {"action": "lauf", "pos": [-1443, 129, 1027]},
    {"action": "holen", "pos": [-1446, 129, 1027], "item": P + "coal", "count": 8},
    {"action": "holen", "pos": [-1446, 130, 1025], "item": P + "hopper", "count": 5},
    {"action": "holen", "pos": [-1446, 130, 1025], "item": P + "dropper", "count": 1},
    {"action": "holen", "pos": [-1446, 130, 1025], "item": P + "observer", "count": 3},
    {"action": "holen", "pos": [-1446, 130, 1025], "item": P + "piston", "count": 3},
    {"action": "holen", "pos": [-1446, 130, 1025], "item": P + "redstone", "count": 8},
    {"action": "holen", "pos": [-1446, 130, 1025], "item": P + "spruce_pressure_plate", "count": 2},
    {"action": "ablegen", "pos": [-1446, 130, 1029], "item": P + "stone_bricks", "grid": []},
    {"action": "ablegen", "pos": [-1439, 129, 1024], "item": P + "spruce_sapling", "grid": []},
    {"action": "lauf", "pos": [-1435, 129, 1029]},
    {"action": "ofen", "pos": [-1435, 129, 1031], "item": P + "stone", "count": 47, "fuel": P + "coal"},
    {"action": "lauf", "pos": [-1433, 129, 1029]},
    {"action": "saege", "pos": [-1433, 129, 1031], "item": P + "smooth_stone", "grid": [P + "smooth_stone_slab"], "count": 36},
    {"action": "lauf", "pos": [-1436, 129, 1029]},
    {"action": "craft", "pos": WB, "item": pl, "grid": [P + "spruce_log"] + [""] * 8, "count": 26},
    {"action": "craft", "pos": WB, "item": ck, "grid": [pl, pl, pl, pl, "", pl, pl, pl, pl], "count": 10},
    {"action": "craft", "pos": WB, "item": P + "spruce_trapdoor", "grid": [pl, pl, pl, pl, pl, pl, "", "", ""], "count": 3},
    {"action": "craft", "pos": WB, "item": P + "spruce_fence", "grid": [pl, st_, pl, pl, st_, pl, "", "", ""], "count": 1},
    {"action": "craft", "pos": WB, "item": P + "lever", "grid": ["", st_, "", "", cb, "", "", "", ""], "count": 3},
    {"action": "craft", "pos": WB, "item": P + "cobblestone_stairs", "grid": [cb, "", "", cb, cb, "", cb, cb, cb], "count": 1},
    {"action": "lauf", "pos": [-1443, 129, 1027]},
    {"action": "ablegen", "pos": [-1446, 130, 1031], "item": P + "spruce_log", "grid": []},
]
print(json.dumps({"steps": s}))
