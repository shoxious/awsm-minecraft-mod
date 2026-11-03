#!/bin/bash
# shot.sh x y z yaw pitch name
cd "$(dirname "$0")"
printf '{"steps":[{"action":"goto","pos":[%s,%s,%s]},{"action":"look","yaw":%s,"pitch":%s}]}' $1 $2 $3 $4 $5 > shot.json
./mc.sh POST /plan shot.json >/dev/null
for i in $(seq 1 30); do sleep 1; s=$(./mc.sh GET /status | python3 -c "import json,sys;print(json.load(sys.stdin)['bauplan'])"); [ "$s" = "aus" ] && break; done
sleep 1
./mc.sh POST /screenshot >/dev/null; sleep 1
f=$(/bin/ls -t "/Users/kohlwegmichael/Library/Application Support/PrismLauncher/instances/26.3/minecraft/screenshots" | head -1)
cp "/Users/kohlwegmichael/Library/Application Support/PrismLauncher/instances/26.3/minecraft/screenshots/$f" "mitte-$6.png"
./mc.sh GET /status | python3 -c "import json,sys;d=json.load(sys.stdin);print(d['pos'],d['yaw'])"
