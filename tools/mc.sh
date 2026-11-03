#!/bin/sh
# mc.sh METHOD PATH [BODYFILE]
A="$HOME/Library/Application Support/PrismLauncher/instances/26.3/minecraft/config/mcguide-api.json"
T=$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["token"])' "$A")
U=$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["url"])' "$A")
if [ -n "$3" ]; then curl -s -m 20 -X "$1" -H "X-Token: $T" --data-binary @"$3" "$U$2"; else curl -s -m 20 -X "$1" -H "X-Token: $T" "$U$2"; fi
