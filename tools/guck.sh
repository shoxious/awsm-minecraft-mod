#!/bin/bash
# guck.sh x y z name  -> Trichter oeffnen, Screenshot, schliessen
cd "$(dirname "$0")"
printf '{"steps":[{"action":"use","item":"minecraft:netherite_sword","click":[%s,%s,%s],"face":"south"}]}' $1 $2 $3 > g.json
./mc.sh POST /plan g.json >/dev/null; sleep 2
./mc.sh POST /screenshot >/dev/null; sleep 1
f=$(/bin/ls -t "/Users/kohlwegmichael/Library/Application Support/PrismLauncher/instances/26.3/minecraft/screenshots" | head -1)
cp "/Users/kohlwegmichael/Library/Application Support/PrismLauncher/instances/26.3/minecraft/screenshots/$f" "trichter-$4.png"
./mc.sh GET /status | python3 -c "import json,sys;print(json.load(sys.stdin).get('fenster'))"
printf '{"steps":[{"action":"holen","pos":[%s,%s,%s],"item":"minecraft:barrier","count":1}]}' $1 $2 $3 > g.json
./mc.sh POST /plan g.json >/dev/null; sleep 3
./mc.sh GET /status | python3 -c "import json,sys;print('danach',json.load(sys.stdin).get('fenster'))"
