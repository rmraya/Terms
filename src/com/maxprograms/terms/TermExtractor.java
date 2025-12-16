/*******************************************************************************
 * Copyright (c) 2024 - 2025 Maxprograms.
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

    private static Logger logger = System.getLogger(TermExtractor.class.getName());
    private static final int WINDOW = 2;
    private static boolean debug = false;

    private List<String> stopWords;
    private String srcLang;

    private List<String> sentences;
    private Map<String, Integer> index;
    private Vector<String[]> chunks;
    private BreakIterator sentenceIterator;
    private BreakIterator wordsIterator;
    private Locale locale;
    private List<Term> terms;
    private List<Integer> sentenceToSegmentNumber;
    private int currentSegmentNumber;

    public static void main(String[] args) {
        args = Utils.fixPath(args);

        String xliff = "";
        String output = "";
        int minFrequency = 3;
        double maxScore = 10.0;
        boolean relevant = false;
        int maxTermLenght = 3;

        try {
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
                if ("-relevant".equals(args[i])) {
                    relevant = true;
                }
                if ("-maxLength".equals(args[i]) && i + 1 < args.length) {
                    maxTermLenght = Integer.parseInt(args[i + 1]);
                }
                if ("-version".equals(args[i])) {
                    MessageFormat mf = new MessageFormat(Messages.getString("TermExtractor.4"));
                    logger.log(Level.INFO, mf.format(new String[] { Constants.VERSION, Constants.BUILD }));
                    System.exit(0);
                }
                if ("-help".equals(args[i])) {
                    usage();
                    System.exit(0);
                }
                if ("-debug".equals(args[i])) {
                    debug = true;
                }
            }
            if (xliff.isEmpty()) {
                usage();
                System.exit(1);
            }
            // Validate parameters
            if (minFrequency < 1) {
                logger.log(Level.ERROR, Messages.getString("TermExtractor.6"));
                System.exit(1);
            }
            if (maxScore <= 0) {
                logger.log(Level.ERROR, Messages.getString("TermExtractor.7"));
                System.exit(1);
            }
            if (maxTermLenght < 1) {
                logger.log(Level.ERROR, Messages.getString("TermExtractor.8"));
                System.exit(1);
            }
            File xliffFile = new File(xliff);
            if (!xliffFile.exists()) {
                MessageFormat mf = new MessageFormat(Messages.getString("TermExtractor.9"));
                logger.log(Level.ERROR, mf.format(new String[] { xliff }));
                System.exit(1);
            }
            if (output.isEmpty()) {
                File file = new File(xliff);
                String path = file.getAbsolutePath();
                if (path.lastIndexOf('.') == -1) {
                    output = path + ".csv";
                } else {
                    output = path.substring(0, path.lastIndexOf('.')) + ".csv";
                }
            }
            TermExtractor extractor = new TermExtractor(xliff, maxTermLenght, minFrequency, maxScore, relevant);
            List<Term> list = extractor.getTerms();
            Collections.sort(list);
            try (FileOutputStream out = new FileOutputStream(new File(output))) {
                out.write(new byte[] { -1, -2 });
                String title = Messages.getString("TermExtractor.1");
                out.write(title.getBytes(StandardCharsets.UTF_16LE));
                for (int i = 0; i < list.size(); i++) {
                    out.write(((i + 1) + "," + list.get(i).getData() + "\n").getBytes(StandardCharsets.UTF_16LE));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageFormat mf = new MessageFormat(Messages.getString("TermExtractor.5"));
            logger.log(Level.ERROR, mf.format(new String[] { e.getClass().getSimpleName(), e.getMessage() }));
        }
    }

    private static void usage() {
        logger.log(Level.INFO, Messages.getString("TermExtractor.2"));
    }

    public List<Term> getTerms() {
        return terms;
    }

    public List<Integer> getSentenceToSegmentMap() {
        return sentenceToSegmentNumber;
    }

    public TermExtractor(String xliffFile, int maxTermLenght, int minFrequency, double maxScore, boolean relevant)
            throws IOException, SAXException, ParserConfigurationException {
        sentences = new Vector<>();
        chunks = new Vector<>();
        sentenceToSegmentNumber = new Vector<>();
        currentSegmentNumber = 0;

        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(xliffFile);
        Element root = doc.getRootElement();
        if (!("xliff".equals(root.getName()) && root.getAttributeValue("version").startsWith("2."))) {
            throw new IOException(Messages.getString("TermExtractor.3"));
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
        deduplicateTerms();
    }

    private void buildSentences(Element e) {
        if ("segment".equals(e.getName())) {
            currentSegmentNumber++;
            Element source = e.getChild("source");
            if (source == null) {
                // Skip segments without source element
                return;
            }
            String sourceText = Utils.pureText(source);
            if (!sourceText.isBlank()) {
                sentenceIterator.setText(sourceText);
                int start = sentenceIterator.first();
                for (int end = sentenceIterator.next(); end != BreakIterator.DONE; start = end, end = sentenceIterator
                        .next()) {
                    String sentence = sourceText.substring(start, end).replace('\u00A0', ' ');
                    sentences.add(sentence.strip());
                    sentenceToSegmentNumber.add(currentSegmentNumber);
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
                        terms.add(new Term(token.getText()));
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
        int sumFrequency = 0;
        for (int i = 0; i < terms.size(); i++) {
            Term term = terms.get(i);
            int frequency = term.getTermFrequency();
            frequencies.add(frequency);
            sumFrequency += frequency;
            if (frequency > maxFrequency) {
                maxFrequency = frequency;
            }
        }
        // Use mean frequency instead of median as per YAKE algorithm
        double meanFrequency = terms.size() > 0 ? (double) sumFrequency / terms.size() : 0;
        double sDeviation = standardDeviation(frequencies.toArray(new Integer[frequencies.size()]));
        for (int i = 0; i < terms.size(); i++) {
            Term term = terms.get(i);
            term.calcFrequency(meanFrequency, sDeviation);
            term.calcDifferent(sentences.size());
            term.calcRelatednes(maxFrequency);
            term.calcTermScore();
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
        double mean = sum / (double) length;
        double deviations = 0.0;
        for (int num : frequencies) {
            deviations += Math.pow(num - mean, 2);
        }
        // Use sample standard deviation (n-1) instead of population (n)
        double variance = length > 1 ? deviations / (length - 1) : 0;
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
                            StringBuilder sb = new StringBuilder();
                            Iterator<Token> it = candidate.iterator();
                            while (it.hasNext()) {
                                sb.append(it.next().getText());
                                sb.append(' ');
                            }
                            String string = sb.toString().strip();
                            if (!candidate.get(0).isStopWord() && !candidate.get(candidate.size() - 1).isStopWord()) {
                                String key = string.toLowerCase();
                                if (!index.containsKey(key)) {
                                    terms.add(new Term(string));
                                    index.put(key, terms.size() - 1);
                                }
                                int idx = index.get(key);
                                Term term = terms.get(idx);
                                if (term.getText().split(" ").length > 1) {
                                    term.increaseFrequency();
                                    term.setSentence(i);  // Track which sentence this multi-word term appears in
                                }
                                // Use actual term frequency for score calculation
                                term.setScore(calcCombinedScore(candidate, term.getTermFrequency()));
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
        terms.removeIf(term -> isStopWord(term.getText()));
        terms.removeIf(term -> term.getTermFrequency() < minFrequency);
        terms.removeIf(term -> isNumber(term.getText()));
        terms.removeIf(term -> term.getText().length() < 2);
    }

    private boolean isNumber(String term) {
        try {
            Double.parseDouble(term);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void deduplicateTerms() {
        int originalCount = terms.size();
        Map<String, Term> normalized = new HashMap<>();
        List<Term> deduplicated = new Vector<>();
        
        // First pass: merge exact case-insensitive duplicates
        for (Term term : terms) {
            String normalizedKey = term.getText().toLowerCase(locale);
            if (normalized.containsKey(normalizedKey)) {
                Term existing = normalized.get(normalizedKey);
                // Keep the term with better score, or if same score, keep the one with higher frequency
                if (term.getScore() < existing.getScore() || 
                    (term.getScore() == existing.getScore() && term.getTermFrequency() > existing.getTermFrequency())) {
                    if (debug) {
                        MessageFormat mf = new MessageFormat(Messages.getString("TermExtractor.10"));
                        logger.log(Level.INFO, mf.format(new String[] { existing.getText(), term.getText() }));
                    }
                    normalized.put(normalizedKey, term);
                }
            } else {
                normalized.put(normalizedKey, term);
            }
        }
        
        // Second pass: merge similar terms using Levenshtein distance
        List<Term> uniqueTerms = new Vector<>(normalized.values());
        boolean[] merged = new boolean[uniqueTerms.size()];
        
        for (int i = 0; i < uniqueTerms.size(); i++) {
            if (merged[i]) {
                continue;
            }
            Term term1 = uniqueTerms.get(i);
            String text1 = term1.getText().toLowerCase(locale);
            Term bestTerm = term1;
            
            // Look for similar terms
            for (int j = i + 1; j < uniqueTerms.size(); j++) {
                if (merged[j]) {
                    continue;
                }
                Term term2 = uniqueTerms.get(j);
                String text2 = term2.getText().toLowerCase(locale);
                
                // Check if terms are similar based on Levenshtein distance
                if (areSimilar(text1, text2)) {
                    merged[j] = true;
                    if (debug) {
                        MessageFormat mf = new MessageFormat(Messages.getString("TermExtractor.11"));
                        logger.log(Level.INFO, mf.format(new String[] { text1, text2 }));
                    }
                    // Keep the term with better score
                    if (term2.getScore() < bestTerm.getScore() ||
                        (term2.getScore() == bestTerm.getScore() && term2.getTermFrequency() > bestTerm.getTermFrequency())) {
                        bestTerm = term2;
                    }
                }
            }
            deduplicated.add(bestTerm);
        }
        
        if (debug) {
            MessageFormat mf = new MessageFormat(Messages.getString("TermExtractor.12"));
            logger.log(Level.INFO, mf.format(new Object[] { originalCount, deduplicated.size(), (originalCount - deduplicated.size()) }));
        }
        
        // Update terms list and index
        terms = deduplicated;
        index.clear();
        for (int i = 0; i < terms.size(); i++) {
            index.put(terms.get(i).getText().toLowerCase(locale), i);
        }
    }
    
    private boolean areSimilar(String text1, String text2) {
        // Same text is not similar, it's identical (already handled)
        if (text1.equals(text2)) {
            return false;
        }
        
        // Only use Levenshtein distance for fuzzy matching (typos, minor variations)
        // Don't merge based on substring relationships - those are different terms
        int distance = LevenshteinDistance.distance(text1, text2);
        int maxLength = Math.max(text1.length(), text2.length());
        
        // Only consider similar if:
        // 1. Length difference is small (no more than 2 characters)
        // 2. Levenshtein distance is very small (1-2 edits for typos)
        // This prevents merging "learning" with "machine learning" 
        if (Math.abs(text1.length() - text2.length()) > 2) {
            return false;
        }
        
        // For very similar lengths, allow only 1-2 character differences (typos)
        // Use 90% similarity threshold - much stricter than before
        double similarity = 1.0 - ((double) distance / maxLength);
        return similarity > 0.90 && distance <= 2;
    }

    private double calcCombinedScore(List<Token> candidateTokens, int termFrequency) {
        double prod = 1;
        double sum = 0;
        // Ensure we don't divide by zero
        if (termFrequency == 0) {
            termFrequency = 1;
        }
        for (int i = 0; i < candidateTokens.size(); i++) {
            Token token = candidateTokens.get(i);
            int idx = index.get(token.getLower());
            Term term = terms.get(idx);
            if (!token.isStopWord()) {
                prod *= term.getScore();
                sum += term.getScore();
            } else {
                double probBefore = 0;
                double probAfter = 0;
                if (i > 0) {
                    Token tokenBefore = candidateTokens.get(i - 1);
                    idx = index.get(tokenBefore.getLower());
                    Term termBefore = terms.get(idx);
                    probBefore = termBefore.getScore();
                }
                if (i < candidateTokens.size() - 1) {
                    Token tokenAfter = candidateTokens.get(i + 1);
                    idx = index.get(tokenAfter.getLower());
                    Term termAfter = terms.get(idx);
                    probAfter = termAfter.getScore();
                }
                double bigramProbability = probBefore * probAfter;
                prod *= 1 + (1 - bigramProbability);
                sum += (1 - bigramProbability);
            }
        }
        return prod / (termFrequency * (sum + 1));
    }
}
