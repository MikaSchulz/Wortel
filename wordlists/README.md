# Wortlisten-Generator

Erzeugt **eine vereinte Liste pro Wortlänge** (4-7) für die Wortel-App:

- **`words_N_letters.ts`** — pro Eintrag `[word, zipf, solutionEligible]`:
  - `word`             — lowercase, Umlaute behalten
  - `zipf`             — wordfreq Zipf-Score (0-7, höher = häufiger)
  - `solutionEligible` — `true` wenn das Wort als Rateziel taugen darf
                          (POS NOUN/ADJ/VERB, kein Eigenname, Grundform)

Der Server filtert daraus zur Laufzeit:
- **Valid (Eingabe-Akzeptanz)** = alle Einträge
- **Solutions (Rateziel)**       = Einträge mit `solutionEligible &&
                                     zipf >= 3.9` (Schwellwert hängt vom
                                     Difficulty-Slider ab)

Schreibt direkt ins Edge-Function-Bundle:
`supabase/functions/_shared/infrastructure/data/`.

## Setup

```bash
cd wordlists
pip install -r requirements.txt
python -m spacy download de_core_news_md   # ~40 MB
```

## Ausführung

```bash
# Voller Lauf — schreibt 8 TS-Module + 8 Debug-TXT
python wordlist_creator.py

# Nur eine Länge (schnell)
python wordlist_creator.py --length 5

# Parameter tweaken
python wordlist_creator.py --top-n 1500 --min-zipf 3.5

# In Sandbox-Ordner statt Edge Function überschreiben
python wordlist_creator.py --output-dir _test

# Nur Valid bauen (POS-Tagging weglassen, ~10x schneller)
python wordlist_creator.py --skip-solutions

# Trockenlauf (zählt nur, schreibt nichts)
python wordlist_creator.py --dry-run

# Debug-TXT mit Zipf-Score pro Wort
python wordlist_creator.py --debug-zipf

# Alle Flags
python wordlist_creator.py --help
```

## Parameter-Tuning

Wichtige Hebel:

| Flag | Default | Effekt |
|------|---------|--------|
| `--min-zipf-valid` | 2.0 | Mindesthäufigkeit für die Valid-Liste. Filtert archaische / dialektale Hunspell-Einträge ("aalst", "zähre") weg. |
| `--min-zipf` | 3.0 | Mindesthäufigkeit für Solutions. Höher = nur sehr gängige Wörter. |
| `--top-n` | 2000 | Hardcap pro Länge. Wird nach Zipf-Sortierung gekappt. |
| `--length` | alle | Nur eine Länge testen (4, 5, 6 oder 7). |

Zipf-Skala:

- 7 = "der/die/und" (extrem häufig)
- 5–6 = alltagshäufig ("sehen", "klein")
- 4–5 = bekannt ("apfel", "frieden")
- 3–4 = kennt man ("brille", "donner")
- 2–3 = selten gehört
- <2 = obskur

## Quellen

| Was | Woher |
|-----|-------|
| Deutsche Wörter | [LibreOffice/dictionaries/de/de_DE_frami.dic](https://github.com/LibreOffice/dictionaries/blob/master/de/de_DE_frami.dic) |
| Vor-/Nachnamen | [PenTestical/german_names](https://github.com/PenTestical/german_names) |
| Frequenz | `wordfreq` Python-Paket (lokal, kein Netzwerk) |
| POS+Lemma | spaCy `de_core_news_md` |

## Caches

Hunspell- und Namenslisten werden in `_cache/` abgelegt. Cache leeren
um Quellen frisch zu ziehen:

```bash
rm -rf _cache
```

## Debug-Output

Nach jedem Lauf liegen Klartext-Dateien in `_debug/`:

- `valid_N_letters.txt`     — alle akzeptierten Wörter
- `solutions_N_letters.txt` — Rateziele (mit `--debug-zipf` inkl. Zipf)

Diese sind nicht im Git getrackt, dienen nur der Sichtung.
