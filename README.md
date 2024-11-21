# Terms Extractor

Java tools for extractiong terms from XLIFF 2.0 files.

This project is based on the paper *YAKE! Keyword extraction from single documents using multiple local features* by Ricardo Campos, Vítor Mangaravite, Arian Pasquali, Alípio Jorge, Célia Nunes and Adam Jatowt.

## Requirements for building

- Java 21 (get it from [https://adoptium.net/](https://adoptium.net/))
- Apache Ant 1.10.14 or newer (get it from [https://ant.apache.org/bindownload.cgi](https://ant.apache.org/bindownload.cgi))

### Building

Follow these steps to build the project:

```bash
git clone https://github.com/rmraya/Terms.git
cd Terms
ant
```

A binary distribution will be created in `/dist` folder.

## Usage

Execute `dist/extractTerms.sh` or `dist\extractTerms.cmd` and the program will display the following usage information:

``` bash
INFO: Usage:

    termExtractor [-version] [-help] -xliff xliffFile [-output outputFile] [-minFreq frequency] [-maxLenght length] [-maxScore score] [-generic]

Where:

        -version:   (optional) Display version information and exit
        -help:      (optional) Display this usage information and exit
        -xliff:     The XLIFF file to process
        -output:    (optional) The output file where the terms will be written
        -maxLenght: (optional) The maximum number of words in a term. Default: 3
        -minFreq:   (optional) The minimum frequency for a term to be considered. Default: 3
        -maxScore:  (optional) The maximum score for a term to be considered. Default: 0.001
        -generic:   (optional) Include terms with relevance < 1.0. Default: false
```

By default, the program extracts terms with a minimum frequency of 3, a maximum length of 3 words, and a maximum score of 0.001. Terms with a relevance less than 1.0 are excluded by default.

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
