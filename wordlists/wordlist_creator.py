"""
wordlist_creator.py — Wortel-Wortlisten-Generator.

Pipeline:
  1. Lade Frequenzliste (hermitdave/FrequencyWords, OpenSubtitles2018 DE).
  2. Lade Namens-Blocklist (Vor- + Nachnamen aus PenTestical/german_names).
  3. Filter:
       - Länge 4-7
       - nur deutsche Buchstaben (a-zA-Z, Umlaute, ß) — Umlaute werden behalten
       - keine Eigennamen (Namens-Blocklist)
       - keine Geo-/sakralen Begriffe
       - keine Profanität (PROFANITY_BLOCKLIST)
       - Häufigkeit >= MIN_FREQ_VALID
  4. POS-Tag via spaCy de_core_news_sm -> behalte NOUN/ADJ/VERB.
  5. Lemma-Check -> behalte nur Grundformen (kein "stehst", "Hunde",
     "kleine"; aber "stehen", "Hund", "klein").
  6. Output pro Länge zwei TS-Module:
       - solutions_N_letters.ts  -> Top-N häufigste, dienen als Rate-Ziel
       - words_N_letters.ts      -> Komplette Valid-Liste fürs Akzeptieren

Die Edge Function nutzt SOLUTIONS für randomWord(), beide Sets gemeinsam
für isValid(). Damit kriegen Spieler bekannte Wörter zu raten, dürfen aber
seltenere Wörter als Guess eingeben ohne Soft-Reject.

Run:
    cd wordlists
    python -m spacy download de_core_news_sm   # einmalig
    python wordlist_creator.py
"""

from __future__ import annotations

import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

import spacy


# ============================================================
# CONFIG
# ============================================================

# Mindesthäufigkeit für Aufnahme in die Akzeptanzliste.
# Quelle ist OpenSubtitles2018-DE. 1000 öffnet das Reservoir auf
# "kommt im Alltag durchaus vor" — der POS+Lemma-Filter siebt das
# Rauschen danach wieder raus. Mit 3000 blieb zu wenig übrig.
MIN_FREQ_VALID = 1000

# Anzahl der häufigsten Wörter, die als Rate-Ziel verwendet werden.
# Kleinere Zahl = bekanntere Lösungen = weniger Spielerfrust.
TOP_N_SOLUTIONS = 1500

# Welche Wortlängen wir bauen.
WORD_LENGTHS = (4, 5, 6, 7)

# Quell-URLs.
WORDS_URL = (
    "https://raw.githubusercontent.com/hermitdave/FrequencyWords/"
    "refs/heads/master/content/2018/de/de_full.txt"
)
FIRSTNAMES_URL = (
    "https://raw.githubusercontent.com/PenTestical/german_names/"
    "master/2000_german_firstnames.txt"
)
SURNAMES_URL = (
    "https://raw.githubusercontent.com/PenTestical/german_names/"
    "master/most_common_german_surnames.txt"
)
# Hunspell-Wörterbuch als Whitelist gegen englisches Subtitles-Rauschen.
# OpenSubtitles2018-DE enthält viele englische Tokens (Namen, Filmtitel,
# Dialogfragmente). Wörter wie "burt", "jodi", "hugh", "look" passieren
# unsere Frequenz- und POS-Filter, weil spaCy unbekannte Tokens default
# als NOUN taggt und lemma == word zurückgibt. Hunspells de_DE_frami
# enthält nur tatsächlich existierende deutsche Wörter (inkl. flektierter
# Formen), das schneidet das fremdsprachige Rauschen sauber ab.
HUNSPELL_DIC_URL = (
    "https://raw.githubusercontent.com/LibreOffice/dictionaries/"
    "master/de/de_DE_frami.dic"
)

# Output-Pfad: TS-Module direkt in das Edge-Function-Bundle schreiben.
HERE = Path(__file__).resolve().parent
EDGE_DATA_DIR = (
    HERE.parent / "supabase" / "functions" / "_shared" / "infrastructure" / "data"
)

# Optionaler Debug-Output als .txt zum Sichten der generierten Listen.
DEBUG_TXT_DIR = HERE / "_debug"
WRITE_DEBUG_TXT = True


# ============================================================
# BLOCKLISTS
# ============================================================

GEO_BLOCKLIST = {
    # Städte
    "berlin", "köln", "bonn", "kiel", "ulm", "trier", "bremen", "mainz",
    "essen", "gera", "jena", "wien", "zürich", "graz", "linz", "basel",
    "paris", "rom", "london", "tokio", "moskau", "kairo",
    # Länder / Regionen
    "europa", "asien", "afrika", "china", "polen", "italien", "spanien",
    "bayern", "hessen", "sachsen", "preussen", "preußen",
    # Religiös
    "gott", "jesus", "allah", "buddha", "satan", "teufel",
}

# Profanity-Blocklist. Eintrage als Lemma (Grundform) — die Filter-Pipeline
# prüft sowohl die Eingabeform als auch die spaCy-Lemma-Ausgabe gegen diese
# Menge, also fangen wir konjugierte/deklinierte Varianten automatisch.
# Alle Einträge sind 4-7 Zeichen lang (alles andere greift der Längenfilter
# ohnehin nicht).
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
    "mord", "morden", "töten", "suizid", "würgen", "amok",
    "leiche",
    # Drogen
    "heroin", "kokain", "koks", "kiffen", "saufen",
    # NS-Bezug / historisch belastet
    "nazi", "hitler", "stasi", "ghetto", "rasse",
    # Religion (Solution-untauglich für säkulares Wortspiel)
    "allah", "satan", "teufel", "jesus",
    # Misc Tabuthemen
    "krebs", "tumor", "aids", "sucht", "knast", "henker",
}

VALID_CHAR_RE = re.compile(r"^[a-zA-ZäöüÄÖÜß]+$")
ALLOWED_POS = {"NOUN", "ADJ", "VERB"}


# ============================================================
# HELPERS
# ============================================================


def fetch_lines(url: str) -> list[str]:
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req) as response:
        raw = response.read()
    try:
        return raw.decode("utf-8").splitlines()
    except UnicodeDecodeError:
        return raw.decode("latin-1").splitlines()


def load_german_whitelist() -> set[str]:
    """
    Lade Hunspell-de_DE_frami als Whitelist deutscher Wörter (inkl.
    flektierter Formen). Wird gegen Eingaben aus der Frequenzliste
    geprüft, um englisches/fremdsprachiges Rauschen rauszuwerfen.

    Hunspell-.dic-Format:
        N            <- erste Zeile = Anzahl Einträge
        Wort/FLAGS   <- ein Eintrag pro Zeile, FLAGS optional nach Slash
        Wort
        ...
    """
    print("Lade Hunspell-de_DE Whitelist...")
    try:
        lines = fetch_lines(HUNSPELL_DIC_URL)
    except Exception as exc:  # noqa: BLE001
        print(f"  Warnung: Hunspell-Download fehlgeschlagen ({exc}). "
              "Filter ist ohne Whitelist aktiv -> mehr englisches Rauschen.")
        return set()

    words: set[str] = set()
    # Erste Zeile ist die Eintragsanzahl. Falls die Datei das nicht enthält
    # (manche Mirrors weichen ab), beginnen wir trotzdem ab Index 0.
    start = 1 if lines and lines[0].strip().isdigit() else 0
    for line in lines[start:]:
        stripped = line.strip()
        if not stripped:
            continue
        # Hunspell-.dic enthält manchmal #-Kommentarzeilen (Header,
        # Versionshinweis). Die nicht als Wörter aufnehmen.
        if stripped.startswith("#"):
            continue
        head = stripped.split("/", 1)[0].strip()
        if not head:
            continue
        words.add(head.lower())
    print(f"  -> {len(words)} deutsche Wörter im Whitelist-Set.")
    return words


def load_names_blocklist() -> set[str]:
    names: set[str] = set(GEO_BLOCKLIST)
    print("Lade Namenslisten für Eigennamen-Blocklist...")
    try:
        for line in fetch_lines(FIRSTNAMES_URL):
            name = line.strip()
            if name:
                names.add(name.lower())

        for line in fetch_lines(SURNAMES_URL):
            name = line.strip()
            # Nur original großgeschriebene Einträge als Nachnamen werten.
            # Verhindert, dass Wörter wie "klein", "lang", "schwarz" rausfliegen,
            # nur weil sie auch als Nachname existieren.
            if name and name[0].isupper():
                names.add(name.lower())
    except Exception as exc:  # noqa: BLE001
        print(f"Warnung: Namenslisten-Download fehlgeschlagen ({exc}). "
              "Nur Basis-Blocklist aktiv.")
    print(f"  -> {len(names)} Einträge.")
    return names


def write_ts_module(words: list[str], length: int, kind: str) -> Path:
    """
    kind = "valid"     -> exportiert WORDS_{length}
    kind = "solutions" -> exportiert SOLUTIONS_{length}
    """
    if kind == "valid":
        var_name = f"WORDS_{length}"
        filename = f"words_{length}_letters.ts"
    elif kind == "solutions":
        var_name = f"SOLUTIONS_{length}"
        filename = f"solutions_{length}_letters.ts"
    else:
        raise ValueError(f"Unknown kind: {kind}")

    EDGE_DATA_DIR.mkdir(parents=True, exist_ok=True)
    path = EDGE_DATA_DIR / filename

    lines: list[str] = [
        "// Auto-generated by wordlists/wordlist_creator.py. Do not edit by hand.",
        "// Regenerate: cd wordlists && python wordlist_creator.py",
        "",
        f"export const {var_name}: readonly string[] = [",
    ]
    lines.extend(f'  "{w}",' for w in words)
    lines.append("];")
    lines.append("")
    path.write_text("\n".join(lines), encoding="utf-8")
    return path


def write_debug_txt(words: list[str], length: int, kind: str) -> None:
    if not WRITE_DEBUG_TXT:
        return
    DEBUG_TXT_DIR.mkdir(parents=True, exist_ok=True)
    path = DEBUG_TXT_DIR / f"{kind}_{length}_letters.txt"
    path.write_text("\n".join(words) + "\n", encoding="utf-8")


def load_nlp() -> "spacy.language.Language":
    """
    Lade das beste verfügbare deutsche spaCy-Modell. _md ist deutlich
    genauer beim POS-Tagging isolierter Wörter als _sm; _lg wenn der
    Nutzer es installiert hat noch besser. Wir fallen in der Reihenfolge
    md -> sm zurück und brechen sonst mit Installationshinweis ab.
    """
    candidates = ("de_core_news_lg", "de_core_news_md", "de_core_news_sm")
    for model in candidates:
        try:
            print(f"Lade spaCy-Modell: {model}")
            return spacy.load(model)
        except OSError:
            print(f"  {model} nicht installiert.")
    print("\nFehler: kein deutsches spaCy-Modell verfügbar. Installation:")
    print("  python -m spacy download de_core_news_md   # empfohlen (~40 MB)")
    print("  python -m spacy download de_core_news_sm   # minimal (~15 MB)")
    raise SystemExit(1)


def analyze_word(nlp, word: str):
    """
    POS-Tagging mit Capitalization-Hedge UND Veto durch Lowercase.

    Naiver Cap-Hedge produziert üble False-Positives: "hast" lowercase
    -> VERB lemma "haben" (richtig erkannt als Konjugation), wird aber
    "Hast" als seltenes Nomen ("die Hast" = Eile) wieder reingespült.
    Selbe Falle bei "sage" (-> Sage), "halt" (-> Halt), "eins" (-> Eins),
    "gehe"/"sehe"/"sieh"/"ging" (alle nur Verbflexionen).

    Strategie: lowercase zuerst befragen. Wenn der eine eindeutige
    Nicht-Lemma-Form als VERB/AUX taggt -> rejecten (Konjugation).
    Wenn er eine harte Nicht-Erlaubt-POS taggt (Zahl, Partikel,
    Pronomen, Determiner, Präposition, Konjunktion) -> rejecten.
    Nur wenn lowercase ambig/unauffällig ist, ziehen wir die
    Cap-Variante als Fallback heran — die ist nur fürs Aufpolieren
    von Substantiven da, die spaCy ohne Großbuchstaben verwirft.

    Rückgabe: spaCy-Token bei Erfolg, sonst None.
    """
    lower = word.lower()
    cap = lower[:1].upper() + lower[1:]

    # POS-Kategorien, die wir nie als gültiges Wortspiel-Wort akzeptieren,
    # auch wenn die Cap-Variante etwas anderes behauptet.
    LOWERCASE_VETO = {
        "NUM",   # eins, zwei, drei
        "INTJ",  # naja, hallo, äh
        "PRON",  # ich, du, wir, mich
        "DET",   # der, die, das, ein
        "ADP",   # in, auf, mit, durch
        "CCONJ", # und, oder, aber
        "SCONJ", # weil, dass, ob
        "PART",  # nicht, ja, doch (Modalpartikel)
        "AUX",   # bin, hast, war, würde — Hilfsverbformen sind nie Lemma
    }

    doc_low = nlp(lower)
    t_low = doc_low[0] if doc_low else None

    # 1) Konjugationen: lowercase VERB mit Lemma != Wort -> hart raus.
    if t_low and t_low.pos_ == "VERB" and t_low.lemma_.lower() != lower:
        return None

    # 2) Harte Lowercase-Veto-Kategorien.
    if t_low and t_low.pos_ in LOWERCASE_VETO:
        return None

    # 3) Lowercase-Akzeptanz: erlaubte POS + Lemma stimmt.
    if (
        t_low
        and t_low.pos_ in ALLOWED_POS
        and t_low.text.lower() == t_low.lemma_.lower()
    ):
        return t_low

    # 4) Fallback Cap-Variante — fischt Substantive ein, die spaCy
    #    lowercase fälschlich als PROPN/ADV taggt.
    if cap != lower:
        doc_cap = nlp(cap)
        if doc_cap:
            t_cap = doc_cap[0]
            if (
                t_cap.pos_ in ALLOWED_POS
                and t_cap.text.lower() == t_cap.lemma_.lower()
            ):
                return t_cap

    return None


# ============================================================
# MAIN
# ============================================================


def main() -> int:
    # spaCy-Modell laden — Lemma + POS. Größtes verfügbares zuerst.
    nlp = load_nlp()

    german_whitelist = load_german_whitelist()
    names_blocklist = load_names_blocklist()

    print("\nLade Frequenzliste...")
    try:
        raw_lines = fetch_lines(WORDS_URL)
    except urllib.error.HTTPError as exc:
        print(f"Fehler beim Download der Wortliste: HTTP {exc.code}")
        return 1
    print(f"  -> {len(raw_lines)} Einträge.")

    # Sammle pro Länge bereits frequenzabsteigend (Quelle ist so sortiert).
    by_length: dict[int, list[str]] = {n: [] for n in WORD_LENGTHS}
    seen: set[str] = set()

    print("\nFilter + POS-Tagging (das kann ein paar Minuten dauern)...")
    processed = 0
    for line in raw_lines:
        processed += 1
        if processed % 25_000 == 0:
            print(f"  ... {processed} Zeilen verarbeitet")

        parts = line.strip().split()
        if len(parts) < 2:
            continue

        word = parts[0]
        try:
            freq = int(parts[1])
        except ValueError:
            continue

        # Frequenzliste ist absteigend sortiert -> früh abbrechen sobald
        # wir unter die Schwelle fallen.
        if freq < MIN_FREQ_VALID:
            break

        if len(word) not in WORD_LENGTHS:
            continue
        if not VALID_CHAR_RE.match(word):
            continue

        word_lower = word.lower()
        if word_lower in seen:
            continue
        if word_lower in names_blocklist:
            continue
        if word_lower in PROFANITY_BLOCKLIST:
            continue
        # Whitelist-Filter: nur wenn Hunspell das Wort kennt. Ohne diese
        # Hürde rutschen englische Tokens (burt, jodi, hugh, look) durch,
        # weil spaCy unbekannte Wörter als generisches NOUN klassifiziert.
        # Skip nur wenn die Whitelist tatsächlich geladen wurde — sonst
        # würden wir bei Download-Fehler alles verwerfen.
        if german_whitelist and word_lower not in german_whitelist:
            continue

        # POS-Tag mit Capitalization-Hedge: spaCy braucht Großschreibung
        # für Substantive. Wir testen klein UND groß, akzeptieren wenn
        # eine Variante einen erlaubten POS-Tag + Lemma-Match liefert.
        token = analyze_word(nlp, word)
        if token is None:
            continue

        # Lemma-Profanity-Check (zusätzlich zum Wort-Check oben).
        if token.lemma_.lower() in PROFANITY_BLOCKLIST:
            continue

        seen.add(word_lower)
        by_length[len(word)].append(word_lower)

    total = sum(len(v) for v in by_length.values())
    print(f"\nAkzeptiert: {total} Wörter über alle Längen.\n")

    print("Schreibe TS-Module + Debug-Output...")
    for length in WORD_LENGTHS:
        valid_words = by_length[length]
        solutions = valid_words[:TOP_N_SOLUTIONS]

        write_ts_module(valid_words, length, "valid")
        write_ts_module(solutions, length, "solutions")
        write_debug_txt(valid_words, length, "valid")
        write_debug_txt(solutions, length, "solutions")

        print(
            f"  Länge {length}: "
            f"{len(valid_words)} valid / {len(solutions)} solutions"
        )

    print(f"\nFertig. TS-Module in {EDGE_DATA_DIR}")
    if WRITE_DEBUG_TXT:
        print(f"Debug-TXT in {DEBUG_TXT_DIR}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
