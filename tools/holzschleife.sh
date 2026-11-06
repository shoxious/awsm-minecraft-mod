#!/bin/bash
# Holzrunden am Stück. Hält an (Exit-Code + Grund im Log), wenn etwas nicht stimmt:
#   2 Figur gestorben (wird wiederbelebt) · 3 Plan gestoppt · 4 Spiel weg · 5 Holzlager fast voll · 6 Axt fast hin
cd "$(dirname "$0")/.."
LOG=${LOG:-/tmp/holzschleife.log}
PLAN=$(mktemp -t holz).json
st() { sh tools/mc.sh GET /status 2>/dev/null; }
feld() { echo "$1" | python3 -I -c "import json,sys;d=json.load(sys.stdin);print(d.get('$2'))" 2>/dev/null; }
runde=0
while true; do
  runde=$((runde+1))
  python3 -I tools/generatoren/holzrunde.py > "$PLAN"
  sh tools/mc.sh POST /plan "$PLAN" >/dev/null
  echo "$(date +%T) Runde $runde gestartet" >> "$LOG"
  sleep 5
  for i in $(seq 1 360); do
    s=$(st)
    [ -z "$s" ] && { echo "$(date +%T) Spiel nicht erreichbar" >> "$LOG"; exit 4; }
    if [ "$(feld "$s" leben)" = "0.0" ]; then
      sleep 2; sh tools/mc.sh POST /weiter >/dev/null
      echo "$(date +%T) FIGUR GESTORBEN bei $(feld "$s" pos) – wiederbelebt" >> "$LOG"; exit 2
    fi
    [ "$(feld "$s" bauplan)" = "aus" ] && break
    sleep 10
  done
  echo "$s" | python3 -I -c 'import json,sys;d=json.load(sys.stdin);[print("   ",m) for m in d["meldungen"][-6:]]' >> "$LOG"
  if echo "$s" | grep -q "Bauplan gestoppt"; then echo "$(date +%T) Plan gestoppt" >> "$LOG"; exit 3; fi
  inh=$(sh tools/mc.sh GET /inhalt)
  belegt=$(echo "$inh" | grep "Holstämme" | sed -E 's/.*belegt ([0-9]+).*/\1/')
  logs=$(echo "$inh" | grep "Holstämme" | grep -oE "spruce_log=[0-9]+")
  axt=$(echo "$s" | grep -oE "netherite_axe x1 \(([0-9]+)" | grep -oE "[0-9]+$")
  echo "$(date +%T) Runde $runde fertig: Holstämme belegt ${belegt:-?}/54, $logs, Axt ${axt:-?}" >> "$LOG"
  [ -n "$belegt" ] && [ "$belegt" -ge 46 ] && { echo "$(date +%T) Holzlager fast voll" >> "$LOG"; exit 5; }
  [ -n "$axt" ] && [ "$axt" -lt 400 ] && { echo "$(date +%T) Axt unter 400" >> "$LOG"; exit 6; }
  [ -z "$axt" ] && { echo "$(date +%T) Axt nicht im Inventar" >> "$LOG"; exit 6; }
done
