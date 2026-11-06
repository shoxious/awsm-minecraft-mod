---
name: minecraft-laufen
description: "Minecraft-Bot: immer Bauplan-Schritt lauf (A*-Wegsuche) statt goto; Geländekarte docs/gelaende.md im Mod-Repo"
metadata:
  type: feedback
---

Für Wege der Spielfigur immer den Bauplan-Schritt `lauf` nehmen, nicht `goto`. `lauf` sucht den Weg selbst (PathWalker.java im awsm-minecraft-mod), öffnet Holztüren, nimmt Treppen und Leitern und meidet Eisentüren. `goto` geht nur für kurze, freie, gerade Strecken.

**Why:** Am 2026-11-04 hat sich der Nutzer beschwert: „du rennst immer gegen wände und türen“. Mit `goto` ist die Figur gegen Wände gesprungen und dabei fast verhungert (Hunger 0, 3,7 Leben).

**How to apply:** Vor Erkundungs- oder Baurunden zuerst `docs/gelaende.md` lesen. Während Automatik `leben`, `hunger` und `nacht` in `/status` beobachten. Siehe [[minecraft-suedtor]], [[minecraft-nachtruhe]], [[minecraft-essen-leiste]].
