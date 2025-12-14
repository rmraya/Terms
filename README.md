# Terms Extractor

Java tools for extracting terms from XLIFF 2.0 files.

This project is based on the paper *YAKE! Keyword extraction from single documents using multiple local features* by Ricardo Campos, Vítor Mangaravite, Arian Pasquali, Alípio Jorge, Célia Nunes and Adam Jatowt.

## Features

- **Monolingual Term Extraction**: Extract terms from source text in XLIFF files
- **Bilingual Term Extraction**: Extract translation pair candidates from XLIFF files with confirmed translations
- **Automatic Deduplication**: Intelligent merging of similar terms
- **Multiple Quality Filters**: Co-occurrence, mutual best match, and relevance-based filtering

## Requirements for building

- Java 21 (get it from [https://adoptium.net/](https://adoptium.net/))
- Gradle 9.2 or newer (get it from [https://gradle.org/install/](https://gradle.org/install/))

### Building

Follow these steps to build the project:

```bash
git clone https://github.com/rmraya/Terms.git
cd Terms
gradle
```

A binary distribution will be created in `/dist` folder.

## Usage

### Monolingual Term Extraction

Execute `dist/extractTerms.sh` or `dist\extractTerms.cmd` and the program will display the following usage information:

``` bash
INFO: Usage:

    termExtractor [-version] [-help] -xliff xliffFile [-output outputFile] [-minFreq frequency] [-maxLength length] [-maxScore score] [-generic] [-debug]

Where:

        -version:   (optional) Display version information and exit
        -help:      (optional) Display this usage information and exit
        -xliff:     The XLIFF file to process
        -output:    (optional) The output file where the terms will be written
        -maxLength: (optional) The maximum number of words in a term. Default: 3
        -minFreq:   (optional) The minimum frequency for a term to be considered. Default: 3
        -maxScore:  (optional) The maximum score for a term to be considered. Default: 0.001
        -generic:   (optional) Include terms with relevance < 1.0. Default: false
        -debug:     (optional) Enable debug mode with detailed logging. Default: false
```

By default, the program extracts terms with a minimum frequency of 3, a maximum length of 3 words, and a maximum score of 10.0. All terms (both single-word and multi-word) are included by default.

Use the `-relevant` flag to exclude single-word terms and focus only on multi-word terms and proper nouns (words with unusual capitalization patterns).

**Output Format:**

The program writes a CSV (comma separated values) file with the same name as the supplied XLIFF file with the `.csv` extension, containing the following columns:

|Column| Description|
|:--:|--|
|#| The candidate term number|
|Term| The term candidate|
|Score| The term score, calculated using the values from the remaining columns.|
|Casing| Insidence of the term case when not used at the start of a sentence. The underlying rationale is that uppercase terms tend to be more relevant than lowercase ones.|
|Position| Insidence of the term position in the XLIFF file. The rationale is that relevant keywords tend to appear at the very beginning of a document, whereas words occurring in the middle or at the end of a document tend to be less important.|
|Frequency| The number of occurrences of the term in the XLIFF file.|
|Relevance| Inverse of the normalized term frequency. The rationale is that common words are less relevant than rare ones.|
|Relatedness| A value which aims to determine the dispersion of a candidate term with regards to its specific context, calculated considering the words that appear before and after the term in the same sentence.|
|Different| A measurement of how often a candidate term appears within different sentences. It reflects the assumption that candidates which appear in many different sentences have a higher probability of being important.|

### Bilingual Term Extraction

Execute `dist/bilingualExtractor.sh` or `dist\bilingualExtractor.cmd` to extract translation pair candidates from bilingual XLIFF files:

``` bash
bilingualExtractor [-version] [-help] -xliff xliffFile [-output outputFile] 
                   [-minFreq frequency] [-maxLength length] [-maxScore score]
                   [-minCoOccurrence count] [-maxPairs limit] [-minCoOccurrenceRatio ratio]
                   [-debug]

Where:

        -version:              (optional) Display version information and exit
        -help:                 (optional) Display this usage information and exit
        -xliff:                The XLIFF file to process (must contain translations with state="final")
        -output:               (optional) The output CSV file. Default: xliffFile_bilingual.csv
        -maxLength:            (optional) Maximum number of words in a term. Default: 5
        -minFreq:              (optional) Minimum frequency for a term. Default: 3
        -maxScore:             (optional) Maximum YAKE score for a term. Default: 10.0
        -minCoOccurrence:      (optional) Minimum times terms must co-occur. Default: 1
        -maxPairs:             (optional) Maximum number of pairs to output (0 = unlimited). Default: 0
        -minCoOccurrenceRatio: (optional) Minimum ratio of co-occurrence to total occurrences. Default: 0.7
        -debug:                (optional) Enable debug mode with detailed logging. Default: false
```

**How It Works:**

1. Processes only segments with `state="final"` (confirmed translations)
2. Extracts terms separately from source and target text using YAKE algorithm
3. Identifies term pairs that co-occur in the same segments
4. Applies mutual best match filtering: keeps only pairs where each term's best match is the other
5. Filters by co-occurrence count and ratio
6. Deduplicates pairs keeping the best scoring variants

**Quality Filters:**

- **Mutual Best Match**: Ensures each source term's highest co-occurrence target is the paired target term, and vice versa. This eliminates false pairs from terms that merely appear in the same segment.
- **Co-occurrence Ratio**: Default 0.7 means terms must co-occur in at least 70% of segments where either term appears
- **Minimum Length**: Terms must be at least 2 characters (eliminates single letters)

**Output Format:**

CSV file with the following columns:

|Column|Description|
|:--:|--|
|Source Term|The source language term|
|Source Score|YAKE score for the source term (lower is better)|
|Source Frequency|Number of occurrences of source term|
|Target Term|The target language term|
|Target Score|YAKE score for the target term (lower is better)|
|Target Frequency|Number of occurrences of target term|
|Shared Segments|Segment numbers where both terms co-occur|
|Co-occurrence Count|Number of segments where both terms appear together|

## Term Deduplication

The program automatically deduplicates extracted terms using two strategies:

1. **Case-insensitive matching**: Merges terms that differ only in capitalization (e.g., "Machine Learning" and "machine learning")
2. **Similarity matching**: Merges terms that are similar based on Levenshtein distance with 85% similarity threshold, including:
   - Substring relationships (e.g., "learning" vs "machine learning")
   - Minor spelling variations

When duplicates are found, the program keeps the variant with the lowest score (best in YAKE), or if scores are equal, the one with highest frequency.

## Credits

Stop words lists extracted from [https://github.com/Alir3z4/stop-words](https://github.com/Alir3z4/stop-words). Supported languages are:

- Arabic
- Bulgarian
- Catalan
- Czech
- Danish
- Dutch
- English
- Finnish
- French
- German
- Gujarati
- Hindi
- Hebrew
- Hungarian
- Indonesian
- Malaysian
- Italian
- Norwegian
- Polish
- Portuguese
- Romanian
- Russian
- Slovak
- Spanish
- Swedish
- Turkish
- Ukrainian
- Vietnamese
- Persian/Farsi
