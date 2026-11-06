---
name: minecraft-essen-leiste
description: "Minecraft-Inventar: immer 1 Stapel Essen + 1 Stapel Fackeln in der Schnellleiste und Pfeile im Inventar – Sortieren darf sie nicht wegräumen"
metadata:
  type: feedback
---

In der Schnellleiste der Spielfigur liegen immer **1 Stapel Essen** (z. B. gebratenes Hühnchen) und **1 Stapel Fackeln**. Im Inventar sind immer **mindestens ein paar Pfeile** für den Bogen. Werkzeuge bleiben auf den Plätzen 0–4.

**Why:** Am 2026-11-03 hat ein `sortieren` das Essen weggeräumt, und die Figur wäre beim automatischen Bauen fast verhungert. Danach hat der Nutzer ausdrücklich verlangt: „auf meiner Leiste sollen auch immer 1 Stack Fackeln sein“ und „mindestens ein Pfeil im Inventar“.

**How to apply:** Nach jedem `sortieren` und vor langen Bauplänen nachprüfen. Fehlt etwas, mit `holen` aus den Kisten nachholen: Essen aus „Essen“ (-1441,129,1024), Fackeln und Pfeile aus „Werkzeug & Kampf“ (-1445,129,1024). In die Leiste kommt es per `use`-Schritt, bei Fackeln mit Klick auf eine Deckenunterseite, damit nichts gesetzt wird. Siehe [[minecraft-material-regel]], [[minecraft-nachtruhe]].
