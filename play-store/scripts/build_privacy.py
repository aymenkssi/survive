#!/usr/bin/env python3
"""Génère docs/confidentialite.html (français et anglais) depuis app/src/main/assets/privacy/privacy.json.

La même source est affichée dans l'application (Réglages › Confidentialité) : un test vérifie que la page
en ligne est à jour. Lancer après toute modification de privacy.json :
    python3 play-store/scripts/build_privacy.py
"""
import html
import json
import os

ROOT = os.path.join(os.path.dirname(__file__), "..", "..")
data = json.load(open(os.path.join(ROOT, "app/src/main/assets/privacy/privacy.json"), encoding="utf-8"))


def block(lang, updated, label):
    d = data[lang]
    parts = [f'<section lang="{lang}" id="{lang}">', f"<h1>{html.escape(d['title'])}</h1>",
             f'<p class="updated">{label} {html.escape(updated)}</p>', f"<p>{html.escape(d['intro'])}</p>"]
    for s in d["sections"]:
        parts.append(f"<h2>{html.escape(s['title'])}</h2>")
        parts.append(f"<p>{html.escape(s['body'])}</p>")
    parts.append("</section>")
    return "\n".join(parts)


page = f"""<!doctype html>
<html lang="fr">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Survivor: End of the World – Confidentialité / Privacy</title>
<style>
  :root {{ --bg: #f7f5ee; --fg: #1b1f1a; --accent: #2e5a3a; --muted: #5b6459; }}
  @media (prefers-color-scheme: dark) {{ :root {{ --bg: #1b1f1a; --fg: #e8ece4; --accent: #8fcb9b; --muted: #a8b2a5; }} }}
  body {{ margin: 0; background: var(--bg); color: var(--fg); font: 16px/1.6 system-ui, sans-serif; }}
  main {{ max-width: 760px; margin: 0 auto; padding: 24px 16px 48px; }}
  nav a {{ color: var(--accent); margin-right: 16px; }}
  h1 {{ color: var(--accent); margin-top: 32px; }}
  h2 {{ font-size: 1.1em; margin-top: 24px; }}
  .updated {{ color: var(--muted); font-size: .9em; }}
  hr {{ border: 0; border-top: 1px solid var(--muted); margin: 40px 0; }}
</style>
</head>
<body>
<main>
<nav><a href="#fr">Français</a><a href="#en">English</a></nav>
{block("fr", data["updated"], "Mise à jour :")}
<hr>
{block("en", data["updatedEn"], "Last updated:")}
</main>
</body>
</html>
"""
out = os.path.join(ROOT, "docs/confidentialite.html")
open(out, "w", encoding="utf-8").write(page)
print("écrit :", os.path.relpath(out, ROOT))
