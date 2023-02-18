/*******************************************************************************
 * Copyright (c) 2023 Maxprograms.
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
import java.nio.charset.StandardCharsets;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private ArrayList<String[]> chunks;
    private BreakIterator sentenceIterator;
    private BreakIterator wordsIterator;
    private Locale locale;
    private List<Term> terms;

    public static void main(String[] args) {
        try {
            TermExtractor extractor = new TermExtractor("/Users/rmraya/Desktop/Guia-para-OR-v7.docx.xlf");
            List<Term> list = extractor.getTerms();
            Collections.sort(list);
            try (FileOutputStream out = new FileOutputStream(new File("/Users/rmraya/Desktop/terms.csv"))) {
                for (int i = 0; i < list.size(); i++) {
                    out.write((i + "\t" + list.get(i).getData() + "\n").getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (SAXException | IOException | ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    private List<Term> getTerms() {
        return terms;
    }

    public TermExtractor(String xliffFile) throws IOException, SAXException, ParserConfigurationException {
        sentences = new ArrayList<>();
        chunks = new ArrayList<>();
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(xliffFile);
        Element root = doc.getRootElement();
        if (!("xliff".equals(root.getName()) && root.getAttributeValue("version").startsWith("2."))) {
            throw new IOException("Selected file is not an XLIFF 2.x document");
        }
        srcLang = root.getAttributeValue("srcLang");
        locale = new Locale(srcLang);
        sentenceIterator = BreakIterator.getSentenceInstance(locale);
        wordsIterator = BreakIterator.getWordInstance(locale);
        stopWords = StopWords.getStopWords(srcLang);
        buildSentences(root);
        preProcess();
        termStatistics();
        featureComputation();
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
                    sentences.add(sourceText.substring(start, end));
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
        terms = new ArrayList<>();
        index = new HashMap<>();
        for (int i = 0; i < sentences.size(); i++) {
            String[] array = chunks.get(i);
            for (int j = 0; j < array.length; j++) {
                String chunk = array[j];
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
        List<Integer> frequencies = new ArrayList<>();
        int sum = 0;
        int maxFrequency = 0;
        for (int i = 0; i < terms.size(); i++) {
            int frequency = terms.get(i).getTermFrequency();
            sum += frequency;
            frequencies.add(frequency);
            if (frequency > maxFrequency) {
                maxFrequency = frequency;
            }
        }
        double meanFrequency = sum / frequencies.size();
        double sDeviation = standardDeviation(frequencies.toArray(new Integer[frequencies.size()]));
        for (int i = 0; i < terms.size(); i++) {
            terms.get(i).calcFrequency(meanFrequency, sDeviation);
            terms.get(i).calcDifferent(sentences.size());
            terms.get(i).calcRelatednes(maxFrequency);
            terms.get(i).calcScore();
        }
    }

    private String[] getChunks(String sentence) {
        List<String> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sentence.length(); i++) {
            char c = sentence.charAt(i);
            if (!isPunctuation(c)) {
                sb.append(c);
            } else {
                if (!sb.isEmpty()) {
                    list.add(sb.toString());
                    sb = new StringBuilder();
                }
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
        List<String> words = new ArrayList<>();
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
        List<Token> tokens = new ArrayList<>();
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
}
