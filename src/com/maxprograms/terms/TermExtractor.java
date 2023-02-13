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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.SAXBuilder;

import org.xml.sax.SAXException;

public class TermExtractor {

    private Map<String, List<Integer>> sourceMap;
    private List<String> sourceStopWords;
    private String srcLang;
    private int segId;

    public static void main(String[] args) {
        try {
            TermExtractor instance = new TermExtractor(
                    "/Users/rmraya/Samples/Patricia/81268977_03b_Probeübersetzung Ausgangstext (Los 4 EN-FR).docx.xlf");
            List<Candidate> candidates = instance.getCandidates();
            for (int i = 0; i < candidates.size(); i++) {
                Candidate c = candidates.get(i);
                if (c.getFrequency() > 2) {
                    System.out.println(c.getTerm() + "   =>  " + c.getFrequency() + "   =>  " + c.getScore());
                }
            }
        } catch (SAXException | IOException | ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    public TermExtractor(String xliffFile) throws IOException, SAXException, ParserConfigurationException {
        sourceMap = new HashMap<>();

        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(xliffFile);
        Element root = doc.getRootElement();
        if (!("xliff".equals(root.getName()) && root.getAttributeValue("version").startsWith("2."))) {
            throw new IOException("Selected file is not an XLIFF 2.x document");
        }
        srcLang = root.getAttributeValue("srcLang");
        sourceStopWords = StopWords.getStopWords(srcLang);
        buildMaps(root);
    }

    private void buildMaps(Element e) {
        if ("segment".equals(e.getName())) {
            Element source = e.getChild("source");
            String sourceText = Utils.pureText(source);
            if (!sourceText.isBlank()) {
                List<String> sourceList = Utils.buildTermsList(sourceText, sourceStopWords);
                for (int i = 0; i < sourceList.size(); i++) {
                    String term = sourceList.get(i);
                    if (sourceMap.containsKey(term)) {
                        List<Integer> list = sourceMap.get(term);
                        list.add(segId);
                        sourceMap.put(term, list);
                    } else {
                        List<Integer> list = new ArrayList<>();
                        list.add(segId);
                        sourceMap.put(term, list);
                    }
                }
                segId++;
            }
        } else {
            List<Element> children = e.getChildren();
            Iterator<Element> it = children.iterator();
            while (it.hasNext()) {
                buildMaps(it.next());
            }
        }
    }

    public List<Candidate> getCandidates() {
        List<Candidate> result = new ArrayList<>();
        Set<String> keys = sourceMap.keySet();
        Iterator<String> it = keys.iterator();
        while (it.hasNext()) {
            String term = it.next();
            List<Integer> count = sourceMap.get(term);
            int termFrequency = count.size();
            double inverseTermFrequncy = Math.log(segId / termFrequency);
            double score = termFrequency * inverseTermFrequncy;
            result.add(new Candidate(term, termFrequency, score));
        }
        Collections.sort(result);
        return result;
    }
}
