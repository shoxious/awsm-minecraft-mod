#!/usr/bin/env bash
# Kopiert die Claude-Erinnerungen (Regeln für den Minecraft-Bot) in das Claude-Code-Gedächtnis
# dieses Rechners. Claude Code legt das Gedächtnis pro Startordner an; Standard ist der Home-Ordner.
#   ./claude/setup.sh            → Gedächtnis für Sitzungen, die im Home-Ordner starten
#   ./claude/setup.sh /pfad      → für Sitzungen, die in /pfad starten
set -euo pipefail
cd "$(dirname "$0")"
START="${1:-$HOME}"
KEY=$(printf '%s' "$START" | sed 's#[/.]#-#g')
DEST="$HOME/.claude/projects/$KEY/memory"
mkdir -p "$DEST"
for f in memory/minecraft-*.md; do cp -n "$f" "$DEST/" 2>/dev/null || true; done
touch "$DEST/MEMORY.md"
while IFS= read -r line; do grep -qxF "$line" "$DEST/MEMORY.md" || echo "$line" >> "$DEST/MEMORY.md"; done < memory/MEMORY.md
echo "Erinnerungen liegen jetzt in $DEST"
