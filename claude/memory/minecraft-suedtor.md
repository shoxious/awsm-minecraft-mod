---
name: minecraft-suedtor
description: "Minecraft-Burg: das Südtor (Eisentüren -1423/-1422,119,1066) NIE durchgehen/verlassen; Türen anklicken oder Knopf"
metadata:
  node_type: memory
  type: feedback
  originSessionId: 8167e27e-a20b-4076-8a5e-3b48edd75647
  modified: 2026-11-04T07:19:39.885Z
---

Durch das **Südtor** der Burg (Eisen-Doppeltür bei x -1423/-1422, y119, z1066, Knöpfe innen z1065 und außen z1067) darf die Spielfigur **nie** hinausgehen.

**Why:** Der Nutzer hat am 2026-11-04 beim Erkunden ausdrücklich geschrieben: „das südtür darfst du nie verlassen!!!“. Draußen ist ungesichertes Gelände.

**How to apply:** Pläne enden immer innerhalb der Burgmauer, und kein goto führt südlich über z1065 hinaus. Für Türen gilt: Holztüren per `use` anklicken, Eisentüren haben Knöpfe. Siehe [[minecraft-material-regel]] (nie zur Haustür hinaus, gilt sinngemäß weiter) und [[minecraft-nachtruhe]].
