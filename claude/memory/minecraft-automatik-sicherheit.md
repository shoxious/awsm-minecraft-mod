---
name: minecraft-automatik-sicherheit
description: "Minecraft-Bot: neue Abläufe erst beaufsichtigt testen; bei Automatik nur Nötiges mitnehmen, Wertvolles in der Kiste lassen"
metadata:
  node_type: memory
  type: feedback
  originSessionId: 8167e27e-a20b-4076-8a5e-3b48edd75647
  modified: 2026-11-04T09:52:07.550Z
---

Vor jeder unbeaufsichtigten Schleife (z. B. Holzrunden) laufen neue oder geänderte Bauplan-Schritte erst einmal, während ich zuschaue. Bei Automatik trägt die Figur nur, was sie für die Aufgabe braucht. Verzauberte und wertvolle Sachen bleiben im Lager.

**Why:** Am 2026-11-04 ist die Figur zweimal gestorben: einmal durch einen Zombie beim Feldbau, einmal erstickt, weil ein Setzling unter ihren Füßen gedüngt wurde. Dabei gingen die verzauberte Netherit-Spitzhacke und -Schaufel verloren. Der Nutzer fragte: „warum hast du das zugelassen“. Er möchte erstmal ohne neue Spitzhacke und Schaufel weitermachen.

Nachtrag: Der Nutzer hat sich neue Netherit-Spitzhacke und -Schaufel besorgt und sagt: „das darf nicht wieder passieren“. Die Mod blockiert deshalb seit dem 2026-11-04 automatische Pläne, sobald Netherit oder Verzaubertes im Inventar ist, auch Rüstung. Für die Holzfarm nimmt die Figur eine unverzauberte Diamant-Axt.

**How to apply:** Vor dem Start einer Schleife: Inventar ausdünnen (`ablegen` mit `item`) und den Ablauf einmal mitverfolgen. Nach einem Tod nicht einfach weiterlaufen lassen, sondern stoppen und die Ursache klären. Siehe [[minecraft-laufen]], [[minecraft-suedtor]], [[minecraft-nachtruhe]].
