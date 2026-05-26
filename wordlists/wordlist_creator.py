"""
wordlist_creator.py — Wortel-Wortlisten-Generator.

Architektur
-----------

Quelle: Hunspell de_DE_frami als Source of Truth für "das ist ein
deutsches Wort". 163.000 validierte Lemmas + Flexionsformen, kein
englischer Subtitle-Müll, kein OCR-Rauschen.

Pipeline:

    Hunspell .dic                              wordfreq            spaCy
        |                                         |                  |
        v                                         v                  v
    [Länge 4-7, Lowercase, [a-zäöüß]+, Profanity-Filter]
        |
        +----> WORDS_N  (Valid-Liste, "alle deutschen Wörter")
        |
        v
    [Namens-Filter]
        |
    [POS: NOUN/ADJ/VERB only, spaCy-Cap-Hedge]
        |
    [Lemma == Wort -> nur Grundformen]
        |
    [wordfreq Zipf >= MIN_ZIPF, sortiert nach Zipf desc, Top-N]
        |
        v
    SOLUTIONS_N  (Solutions-Liste, "Rateziele")

Verwendung
----------

    # Einmalig: Modelle/Pakete
    pip install -r wordlists/requirements.txt
    python -m spacy download de_core_news_md

    # Voller Lauf, alle Längen
    cd wordlists
    python wordlist_creator.py

    # Schnell iterieren — nur 5-Buchstaben-Wörter, Cap und Zipf tweaken
    python wordlist_creator.py --length 5 --top-n 1500 --min-zipf 3.2

    # Nur Valid bauen (überspringt das teure POS-Tagging)
    python wordlist_creator.py --skip-solutions

    # Nichts schreiben, nur Stats
    python wordlist_creator.py --dry-run

    # In Sandbox-Ordner schreiben statt edge function überschreiben
    python wordlist_creator.py --output-dir _test

    # Debug-TXT mit Zipf-Score pro Wort (für Frequenz-Inspektion)
    python wordlist_creator.py --debug-zipf

Caches
------

Hunspell- und Namenslisten werden in `_cache/` lokal abgelegt nach dem
ersten Download. Cache leeren um Quellen neu zu ziehen:

    rm -rf wordlists/_cache
"""

from __future__ import annotations

import argparse
import re
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

import spacy
from wordfreq import top_n_list, zipf_frequency


# ============================================================
# Defaults (per CLI überschreibbar)
# ============================================================

WORD_LENGTHS: tuple[int, ...] = (4, 5, 6, 7)

# Wie viele Solutions maximal pro Länge ins TS-Modul.
DEFAULT_TOP_N_SOLUTIONS = 2000

# Zipf-Skala (0–7) aus dem wordfreq-Paket:
#   7   = "der/die/und" (extrem häufig)
#   5–6 = alltagshäufig ("sehen", "klein")
#   4–5 = bekannt ("apfel", "frieden")
#   3–4 = kennt man ("brille", "donner")
#   2–3 = selten gehört, aber existent
#   <2  = obskur, archaisch, technische Fragmente
#
# DEFAULT_MIN_ZIPF_VALID filtert die Valid-Liste:
#   - Hunspell enthält ~163k Einträge inkl. archaischer/dialektaler
#     Formen ("aalst", "zähre", "bohlt"). Ohne Frequenzfilter rutscht
#     viel davon in die Valid-Liste, was Spieler verwirrt.
#   - 2.0 = "kommt in irgendeinem deutschen Korpus vor".
#   - Höher = sauberer aber kürzer. Niedriger = mehr Wörter aber mehr Müll.
DEFAULT_MIN_ZIPF_VALID = 2.0

# DEFAULT_MIN_ZIPF_SOLUTIONS filtert die Solutions-Liste:
#   3.9 = "definitiv schon gehört, kommt regelmäßig vor". Rateziel
#   sollte allgemein bekannt sein, sonst frustriert das Spiel.
DEFAULT_MIN_ZIPF_SOLUTIONS = 3.9

# Wie viele Wörter aus wordfreq's top-N-Liste als zweite Quelle dazu.
# Hunspell de_DE_frami deckt klassisches Deutsch ab, kennt aber kaum
# Lehnwörter (Manager, Look, Internet) und kaum moderne Slang-Begriffe.
# wordfreq's top-N füllt diese Lücke. 1_000_000 ist großzügig — die
# Zipf-Schwelle siebt am Ende, was wirklich häufig genug ist.
DEFAULT_WORDFREQ_TOP_N = 1_000_000

# wordfreq hat zwei Wortlisten:
#   "best"  = konservativ, nur Wörter die in mehreren Korpora vorkommen
#   "large" = großzügig, alle Korpora vereint, ~2-3x mehr Einträge
# Wir wollen Quantität auf der Valid-Seite -> "large".
DEFAULT_WORDFREQ_LIST = "large"


# ============================================================
# Pfade
# ============================================================

HERE = Path(__file__).resolve().parent
CACHE_DIR = HERE / "_cache"
DEFAULT_DEBUG_DIR = HERE / "_debug"
DEFAULT_OUTPUT_DIR = (
    HERE.parent / "supabase" / "functions" / "_shared" / "infrastructure" / "data"
)


# ============================================================
# Quell-URLs
# ============================================================

HUNSPELL_DIC_URL = (
    "https://raw.githubusercontent.com/LibreOffice/dictionaries/"
    "master/de/de_DE_frami.dic"
)
FIRSTNAMES_URL = (
    "https://raw.githubusercontent.com/PenTestical/german_names/"
    "master/2000_german_firstnames.txt"
)
SURNAMES_URL = (
    "https://raw.githubusercontent.com/PenTestical/german_names/"
    "master/most_common_german_surnames.txt"
)


# ============================================================
# Blocklists
# ============================================================

GEO_BLOCKLIST = {
    # Städte
    "berlin", "köln", "bonn", "kiel", "ulm", "trier", "bremen", "mainz",
    "essen", "gera", "jena", "wien", "zürich", "graz", "linz", "basel",
    "paris", "rom", "london", "tokio", "moskau", "kairo",
    # Länder / Regionen
    "europa", "asien", "afrika", "china", "polen", "italien", "spanien",
    "bayern", "hessen", "sachsen", "preussen", "preußen",
    # Religion
    "gott", "jesus", "allah", "buddha", "satan", "teufel",
}

# Lemmas, die nie als Solution UND nie als Valid akzeptiert werden.
# Konjugierte/deklinierte Formen werden via spaCy-Lemma-Check (Solutions)
# bzw. direkten Wort-Check (Valid) gefangen.
PROFANITY_BLOCKLIST = {
    # Sex / Geschlechtsteile
    "arsch", "fotze", "ficken", "schwanz", "möse", "muschi", "nutte",
    "hure", "puff", "votze", "pimmel", "titte", "titten", "vulva",
    "penis", "pussy", "bumsen", "vögeln", "blasen",
    # Skatologisch
    "kacke", "kacken", "kotze", "kotzen", "pisse", "pissen",
    "scheiße", "schiss", "rotzen", "furzen",
    # Onanie
    "wichser", "wichsen",
    # Slurs / Beleidigungen
    "neger", "kanake", "polacke", "spast", "spasti", "krüppel",
    "tunte", "trottel", "idiot", "depp",
    # Sexuelle Orientierung als Slur-Material
    "schwul", "lesbe", "tucke",
    # Gewalt / Tod
    "mord", "morden", "töten", "suizid", "würgen", "amok", "leiche",
    # Drogen
    "heroin", "kokain", "koks", "kiffen", "saufen",
    # NS-Bezug
    "nazi", "hitler", "stasi", "ghetto", "rasse",
    # Misc Tabuthemen
    "krebs", "tumor", "aids", "sucht", "knast", "henker",
}

VALID_CHAR_RE = re.compile(r"^[a-zäöüß]+$")

ALLOWED_POS = {"NOUN", "ADJ", "VERB"}
HARD_VETO_POS = {
    "NUM", "INTJ", "PRON", "DET", "ADP", "CCONJ", "SCONJ",
    "PART", "AUX", "PROPN", "X",
}


# ============================================================
# Loader (mit Cache)
# ============================================================


def fetch_lines(url: str) -> list[str]:
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req) as response:
        raw = response.read()
    for enc in ("utf-8", "latin-1"):
        try:
            return raw.decode(enc).splitlines()
        except UnicodeDecodeError:
            continue
    raise RuntimeError(f"Konnte {url} nicht dekodieren.")


def _cache_path(name: str) -> Path:
    return CACHE_DIR / name


def load_hunspell_entries() -> set[str]:
    """
    Lade Hunspell de_DE_frami und gib alle Einträge (lowercase) zurück.
    Gecached unter _cache/hunspell.txt — erst beim ersten Lauf wird
    aus dem Netz geladen.
    """
    cache = _cache_path("hunspell.txt")
    if cache.exists():
        return set(cache.read_text("utf-8").splitlines())

    print("Lade Hunspell-Wörterbuch von LibreOffice...")
    lines = fetch_lines(HUNSPELL_DIC_URL)
    words: set[str] = set()
    start = 1 if lines and lines[0].strip().isdigit() else 0
    for line in lines[start:]:
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        head = stripped.split("/", 1)[0].strip()
        if head:
            words.add(head.lower())

    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    cache.write_text("\n".join(sorted(words)), "utf-8")
    print(f"  -> {len(words)} Wörter geladen (gecached: {cache}).")
    return words


def load_wordfreq_top(n: int, wordlist: str) -> set[str]:
    """
    Lade die top-N häufigsten deutschen Wörter aus wordfreq als
    zusätzliche Quelle neben Hunspell. wordfreq kombiniert Subtitles,
    Wikipedia, Common Crawl und Twitter und kennt damit auch:
      - Lehnwörter (Computer, Internet, Manager, Look)
      - moderne / Slang-Begriffe
      - Verbformen die Hunspell-Frami nicht explizit listet
    Die Liste ist bereits lowercase und ausschließlich deutsch
    klassifiziert. Wir vertrauen wordfreq's Sprachfilter.

    wordlist="best"  -> konservativ, nur Multi-Korpus-Wörter
    wordlist="large" -> großzügig, alle Korpora vereint
    """
    print(f"Lade wordfreq top-{n} ({wordlist}) deutsche Wörter...")
    words = set(top_n_list("de", n, wordlist=wordlist))
    print(f"  -> {len(words)} Wörter.")
    return words


def load_names_blocklist() -> set[str]:
    """
    Vor- und Nachnamen aus PenTestical-Repos, kombiniert mit der
    Geo-Blocklist. Gecached unter _cache/names.txt.
    """
    cache = _cache_path("names.txt")
    if cache.exists():
        return set(cache.read_text("utf-8").splitlines())

    print("Lade Namens-Blocklist...")
    names = set(GEO_BLOCKLIST)
    try:
        for line in fetch_lines(FIRSTNAMES_URL):
            name = line.strip()
            if name:
                names.add(name.lower())
        for line in fetch_lines(SURNAMES_URL):
            name = line.strip()
            # Nur original großgeschriebene Einträge als Nachname werten —
            # vermeidet, dass Adjektive ("klein", "lang", "schwarz")
            # versehentlich auf der Blocklist landen.
            if name and name[0].isupper():
                names.add(name.lower())
    except Exception as exc:  # noqa: BLE001
        print(f"  Warnung: Namens-Download fehlgeschlagen ({exc}).")

    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    cache.write_text("\n".join(sorted(names)), "utf-8")
    print(f"  -> {len(names)} Einträge (gecached: {cache}).")
    return names


def load_nlp():
    for model in ("de_core_news_lg", "de_core_news_md", "de_core_news_sm"):
        try:
            print(f"Lade spaCy-Modell: {model}")
            return spacy.load(model)
        except OSError:
            continue
    print("\nFehler: kein deutsches spaCy-Modell installiert. Installiere mit:")
    print("  python -m spacy download de_core_news_md   # empfohlen (~40 MB)")
    raise SystemExit(1)


# ============================================================
# Solutions-Klassifizierung (batched über spaCy)
# ============================================================


def extract_lemmas(
    nlp,
    candidates: list[str],
    lengths: tuple[int, ...],
) -> dict[str, str]:
    """
    Lemmatisiert ALLE Kandidaten und sammelt die einzigartigen Lemmas.

    Statt "ist dieses Wort selbst ein Lemma?" zu fragen (was den
    Solutions-Pool stark verkleinert), gehen wir andersrum vor: jedes
    Flexionsform-Wort steuert sein Lemma zur Solutions-Liste bei.

      "kleine"   -> Lemma "klein"
      "Hunde"    -> Lemma "Hund"
      "ging"     -> Lemma "gehen"
      "haben"    -> Lemma "haben"
      "schöne"   -> Lemma "schön"

    Lower-Priority-Hedge:
      Naives "tagge lower UND cap und nimm beide" füttert über den
      Cap-Pfad Müll-Lemmas ein. spaCy ohne Satzkontext taggt
      großgeschriebene Funktionsworte wie "Wurde", "Weil", "Zwei",
      "Muss" gerne als NOUN und liefert das Surface-Form-Wort selbst
      als Lemma zurück — dadurch landen Verb-Flexionen und Partikeln
      als "solution-eligible" in der Liste.

      Fix: zuerst die Lowercase-Variante befragen.
        - Lower sagt HARD_VETO (NUM, INTJ, PRON, DET, ADP, CCONJ,
          SCONJ, PART, AUX-... wait, AUX akzeptieren wir, kein Veto):
          ist strukturell kein Inhaltswort -> raus, kein Cap-Override.
        - Lower sagt CONTENT (NOUN, ADJ, VERB, AUX): Lemma nehmen,
          fertig. Cap-Pass wird übersprungen, sonst doppelt einsortiert.
        - Lower sagt PROPN / X / ADV / sonstwas: keine harte Aussage.
          Cap-Variante als Fallback ziehen, dort denselben Check.

    PROPN/X NICHT als Hard-Veto: viele Substantive werden lowercase
    fälschlich als PROPN getaggt; der Cap-Fallback fischt sie wieder
    ein.

    Rückgabe: dict mapping lemma -> origin_word.
    """
    if not candidates:
        return {}

    lower_inputs = [c.lower() for c in candidates]
    cap_inputs = [c[:1].upper() + c[1:] for c in lower_inputs]

    print(f"  spaCy: lemmatisiere {len(candidates)} Wörter (lower + cap)...")
    t0 = time.time()
    docs_low = list(nlp.pipe(lower_inputs, batch_size=2000))
    docs_cap = list(nlp.pipe(cap_inputs, batch_size=2000))
    print(f"  spaCy: fertig in {time.time() - t0:.1f}s.")

    # Strukturell-Nicht-Inhaltsklassen — sehen wir die in lowercase,
    # ist das Wort kein Inhaltswort. AUX wandert NICHT hier rein:
    # "hast/bin/war" sind als Lemma-Quelle willkommen ("haben/sein").
    HARD_VETO = {
        "NUM", "INTJ", "PRON", "DET", "ADP",
        "CCONJ", "SCONJ", "PART",
    }
    CONTENT_POS = {"NOUN", "ADJ", "VERB", "AUX"}

    def try_record(token, surface_word: str) -> bool:
        """
        Versucht das Lemma des Tokens zu speichern. Liefert True, wenn
        es geklappt hat (Filter-Pässe), sonst False.
        """
        if token.pos_ in HARD_VETO:
            return False
        if token.pos_ not in CONTENT_POS:
            return False
        lemma = token.lemma_.lower()
        if not lemma or len(lemma) not in lengths:
            return False
        if not VALID_CHAR_RE.match(lemma):
            return False
        if lemma in PROFANITY_BLOCKLIST:
            return False
        if lemma not in lemmas:
            lemmas[lemma] = surface_word
        return True

    lemmas: dict[str, str] = {}
    for word, d_low, d_cap in zip(lower_inputs, docs_low, docs_cap):
        t_low = d_low[0] if len(d_low) else None

        # 1) Lowercase-Verdikt absolut. HARD_VETO -> kein Cap-Override.
        if t_low is not None and t_low.pos_ in HARD_VETO:
            continue

        # 2) Lowercase liefert Content-POS -> recorden, fertig.
        if t_low is not None and t_low.pos_ in CONTENT_POS:
            try_record(t_low, word)
            continue

        # 3) Lowercase war ambig (PROPN, X, ADV, ...) -> Cap probieren.
        t_cap = d_cap[0] if len(d_cap) else None
        if t_cap is None:
            continue
        if t_cap.pos_ in HARD_VETO:
            continue
        try_record(t_cap, word)

    return lemmas


# ============================================================
# Output
# ============================================================


def write_ts_module(
    entries: list[tuple[str, float, bool]],
    length: int,
    output_dir: Path,
) -> Path:
    """
    Vereinte Liste pro Länge: ein TS-Modul mit allen deutschen Wörtern
    dieser Länge. Jeder Eintrag ist [word, zipf, solutionEligible]:

      word              -> lowercase, Umlaute behalten
      zipf              -> wordfreq Zipf-Score (0-7, höher = häufiger).
                           Server kann nach Schwierigkeit filtern.
      solutionEligible  -> true, wenn das Wort als Rateziel taugen
                           könnte (kein Eigenname, POS NOUN/ADJ/VERB,
                           Lemma-Grundform). Server filtert Solutions
                           per (solutionEligible AND zipf >= 3.9) oder
                           wie auch immer der Difficulty-Slider justiert.

    Damit gibt es nur EINE Datei pro Länge — keine separate
    solutions_N_letters.ts mehr.
    """
    var_name = f"WORDS_{length}"
    filename = f"words_{length}_letters.ts"

    output_dir.mkdir(parents=True, exist_ok=True)
    path = output_dir / filename
    lines: list[str] = [
        "// Auto-generated by wordlists/wordlist_creator.py. Do not edit by hand.",
        "// Regenerate: cd wordlists && python wordlist_creator.py",
        "//",
        "// Each entry is [word, zipf, solutionEligible]:",
        "//   word              = lowercase, umlauts intact",
        "//   zipf              = wordfreq Zipf score (0-7, higher = more common)",
        "//   solutionEligible  = true if the word may be picked as a target",
        "//                       (not a proper noun, NOUN/ADJ/VERB, lemma form)",
        "",
        f"export const {var_name}: "
        "readonly (readonly [string, number, boolean])[] = [",
    ]
    lines.extend(
        f'  ["{w}", {z:.2f}, {"true" if s else "false"}],'
        for w, z, s in entries
    )
    lines.append("];")
    lines.append("")
    path.write_text("\n".join(lines), encoding="utf-8")
    return path


def delete_legacy_solutions_files(output_dir: Path, lengths: tuple[int, ...]) -> None:
    """
    Alte solutions_N_letters.ts Dateien aus dem vorherigen Zwei-Listen-
    Output entfernen. Wir haben jetzt eine vereinte Liste pro Länge,
    die separate solutions-Datei wird obsolet.
    """
    for n in lengths:
        legacy = output_dir / f"solutions_{n}_letters.ts"
        if legacy.exists():
            print(f"  Lösche obsolete Datei: {legacy.name}")
            legacy.unlink()


def write_debug_txt(
    entries: list[tuple[str, float]],
    length: int,
    kind: str,
    debug_dir: Path,
    with_zipf: bool = False,
) -> Path:
    debug_dir.mkdir(parents=True, exist_ok=True)
    path = debug_dir / f"{kind}_{length}_letters.txt"
    if with_zipf:
        # Nach Zipf desc, damit häufigste oben stehen.
        ordered = sorted(entries, key=lambda x: -x[1])
        lines = [f"{w}\t{z:.2f}" for w, z in ordered]
    else:
        lines = [w for w, _ in entries]
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return path


# ============================================================
# CLI
# ============================================================


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(
        description="Generiert Valid- und Solutions-Wortlisten "
        "für die Wortel-App. Quelle: Hunspell de_DE_frami. "
        "Solutions werden zusätzlich per spaCy POS-Tagging und "
        "wordfreq Zipf-Ranking gefiltert.",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
    )
    p.add_argument(
        "--length", type=int, choices=WORD_LENGTHS,
        help="Nur eine bestimmte Wortlänge prozessieren (4, 5, 6 oder 7).",
    )
    p.add_argument(
        "--top-n", type=int, default=DEFAULT_TOP_N_SOLUTIONS,
        help="Maximale Anzahl Solutions pro Länge.",
    )
    p.add_argument(
        "--min-zipf-valid", type=float, default=DEFAULT_MIN_ZIPF_VALID,
        help="Mindest-Zipf-Frequenz, damit ein Wort in die Valid-Liste kommt. "
        "Filtert archaische / obskure Einträge raus.",
    )
    p.add_argument(
        "--wordfreq-top", type=int, default=DEFAULT_WORDFREQ_TOP_N,
        help="Top-N häufigste DE-Wörter aus wordfreq als zweite Quelle "
        "neben Hunspell. 0 = nur Hunspell.",
    )
    p.add_argument(
        "--wordfreq-list", choices=("best", "large"),
        default=DEFAULT_WORDFREQ_LIST,
        help="wordfreq wordlist-Variante. 'best' konservativ, "
        "'large' deutlich umfangreicher.",
    )
    p.add_argument(
        "--min-zipf", type=float, default=DEFAULT_MIN_ZIPF_SOLUTIONS,
        dest="min_zipf_solutions",
        help="Mindest-Zipf-Frequenz, damit ein Wort als Solution zählt.",
    )
    p.add_argument(
        "--output-dir", type=Path, default=DEFAULT_OUTPUT_DIR,
        help="Wohin die TS-Module geschrieben werden. Default = Edge-Function-Bundle.",
    )
    p.add_argument(
        "--debug-dir", type=Path, default=DEFAULT_DEBUG_DIR,
        help="Wohin die Klartext-Debug-Files geschrieben werden.",
    )
    p.add_argument(
        "--dry-run", action="store_true",
        help="Keine Dateien schreiben, nur Statistik ausgeben.",
    )
    p.add_argument(
        "--skip-solutions", action="store_true",
        help="Nur Valid-Liste bauen (überspringt spaCy + wordfreq).",
    )
    p.add_argument(
        "--debug-zipf", action="store_true",
        help="Debug-TXT für Solutions enthält Zipf-Score pro Wort.",
    )
    return p.parse_args()


# ============================================================
# Main
# ============================================================


def main() -> int:
    args = parse_args()
    lengths = (args.length,) if args.length else WORD_LENGTHS

    print(f"Längen:                 {lengths}")
    print(f"Output dir:             {args.output_dir}")
    print(f"Debug dir:              {args.debug_dir}")
    print(f"Top-N solutions:        {args.top_n}")
    print(f"Min-Zipf valid:         {args.min_zipf_valid}")
    print(f"Min-Zipf solutions:     {args.min_zipf_solutions}")
    print(f"wordfreq top-N source:  {args.wordfreq_top}")
    print(f"Dry-Run:                {args.dry_run}")
    print()

    hunspell = load_hunspell_entries()
    wordfreq_top = (
        load_wordfreq_top(args.wordfreq_top, args.wordfreq_list)
        if args.wordfreq_top > 0
        else set()
    )
    source = hunspell | wordfreq_top
    print(
        f"\nVereinte Quelle: {len(hunspell)} Hunspell + "
        f"{len(wordfreq_top)} wordfreq = {len(source)} (nach Union/Dedup)\n"
    )

    # ---------------------------------------------------------------
    # Valid-Liste pro Länge: alles Deutsche dieser Länge
    # mit Zipf-Frequenz >= MIN_ZIPF_VALID.
    # ---------------------------------------------------------------
    print("Bau Valid-Listen...")
    raw_by_length: dict[int, list[str]] = {n: [] for n in lengths}
    for entry in source:
        if len(entry) not in lengths:
            continue
        lower = entry.lower()
        if not VALID_CHAR_RE.match(lower):
            continue
        if lower in PROFANITY_BLOCKLIST:
            continue
        raw_by_length[len(lower)].append(lower)

    valid_by_length: dict[int, list[tuple[str, float]]] = {n: [] for n in lengths}
    for n in lengths:
        raw = sorted(set(raw_by_length[n]))
        # Zipf-Verteilung berechnen — hilft beim Tunen.
        zipf_pairs = [(w, zipf_frequency(w, "de")) for w in raw]
        buckets = {
            ">=5.0": 0, ">=4.0": 0, ">=3.0": 0,
            ">=2.0": 0, ">=1.0": 0,  "< 1.0": 0,
        }
        for _, z in zipf_pairs:
            if z >= 5.0:
                buckets[">=5.0"] += 1
            elif z >= 4.0:
                buckets[">=4.0"] += 1
            elif z >= 3.0:
                buckets[">=3.0"] += 1
            elif z >= 2.0:
                buckets[">=2.0"] += 1
            elif z >= 1.0:
                buckets[">=1.0"] += 1
            else:
                buckets["< 1.0"] += 1

        # Zipf-Score pro Wort behalten — landet im TS-Modul für späteren
        # Schwierigkeits-Slider. Alphabetisch sortiert für menschliche
        # Inspektion (für Set-Lookup ist die Reihenfolge eh egal).
        kept = sorted(
            [(w, z) for w, z in zipf_pairs if z >= args.min_zipf_valid],
            key=lambda x: x[0],
        )
        valid_by_length[n] = kept

        print(f"  Länge {n}: {len(raw)} aus Quelle (Hunspell ∪ wordfreq)")
        print(f"    Zipf-Verteilung: " + ", ".join(
            f"{k}={v}" for k, v in buckets.items()
        ))
        print(f"    -> {len(kept)} valid (Zipf >= {args.min_zipf_valid})")

    # ---------------------------------------------------------------
    # Solutions-Liste pro Länge: Subset, gefiltert + ranked
    # ---------------------------------------------------------------
    # solution_eligible_words[N] = Set von Lemmas, die als Rateziel
    # taugen (POS NOUN/ADJ/VERB, kein Name, Grundform). Zipf-Schwelle
    # NICHT hier angewendet — das macht der Server zur Laufzeit über
    # den Difficulty-Slider.
    solution_eligible_by_length: dict[int, set[str]] = {n: set() for n in lengths}

    if args.skip_solutions:
        print("\n--skip-solutions: keine Solution-Eligibility berechnet.")
    else:
        names = load_names_blocklist()
        nlp = load_nlp()

        # Lemma-Extraktion über alle Längen. Eine 6-Buchstaben-Flexion
        # kann ein 5-Buchstaben-Lemma beisteuern. Das vervielfacht den
        # Solution-Pool gegenüber "Wort muss selbst Lemma sein".
        print("\nLemma-Extraktion für Solution-Eligibility über alle Längen...")
        all_candidates: list[str] = []
        for n in lengths:
            all_candidates.extend(w for w, _ in valid_by_length[n])
        print(f"  Pool: {len(all_candidates)} Wörter (alle Längen)")

        lemmas_seen = extract_lemmas(nlp, all_candidates, lengths)
        print(f"  Extrahierte Lemmas: {len(lemmas_seen)}")

        # Namens-Filter auf extrahierte Lemmas
        for lemma in lemmas_seen:
            if lemma in names:
                continue
            solution_eligible_by_length[len(lemma)].add(lemma)

        for n in lengths:
            print(
                f"  Länge {n}: {len(solution_eligible_by_length[n])} "
                "solution-eligible Lemmas"
            )

    # ---------------------------------------------------------------
    # Vereinte Liste pro Länge: alle Valid-Wörter + alle eligible
    # Lemmas, jeweils mit Zipf-Score und Solution-Flag.
    # ---------------------------------------------------------------
    print("\nVereine zu einer Liste pro Länge...")
    unified_by_length: dict[int, list[tuple[str, float, bool]]] = {
        n: [] for n in lengths
    }
    for n in lengths:
        # Start: Valid-Liste (alle Wörter mit Zipf >= MIN_ZIPF_VALID)
        valid_words: dict[str, float] = dict(valid_by_length[n])

        # Lemmas, die nicht schon in Valid sind, mit ihrem Zipf
        # hinzufügen — sonst gingen valide Lemmas verloren, deren
        # Surface-Form nicht im Quell-Pool stand.
        for lemma in solution_eligible_by_length[n]:
            if lemma not in valid_words:
                valid_words[lemma] = zipf_frequency(lemma, "de")

        # Flag setzen + alphabetisch sortieren
        eligible = solution_eligible_by_length[n]
        for word in sorted(valid_words):
            unified_by_length[n].append(
                (word, valid_words[word], word in eligible)
            )

        n_total = len(unified_by_length[n])
        n_solutions = sum(1 for _, _, s in unified_by_length[n] if s)
        print(
            f"  Länge {n}: {n_total} Wörter, davon {n_solutions} "
            "solution-eligible"
        )

    # ---------------------------------------------------------------
    # Output
    # ---------------------------------------------------------------
    if args.dry_run:
        print("\n--dry-run: keine Files geschrieben.")
        return 0

    print("\nSchreibe Output-Dateien...")
    delete_legacy_solutions_files(args.output_dir, lengths)
    for n in lengths:
        write_ts_module(unified_by_length[n], n, args.output_dir)
        # Debug-TXT: alphabetisch + Zipf + Solution-Flag.
        debug_lines = [
            f"{w}\t{z:.2f}\t{'S' if s else '-'}"
            for w, z, s in unified_by_length[n]
        ]
        args.debug_dir.mkdir(parents=True, exist_ok=True)
        (args.debug_dir / f"words_{n}_letters.txt").write_text(
            "\n".join(debug_lines) + "\n", encoding="utf-8",
        )
        print(f"  Länge {n}: ✓")

    print(f"\nFertig.")
    print(f"  TS-Module: {args.output_dir}")
    print(f"  Debug-TXT: {args.debug_dir}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
