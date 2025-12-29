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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.XMLOutputter;

public class BilingualExtraction {

    private static Logger logger = System.getLogger(BilingualExtraction.class.getName());
    private static boolean debug = false;

    static class TermPair {
        Term sourceTerm;
        Term targetTerm;
        Set<Integer> sharedSegments;

        TermPair(Term source, Term target, Set<Integer> segments) {
            this.sourceTerm = source;
            this.targetTerm = target;
            this.sharedSegments = segments;
        }

        int getCoOccurrenceCount() {
            return sharedSegments.size();
        }
    }

    public static void main(String[] args) {
        args = Utils.fixPath(args);

        String xliff = "";
        String output = "";
        int minFrequency = 3;
        double maxScore = 10.0;
        int maxTermLength = 5;
        int minCoOccurrence = 1;
        int maxPairs = 0; // 0 means no limit
        double minCoOccurrenceRatio = 0.7; // 70% minimum ratio

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
                if ("-maxLength".equals(args[i]) && i + 1 < args.length) {
                    maxTermLength = Integer.parseInt(args[i + 1]);
                }
                if ("-minCoOccurrence".equals(args[i]) && i + 1 < args.length) {
                    minCoOccurrence = Integer.parseInt(args[i + 1]);
                }
                if ("-maxPairs".equals(args[i]) && i + 1 < args.length) {
                    maxPairs = Integer.parseInt(args[i + 1]);
                }
                if ("-minCoOccurrenceRatio".equals(args[i]) && i + 1 < args.length) {
                    minCoOccurrenceRatio = Double.parseDouble(args[i + 1]);
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
                if ("-lang".equals(args[i]) && i + 1 < args.length) {
                    String lang = args[i + 1];
                    if ("en".equals(lang) || "es".equals(lang) ) {
                        Locale.setDefault(Locale.forLanguageTag(lang));
                    }
                }
            }

            if (xliff.isEmpty()) {
                usage();
                System.exit(1);
            }

            // Validate parameters
            if (minFrequency < 1) {
                logger.log(Level.ERROR, Messages.getString("BilingualExtraction.1"));
                System.exit(1);
            }
            if (maxScore <= 0) {
                logger.log(Level.ERROR, Messages.getString("BilingualExtraction.2"));
                System.exit(1);
            }
            if (maxTermLength < 1) {
                logger.log(Level.ERROR, Messages.getString("BilingualExtraction.3"));
                System.exit(1);
            }
            if (minCoOccurrence < 1) {
                logger.log(Level.ERROR, Messages.getString("BilingualExtraction.4"));
                System.exit(1);
            }
            if (maxPairs < 0) {
                logger.log(Level.ERROR, Messages.getString("BilingualExtraction.5"));
                System.exit(1);
            }
            if (minCoOccurrenceRatio < 0.0 || minCoOccurrenceRatio > 1.0) {
                logger.log(Level.ERROR, Messages.getString("BilingualExtraction.6"));
                System.exit(1);
            }

            File xliffFile = new File(xliff);
            if (!xliffFile.exists()) {
                MessageFormat mf = new MessageFormat(Messages.getString("BilingualExtraction.7"));
                logger.log(Level.ERROR, mf.format(new String[] { xliff }));
                System.exit(1);
            }

            if (output.isEmpty()) {
                File file = new File(xliff);
                String path = file.getAbsolutePath();
                int index = path.lastIndexOf('.');
                if (index != -1) {
                    output = path.substring(0, index) + "_bilingual.csv";
                } else {
                    output = path + "_bilingual.csv";
                }
            }

            BilingualExtraction extractor = new BilingualExtraction();
            extractor.extract(xliff, output, minFrequency, maxScore, maxTermLength,
                    minCoOccurrence, maxPairs, minCoOccurrenceRatio);

            if (debug) {
                MessageFormat mf = new MessageFormat(Messages.getString("BilingualExtraction.8"));
                logger.log(Level.INFO, mf.format(new String[] { output }));
            }

        } catch (Exception e) {
            logger.log(Level.ERROR, Messages.getString("BilingualExtraction.9"), e);
            if (debug) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }

    private static void usage() {
        String msg = Messages.getString("BilingualExtraction.help");
        logger.log(Level.INFO, msg);
    }

    public void extract(String xliffFile, String outputFile, int minFrequency, double maxScore,
            int maxTermLength, int minCoOccurrence, int maxPairs, double minCoOccurrenceRatio)
            throws SAXException, IOException, ParserConfigurationException {

        // Step 1: Parse XLIFF and get languages
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(xliffFile);
        Element root = doc.getRootElement();

        String srcLang = root.getAttributeValue("srcLang", "en");
        String trgLang = root.getAttributeValue("trgLang", "en");

        if (debug) {
            MessageFormat mf = new MessageFormat(Messages.getString("BilingualExtraction.10"));
            logger.log(Level.INFO, mf.format(new String[] { srcLang, trgLang }));
        }

        // Step 2: Create temporary XLIFF files for source and target
        File tempSourceXliff = File.createTempFile("bilingual_source_", ".xlf");
        File tempTargetXliff = File.createTempFile("bilingual_target_", ".xlf");
        tempSourceXliff.deleteOnExit();
        tempTargetXliff.deleteOnExit();

        int segmentCount = createTempXliffFiles(xliffFile, srcLang, trgLang,
                tempSourceXliff.getAbsolutePath(),
                tempTargetXliff.getAbsolutePath());

        if (segmentCount == 0) {
            logger.log(Level.WARNING, Messages.getString("BilingualExtraction.11"));
            return;
        }

        if (debug) {
            MessageFormat mf = new MessageFormat(Messages.getString("BilingualExtraction.12"));
            logger.log(Level.INFO, mf.format(new Object[] { segmentCount }));
        }

        // Step 3: Extract terms from source using TermExtractor
        TermExtractor sourceExtractor = new TermExtractor(tempSourceXliff.getAbsolutePath(),
                maxTermLength, minFrequency, maxScore, false);
        List<Term> sourceTerms = sourceExtractor.getTerms();
        List<Integer> sourceSentenceToSegment = sourceExtractor.getSentenceToSegmentMap();

        if (debug) {
            MessageFormat mf = new MessageFormat(Messages.getString("BilingualExtraction.13"));
            logger.log(Level.INFO, mf.format(new Object[] { sourceTerms.size() }));
        }

        // Step 4: Extract terms from target using TermExtractor
        TermExtractor targetExtractor = new TermExtractor(tempTargetXliff.getAbsolutePath(),
                maxTermLength, minFrequency, maxScore, false);
        List<Term> targetTerms = targetExtractor.getTerms();
        List<Integer> targetSentenceToSegment = targetExtractor.getSentenceToSegmentMap();

        if (debug) {
            MessageFormat mf = new MessageFormat(Messages.getString("BilingualExtraction.14"));
            logger.log(Level.INFO, mf.format(new Object[] { targetTerms.size() }));
        }

        // Step 5: Build segment-to-term mapping using sentence indices
        Map<String, Set<Integer>> sourceTermSegments = buildTermSegmentMapFromSentences(sourceTerms,
                sourceSentenceToSegment);
        Map<String, Set<Integer>> targetTermSegments = buildTermSegmentMapFromSentences(targetTerms,
                targetSentenceToSegment);

        // Step 6: Generate co-occurring pairs
        List<TermPair> pairs = generatePairs(sourceTerms, targetTerms, sourceTermSegments, targetTermSegments);

        // Step 7: Apply mutual best match filtering to reduce garbage pairs
        pairs = filterMutualBestMatch(pairs);

        // Step 8: Apply co-occurrence filters
        pairs = filterPairs(pairs, minCoOccurrence, maxPairs, minCoOccurrenceRatio);

        // Step 9: Deduplicate pairs (keep best terms based on YAKE score)
        pairs = deduplicatePairs(pairs);

        if (debug) {
            MessageFormat mf = new MessageFormat(Messages.getString("BilingualExtraction.15"));
            logger.log(Level.INFO, mf.format(new Object[] { pairs.size() }));
        }

        // Step 9: Write CSV output
        writeCSV(outputFile, pairs);
    }

    private int createTempXliffFiles(String xliffFile, String srcLang, String trgLang,
            String sourceXliffPath, String targetXliffPath)
            throws SAXException, IOException, ParserConfigurationException {

        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(xliffFile);
        Element root = doc.getRootElement();

        // Create both documents simultaneously to ensure matching segment numbers
        Document sourceDoc = createEmptyXliff(srcLang);
        Document targetDoc = createEmptyXliff(trgLang);

        Element sourceFile = sourceDoc.getRootElement().getChild("file");
        Element targetFile = targetDoc.getRootElement().getChild("file");

        Locale srcLocale = Locale.forLanguageTag(srcLang);
        Locale trgLocale = Locale.forLanguageTag(trgLang);

        // Process both source and target together
        int segmentCount = collectFinalSegmentPairs(root, sourceFile, targetFile, srcLocale, trgLocale);

        writeXmlDocument(sourceDoc, sourceXliffPath);
        writeXmlDocument(targetDoc, targetXliffPath);

        return segmentCount;
    }

    private Document createEmptyXliff(String language) {
        Element xliff = new Element("xliff");
        xliff.setAttribute("version", "2.1");
        xliff.setAttribute("srcLang", language);
        xliff.setAttribute("xmlns", "urn:oasis:names:tc:xliff:document:2.0");

        Element file = new Element("file");
        file.setAttribute("id", "f1");
        xliff.addContent(file);

        Document doc = new Document(null, "xliff", null);
        doc.setRootElement(xliff);
        return doc;
    }

    private int collectFinalSegmentPairs(Element element, Element sourceFile, Element targetFile,
            Locale srcLocale, Locale trgLocale) {

        if ("segment".equals(element.getName())) {
            String state = element.getAttributeValue("state", "");
            if ("final".equals(state)) {
                Element sourceElem = element.getChild("source");
                Element targetElem = element.getChild("target");

                if (sourceElem != null && targetElem != null) {
                    String sourceText = Utils.pureText(sourceElem);
                    String targetText = Utils.pureText(targetElem);

                    if (!sourceText.isBlank() && !targetText.isBlank()) {
                        int segmentNumber = sourceFile.getChildren().size();

                        // Create source segment with full text
                        Element srcSeg = new Element("segment");
                        srcSeg.setAttribute("id", String.valueOf(segmentNumber));
                        Element src = new Element("source");
                        src.addContent(sourceText);
                        srcSeg.addContent(src);
                        sourceFile.addContent(srcSeg);

                        // Create target segment with SAME number
                        Element tgtSeg = new Element("segment");
                        tgtSeg.setAttribute("id", String.valueOf(segmentNumber));
                        Element tgt = new Element("source"); // target becomes source for extraction
                        tgt.addContent(targetText);
                        tgtSeg.addContent(tgt);
                        targetFile.addContent(tgtSeg);
                    }
                }
            }
        }

        // Recurse through children
        List<Element> children = element.getChildren();
        for (Element child : children) {
            collectFinalSegmentPairs(child, sourceFile, targetFile, srcLocale, trgLocale);
        }

        return sourceFile.getChildren().size();
    }

    private void writeXmlDocument(Document doc, String filePath) throws IOException {
        XMLOutputter outputter = new XMLOutputter();
        outputter.preserveSpace(true);
        try (FileOutputStream out = new FileOutputStream(filePath)) {
            outputter.output(doc, out);
        }
    }

    private Map<String, Set<Integer>> buildTermSegmentMapFromSentences(List<Term> terms,
            List<Integer> sentenceToSegment) {
        Map<String, Set<Integer>> termSegments = new HashMap<>();

        if (debug) {
            MessageFormat mf = new MessageFormat(Messages.getString("BilingualExtraction.16"));
            logger.log(Level.INFO, mf.format(new Object[] { terms.size(), sentenceToSegment.size() }));
        }

        int termsWithSegments = 0;
        int termsWithNoSentences = 0;
        int termsWithOutOfBoundsSentences = 0;

        for (Term term : terms) {
            Set<Integer> segments = new HashSet<>();
            Vector<Integer> sentenceIndices = term.getOffsetSentences();

            if (sentenceIndices.isEmpty()) {
                termsWithNoSentences++;
                if (debug && termsWithNoSentences <= 3) {
                    MessageFormat mf = new MessageFormat(Messages.getString("BilingualExtraction.17"));
                    logger.log(Level.INFO, mf.format(new String[] { term.getText() }));
                }
            }

            // Map sentence indices to segment numbers
            for (Integer sentenceIndex : sentenceIndices) {
                if (sentenceIndex < sentenceToSegment.size()) {
                    segments.add(sentenceToSegment.get(sentenceIndex));
                } else {
                    termsWithOutOfBoundsSentences++;
                    if (debug && termsWithOutOfBoundsSentences <= 3) {
                        MessageFormat mf = new MessageFormat(Messages.getString("BilingualExtraction.18"));
                        logger.log(Level.INFO, mf.format(new Object[] { term.getText(), sentenceIndex, sentenceToSegment.size() - 1 }));
                    }
                }
            }

            if (!segments.isEmpty()) {
                termsWithSegments++;
                if (debug && termsWithSegments <= 3) {
                    MessageFormat mf = new MessageFormat(Messages.getString("BilingualExtraction.19"));
                    logger.log(Level.INFO, mf.format(new Object[] { term.getText(), segments.size() }));
                }
            }

            termSegments.put(term.getText(), segments);
        }

        if (debug) {
            MessageFormat mf = new MessageFormat(Messages.getString("BilingualExtraction.20"));
            logger.log(Level.INFO, mf.format(new Object[] { termsWithSegments, termsWithNoSentences, termsWithOutOfBoundsSentences }));
        }
        return termSegments;
    }

    private List<TermPair> generatePairs(List<Term> sourceTerms, List<Term> targetTerms,
            Map<String, Set<Integer>> sourceTermSegments, Map<String, Set<Integer>> targetTermSegments) {

        List<TermPair> pairs = new ArrayList<>();
        if (debug) {
            MessageFormat mf = new MessageFormat(Messages.getString("BilingualExtraction.21"));
            logger.log(Level.INFO, mf.format(new Object[] { sourceTerms.size(), targetTerms.size() }));
        }

        int pairsFound = 0;
        for (Term sourceTerm : sourceTerms) {
            Set<Integer> sourceSegs = sourceTermSegments.get(sourceTerm.getText());
            if (sourceSegs == null || sourceSegs.isEmpty()) {
                continue;
            }

            for (Term targetTerm : targetTerms) {
                Set<Integer> targetSegs = targetTermSegments.get(targetTerm.getText());
                if (targetSegs == null || targetSegs.isEmpty()) {
                    continue;
                }

                // Find intersection
                Set<Integer> sharedSegments = new HashSet<>(sourceSegs);
                sharedSegments.retainAll(targetSegs);

                if (!sharedSegments.isEmpty()) {
                    pairs.add(new TermPair(sourceTerm, targetTerm, sharedSegments));
                    pairsFound++;
                    if (debug && pairsFound <= 5) {
                        MessageFormat mf = new MessageFormat(Messages.getString("BilingualExtraction.22"));
                        logger.log(Level.INFO, mf.format(new Object[] { sourceTerm.getText(), targetTerm.getText(), sharedSegments.size() }));
                    }
                }
            }
        }

        if (debug) {
            MessageFormat mf = new MessageFormat(Messages.getString("BilingualExtraction.23"));
            logger.log(Level.INFO, mf.format(new Object[] { pairsFound }));
        }
        return pairs;
    }

    private List<TermPair> filterMutualBestMatch(List<TermPair> pairs) {
        // Build maps of best matches in each direction
        Map<String, TermPair> bestTargetForSource = new HashMap<>();
        Map<String, TermPair> bestSourceForTarget = new HashMap<>();

        // Find best target for each source term (highest co-occurrence count)
        for (TermPair pair : pairs) {
            String sourceText = pair.sourceTerm.getText();
            TermPair existing = bestTargetForSource.get(sourceText);
            if (existing == null || pair.getCoOccurrenceCount() > existing.getCoOccurrenceCount()) {
                bestTargetForSource.put(sourceText, pair);
            }
        }

        // Find best source for each target term (highest co-occurrence count)
        for (TermPair pair : pairs) {
            String targetText = pair.targetTerm.getText();
            TermPair existing = bestSourceForTarget.get(targetText);
            if (existing == null || pair.getCoOccurrenceCount() > existing.getCoOccurrenceCount()) {
                bestSourceForTarget.put(targetText, pair);
            }
        }

        // Keep only mutual best matches
        List<TermPair> filtered = new ArrayList<>();
        for (TermPair pair : pairs) {
            String sourceText = pair.sourceTerm.getText();
            String targetText = pair.targetTerm.getText();

            TermPair bestTarget = bestTargetForSource.get(sourceText);
            TermPair bestSource = bestSourceForTarget.get(targetText);

            // Mutual best match: this pair is best for source AND best for target
            if (bestTarget == pair && bestSource == pair) {
                filtered.add(pair);
            }
        }

        if (debug) {
            MessageFormat mf = new MessageFormat(Messages.getString("BilingualExtraction.24"));
            logger.log(Level.INFO, mf.format(new Object[] { pairs.size(), filtered.size() }));
        }

        return filtered;
    }

    private List<TermPair> filterPairs(List<TermPair> pairs, int minCoOccurrence, int maxPairs,
            double minCoOccurrenceRatio) {
        // Filter by minimum co-occurrence and ratio (check both directions)
        List<TermPair> filtered = pairs.stream()
                .filter(pair -> {
                    if (pair.getCoOccurrenceCount() < minCoOccurrence) {
                        return false;
                    }
                    // Calculate co-occurrence ratio from both source and target perspectives
                    int sourceFreq = pair.sourceTerm.getTermFrequency();
                    int targetFreq = pair.targetTerm.getTermFrequency();
                    double sourceRatio = (double) pair.getCoOccurrenceCount() / sourceFreq;
                    double targetRatio = (double) pair.getCoOccurrenceCount() / targetFreq;
                    // Both ratios must meet the threshold
                    return sourceRatio >= minCoOccurrenceRatio && targetRatio >= minCoOccurrenceRatio;
                })
                .collect(Collectors.toList());

        // Sort by co-occurrence count (descending)
        filtered.sort((a, b) -> Integer.compare(b.getCoOccurrenceCount(), a.getCoOccurrenceCount()));

        // Apply maxPairs limit symmetrically - both per source term AND per target term
        if (maxPairs > 0) {
            // First: limit target terms per source term
            Map<String, List<TermPair>> bySourceTerm = new HashMap<>();
            for (TermPair pair : filtered) {
                String sourceTerm = pair.sourceTerm.getText();
                bySourceTerm.computeIfAbsent(sourceTerm, k -> new ArrayList<>()).add(pair);
            }

            List<TermPair> limitedBySource = new ArrayList<>();
            for (List<TermPair> termPairs : bySourceTerm.values()) {
                int count = Math.min(maxPairs, termPairs.size());
                limitedBySource.addAll(termPairs.subList(0, count));
            }

            // Second: limit source terms per target term
            Map<String, List<TermPair>> byTargetTerm = new HashMap<>();
            for (TermPair pair : limitedBySource) {
                String targetTerm = pair.targetTerm.getText();
                byTargetTerm.computeIfAbsent(targetTerm, k -> new ArrayList<>()).add(pair);
            }

            List<TermPair> limitedByTarget = new ArrayList<>();
            for (List<TermPair> termPairs : byTargetTerm.values()) {
                int count = Math.min(maxPairs, termPairs.size());
                limitedByTarget.addAll(termPairs.subList(0, count));
            }

            filtered = limitedByTarget;
        }

        return filtered;
    }

    private List<TermPair> deduplicatePairs(List<TermPair> pairs) {
        // First pass: deduplicate by source term + segments (remove shorter targets)
        Map<String, List<TermPair>> bySource = new HashMap<>();
        for (TermPair pair : pairs) {
            String key = pair.sourceTerm.getText() + "|" + pair.sharedSegments.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            bySource.computeIfAbsent(key, k -> new ArrayList<>()).add(pair);
        }

        List<TermPair> afterSourceDedup = new ArrayList<>();
        for (List<TermPair> group : bySource.values()) {
            afterSourceDedup.addAll(deduplicateBySubstring(group, false)); // false = check targets
        }

        // Second pass: deduplicate by target term + segments (remove shorter sources)
        Map<String, List<TermPair>> byTarget = new HashMap<>();
        for (TermPair pair : afterSourceDedup) {
            String key = pair.targetTerm.getText() + "|" + pair.sharedSegments.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            byTarget.computeIfAbsent(key, k -> new ArrayList<>()).add(pair);
        }

        List<TermPair> deduplicated = new ArrayList<>();
        for (List<TermPair> group : byTarget.values()) {
            deduplicated.addAll(deduplicateBySubstring(group, true)); // true = check sources
        }

        return deduplicated;
    }

    private List<TermPair> deduplicateBySubstring(List<TermPair> group, boolean checkSource) {
        if (group.size() == 1) {
            return group;
        }

        List<TermPair> result = new ArrayList<>();
        boolean[] kept = new boolean[group.size()];
        for (int i = 0; i < group.size(); i++) {
            kept[i] = true;
        }

        // First pass: remove substring relationships
        for (int i = 0; i < group.size(); i++) {
            if (!kept[i])
                continue;
            String term1 = checkSource ? group.get(i).sourceTerm.getText().toLowerCase()
                    : group.get(i).targetTerm.getText().toLowerCase();

            for (int j = 0; j < group.size(); j++) {
                if (i == j || !kept[j])
                    continue;
                String term2 = checkSource ? group.get(j).sourceTerm.getText().toLowerCase()
                        : group.get(j).targetTerm.getText().toLowerCase();

                // If term1 is a substring of term2, remove term1 (keep longer)
                if (term2.contains(term1) && !term1.equals(term2)) {
                    kept[i] = false;
                    break;
                }
                // If term2 is a substring of term1, remove term2 (keep longer)
                if (term1.contains(term2) && !term1.equals(term2)) {
                    kept[j] = false;
                }
            }
        }

        // Second pass: among remaining terms, keep only the longest (most words)
        List<TermPair> remaining = new ArrayList<>();
        for (int i = 0; i < group.size(); i++) {
            if (kept[i]) {
                remaining.add(group.get(i));
            }
        }

        if (remaining.size() > 1) {
            // Find the pair with the best term (lowest YAKE score = highest quality)
            TermPair best = remaining.get(0);
            double bestScore = checkSource ? best.sourceTerm.getScore() : best.targetTerm.getScore();

            for (int i = 1; i < remaining.size(); i++) {
                TermPair candidate = remaining.get(i);
                double candidateScore = checkSource ? candidate.sourceTerm.getScore() : candidate.targetTerm.getScore();

                if (candidateScore < bestScore) {
                    best = candidate;
                    bestScore = candidateScore;
                }
            }

            result.add(best);
        } else {
            result.addAll(remaining);
        }

        return result;
    }

    private void writeCSV(String outputFile, List<TermPair> pairs) throws IOException {
        try (FileOutputStream output = new FileOutputStream(outputFile)) {
            // Write BOM for UTF-16LE
            output.write(0xFF);
            output.write(0xFE);

            // Write header
            String header = "SourceTerm,SourceScore,SourceFreq,TargetTerm,TargetScore,TargetFreq,SharedSegments,CoOccurrenceCount\n";
            output.write(header.getBytes(StandardCharsets.UTF_16LE));

            // Write data rows
            for (TermPair pair : pairs) {
                String segmentList = pair.sharedSegments.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining("|"));
                String row = String.format("%s,%.6f,%d,%s,%.6f,%d,\"%s\",%d\n",
                        escapeCsv(pair.sourceTerm.getText()),
                        pair.sourceTerm.getScore(),
                        pair.sourceTerm.getTermFrequency(),
                        escapeCsv(pair.targetTerm.getText()),
                        pair.targetTerm.getScore(),
                        pair.targetTerm.getTermFrequency(),
                        segmentList,
                        pair.getCoOccurrenceCount());

                output.write(row.getBytes(StandardCharsets.UTF_16LE));
            }
        }
    }

    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
