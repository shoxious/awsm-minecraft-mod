---
name: minecraft-nachtruhe
description: Beim automatischen Bauen in Minecraft nachts immer ins Haus schlafen gehen
metadata:
  node_type: memory
  type: feedback
  originSessionId: 8167e27e-a20b-4076-8a5e-3b48edd75647
  modified: 2026-11-03T08:58:39.013Z
---

Wenn es im Spiel dunkel wird, muss die Spielfigur während automatischer Arbeiten (Baupläne über die mcguide-Schnittstelle) immer schlafen gehen – bevorzugt **im Haus** (rosa Betten bei x -1400/-1399, y129, z1024 und z1026), nicht irgendwo draußen.

**Why:** Ausdrücklicher Wunsch des Nutzers (zweimal betont), u. a. wegen Monstern und damit die Nacht übersprungen wird.

**How to apply:** Baupläne außerhalb des Hauses (z. B. Dach) immer mit `nacht`/`morgen`-Weg und `bett` ausstatten (Weg durch die Haustür x -1423/-1422 z1032, Tür per "use" öffnen/schließen). Siehe [[minecraft-haus-offen]], [[minecraft-material-regel]].
