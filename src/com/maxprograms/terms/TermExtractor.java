/*******************************************************************************
 * Copyright (c) 2024 Maxprograms.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors: Maxprograms - initial API and implementation
 *******************************************************************************/

package com.maxprograms.terms;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.text.BreakIterator;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.SAXBuilder;

public class TermExtractor {

    private static final int WINDOW = 1;

    private List<String> stopWords;
    private String srcLang;

    private List<String> sentences;
    private Map<String, Integer> index;
    private Vector<String[]> chunks;
    private BreakIterator sentenceIterator;
    private BreakIterator wordsIterator;
    private Locale locale;
    private List<Term> terms;

    public static void main(String[] args) {
        args = Utils.fixPath(args);

        String xliff = "";
        String output = "";
        int minFrequency = 3;
        double maxScore = 0.001;
        boolean relevant = true;
        int maxTermLenght = 3;

        for (int i = 0; i < args.length; i++) {
            if ("-xliff".equals(args[i]) && i + 1 < args.length) {
                xliff = args[i + 1];
            }
            if ("-output".equals(args[i]) && i + 1 < args.length) {
                output = args[i + 1];
            }
            if ("-minFreq".equals(args[i]) && i + 1 < args.length) {
                minFrequency = Integer.parseInt(args[i + 1]);
            }
            if ("-maxScore".equals(args[i]) && i + 1 < args.length) {
                maxScore = Double.parseDouble(args[i + 1]);
            }
            if ("-generic".equals(args[i])) {
                relevant = false;
            }
            if ("-maxLenght".equals(args[i]) && i + 1 < args.length) {
                maxTermLenght = Integer.parseInt(args[i + 1]);
            }
            if ("-version".equals(args[i])) {
                Logger logger = System.getLogger(TermExtractor.class.getName());
                MessageFormat mf = new MessageFormat("Version {0} Build {1}");
                logger.log(Level.INFO, mf.format(new String[] { Constans.VERSION, Constans.BUILD }));
                System.exit(0);
            }
            if ("-help".equals(args[i])) {
                usage();
                System.exit(0);
            }
        }
        if (xliff.isEmpty()) {
            usage();
            System.exit(1);
        }
        if (output.isEmpty()) {
            File file = new File(xliff);
            String path = file.getAbsolutePath();
            output = path.substring(0, path.lastIndexOf('.')) + ".csv";
        }
        try {
            TermExtractor extractor = new TermExtractor(xliff, maxTermLenght, minFrequency, maxScore, relevant);
            List<Term> list = extractor.getTerms();
            Collections.sort(list);
            try (FileOutputStream out = new FileOutputStream(new File(output))) {
                out.write(new byte[] { -1, -2 });
                String title = "#,term,score,casing,position,frequency,relevance,relatedness,different\n";
                out.write(title.getBytes(StandardCharsets.UTF_16LE));
                for (int i = 0; i < list.size(); i++) {
                    out.write(((i + 1) + "," + list.get(i).getData() + "\n").getBytes(StandardCharsets.UTF_16LE));
                }
            }
        } catch (SAXException | IOException | ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    private static void usage() {
        Logger logger = System.getLogger(TermExtractor.class.getName());
        logger.log(Level.INFO,
                """
                        Usage:

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
                        """);
    }

    private List<Term> getTerms() {
        return terms;
    }

    public TermExtractor(String xliffFile, int maxTermLenght, int minFrequency, double maxScore, boolean relevant)
            throws IOException, SAXException, ParserConfigurationException {
        sentences = new Vector<>();
        chunks = new Vector<>();

        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(xliffFile);
        Element root = doc.getRootElement();
        if (!("xliff".equals(root.getName()) && root.getAttributeValue("version").startsWith("2."))) {
            throw new IOException("Selected file is not an XLIFF 2.x document");
        }
        srcLang = root.getAttributeValue("srcLang");
        locale = Locale.forLanguageTag(srcLang);
        sentenceIterator = BreakIterator.getSentenceInstance(locale);
        wordsIterator = BreakIterator.getWordInstance(locale);
        stopWords = StopWords.getStopWords(srcLang);
        buildSentences(root);
        preProcess();
        termStatistics();
        featureComputation();
        generateCandidates(maxTermLenght, minFrequency, maxScore, relevant);
    }

    private void buildSentences(Element e) {
        if ("segment".equals(e.getName())) {
            Element source = e.getChild("source");
            String sourceText = Utils.pureText(source);
            if (!sourceText.isBlank()) {
                sentenceIterator.setText(sourceText);
                int start = sentenceIterator.first();
                for (int end = sentenceIterator.next(); end != BreakIterator.DONE; start = end, end = sentenceIterator
                        .next()) {
                    String sentence = sourceText.substring(start, end).replace('\u00A0', ' ');
                    sentences.add(sentence.strip());
                }
            }
        } else {
            List<Element> children = e.getChildren();
            Iterator<Element> it = children.iterator();
            while (it.hasNext()) {
                buildSentences(it.next());
            }
        }
    }

    private void preProcess() {
        for (int i = 0; i < sentences.size(); i++) {
            String[] array = getChunks(sentences.get(i));
            chunks.add(array);
        }
    }

    private void termStatistics() {
        terms = new Vector<>();
        index = new HashMap<>();
        for (int i = 0; i < sentences.size(); i++) {
            String[] chunkArray = chunks.get(i);
            for (int j = 0; j < chunkArray.length; j++) {
                String chunk = chunkArray[j];
                List<Token> tokens = getTokens(chunk, j == 0);
                for (int k = 0; k < tokens.size(); k++) {
                    Token token = tokens.get(k);
                    String key = token.getLower();
                    if (!index.containsKey(key)) {
                        terms.add(new Term(token.getToken()));
                        index.put(key, terms.size() - 1);
                    }
                    int idx = index.get(key);
                    Term term = terms.get(idx);
                    term.increaseFrequency();
                    term.setSentence(i);
                    if (Token.ACRONYM.equals(token.getTag())) {
                        term.increaseAcronym();
                    }
                    if (Token.UPPERCASE.equals(token.getTag())) {
                        term.increaseUpperCase();
                    }
                    for (int m = 1; m <= WINDOW; m++) {
                        if (k - m >= 0) {
                            Token previous = tokens.get(k - m);
                            if (previous.isRelatable()) {
                                term.addLeft(previous.getLower());
                            }
                        }
                        if (k + m < tokens.size()) {
                            Token next = tokens.get(k + m);
                            if (next.isRelatable()) {
                                term.addRight(next.getLower());
                            }
                        }
                    }
                }
            }
        }
    }

    private void featureComputation() {
        List<Integer> frequencies = new Vector<>();
        int maxFrequency = 0;
        for (int i = 0; i < terms.size(); i++) {
            Term term = terms.get(i);
            int frequency = term.getTermFrequency();
            frequencies.add(frequency);
            if (frequency > maxFrequency) {
                maxFrequency = frequency;
            }
        }
        double meanFrequency = Utils.median(frequencies);
        double sDeviation = standardDeviation(frequencies.toArray(new Integer[frequencies.size()]));
        for (int i = 0; i < terms.size(); i++) {
            Term term = terms.get(i);
            term.calcFrequency(meanFrequency, sDeviation);
            term.calcDifferent(sentences.size());
            term.calcRelatednes(maxFrequency);
            term.calcScore();
        }
    }

    private String[] getChunks(String sentence) {
        List<String> list = new Vector<>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sentence.length(); i++) {
            char c = sentence.charAt(i);
            if ((i > 0 && i < sentence.length() - 1) && (c == '.')) {
                // check if we have a number
                char before = sentence.charAt(i - 1);
                char after = sentence.charAt(i + 1);
                if (Character.isDigit(before) && Character.isDigit(after)) {
                    sb.append(c);
                    continue;
                }
            }
            if (!isPunctuation(c)) {
                sb.append(c);
            } else {
                list.add(sb.toString());
                sb = new StringBuilder();
            }
        }
        if (!sb.isEmpty()) {
            list.add(sb.toString());
        }
        return list.toArray(new String[list.size()]);
    }

    private boolean isPunctuation(char c) {
        int type = Character.getType(c);
        return type == Character.CONNECTOR_PUNCTUATION || type == Character.DASH_PUNCTUATION
                || type == Character.END_PUNCTUATION || type == Character.FINAL_QUOTE_PUNCTUATION
                || type == Character.INITIAL_QUOTE_PUNCTUATION || type == Character.OTHER_PUNCTUATION
                || type == Character.START_PUNCTUATION;
    }

    private List<Token> getTokens(String chunk, boolean firstChunk) {
        List<String> words = new Vector<>();
        if (!chunk.isBlank()) {
            wordsIterator.setText(chunk);
            int start = wordsIterator.first();
            for (int end = wordsIterator.next(); end != BreakIterator.DONE; start = end, end = wordsIterator.next()) {
                String word = chunk.substring(start, end);
                if (!word.isBlank()) {
                    words.add(word);
                }
            }
        }
        List<Token> tokens = new Vector<>();
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            tokens.add(new Token(word, isStopWord(word), locale, i == 0 && firstChunk));
        }
        return tokens;
    }

    private boolean isStopWord(String token) {
        return stopWords.contains(token.toLowerCase(locale));
    }

    public static double standardDeviation(Integer[] frequencies) {
        int sum = 0;
        for (int num : frequencies) {
            sum += num;
        }
        int length = frequencies.length;
        double mean = sum / length;
        double deviations = 0.0;
        for (int num : frequencies) {
            deviations += Math.pow(num - mean, 2);
        }
        double variance = deviations / length;
        return Math.sqrt(variance);
    }

    public void generateCandidates(int maxTermLenght, int minFrequency, double maxScore, boolean relevant) {
        for (int i = 0; i < chunks.size(); i++) {
            String[] array = chunks.get(i);
            for (int j = 0; j < array.length; j++) {
                String chunk = array[j];
                List<Token> tokens = getTokens(chunk, j == 0);
                for (int h = 0; h < tokens.size(); h++) {
                    List<Token> candidate = new Vector<>();
                    Token token = tokens.get(h);
                    if (token.isRelatable()) {
                        for (int k = 0; k < maxTermLenght && (h + k) < tokens.size(); k++) {
                            candidate.add(tokens.get(h + k));
                            String string = "";
                            Iterator<Token> it = candidate.iterator();
                            while (it.hasNext()) {
                                string += it.next().getToken() + ' ';
                            }
                            string = string.strip();
                            if (!candidate.get(0).isStopWord() && !candidate.get(candidate.size() - 1).isStopWord()) {
                                String key = string.toLowerCase();
                                if (!index.containsKey(key)) {
                                    terms.add(new Term(string));
                                    index.put(key, terms.size() - 1);
                                }
                                int idx = index.get(key);
                                Term term = terms.get(idx);
                                term.increaseFrequency();
                                term.setScore(calcScore(candidate, term.getTermFrequency()));
                            }
                        }
                    }
                }
            }
        }
        if (relevant) {
            terms.removeIf(term -> term.getRelevance() < 1.0);
        }
        terms.removeIf(term -> term.getScore() > maxScore);
        terms.removeIf(term -> isStopWord(term.getTerm()));
        terms.removeIf(term -> term.getTermFrequency() < minFrequency);
        terms.removeIf(term -> isNumber(term.getTerm()));
    }

    private boolean isNumber(String term) {
        try {
            Double.parseDouble(term);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private double calcScore(List<Token> tokens, int frequency) {
        double prod = 1;
        double sum = 0;
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (!token.isStopWord()) {
                int idx = index.get(token.getLower());
                Term term = terms.get(idx);
                prod *= term.getScore();
                sum += term.getScore();
            } else {
                Token tokenBefore = tokens.get(i - 1);
                int idx = index.get(tokenBefore.getLower());
                Term termBefore = terms.get(idx);
                Token tokenAfter = tokens.get(i + 1);
                idx = index.get(tokenAfter.getLower());
                Term termAfter = terms.get(idx);
                double bigramProbability = termBefore.getScore() * termAfter.getScore();
                prod *= 1 + (1 - bigramProbability);
                sum += (1 - bigramProbability);
            }
        }
        double score = prod / (frequency * (sum + 1));
        return score;
    }
}
