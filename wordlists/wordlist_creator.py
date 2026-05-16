import urllib.request
import re

# INSERT your own raw GitHub link here
URL = "https://raw.githubusercontent.com/hermitdave/FrequencyWords/refs/heads/master/content/2018/de/de_full.txt"

# Target dictionary for internal grouping
word_dictionary = {
    5: [],
    6: [],
    7: []
}

seen = set()
# Filter for purely alphabetical words (including German umlauts and ß)
valid_pattern = re.compile(r"^[a-zA-ZäöüÄÖÜß]+$")

print("Fetching wordlist from GitHub and starting filtration...")

try:
    # Send request with a User-Agent to prevent GitHub from blocking the download
    req = urllib.request.Request(URL, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req) as response:
        raw_content = response.read().decode('utf-8')
        lines = raw_content.splitlines()

    for line in lines:
        parts = line.strip().split()
        if len(parts) < 2:
            continue

        word = parts[0]

        # Extract the frequency from the second column
        try:
            frequency = int(parts[1])
        except ValueError:
            continue

        # FILTER 1: Frequency must be at least 100
        if frequency < 100:
            # If your GitHub list is strictly sorted from highest to lowest frequency,
            # you can change 'continue' to 'break' here to speed up the script.
            continue

        word_length = len(word)

        # FILTER 2 & 3: Exact length (5-7) and clean text without special characters
        if 5 <= word_length <= 7 and valid_pattern.match(word):
            word_lower = word.lower()

            # Prevent duplicates caused by different casing
            if word_lower not in seen:
                seen.add(word_lower)
                word_dictionary[word_length].append(word)

    # --- EXPORT AS SEPARATE FILES ---
    print("\nCreating separate export files...")

    for length, words in word_dictionary.items():
        # Dynamic filename based on word length
        output_filename = f"words_{length}_letters.txt"

        with open(output_filename, "w", encoding="utf-8") as f:
            # Write words separated by a newline
            f.write("\n".join(words))

        print(f"-> '{output_filename}' successfully created ({len(words)} words).")

    print("\nDone! All files have been saved to your current directory.")

except urllib.error.HTTPError as e:
    print(f"Error downloading from GitHub (HTTP Error {e.code}). Is the URL correct and the repo public?")
except Exception as e:
    print(f"An error occurred: {e}")