/*****************************************************************************
Copyright (c) 2020 - Maxprograms,  http://www.maxprograms.com/

Permission is hereby granted, free of charge, to any person obtaining a copy of 
this software and associated documentation files (the "Software"), to compile, 
modify and use the Software in its executable form without restrictions.

Redistribution of this Software or parts of it in any form (source code or 
executable binaries) requires prior written permission from Maxprograms.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE 
SOFTWARE.
*****************************************************************************/

package com.maxprograms.terms;

import java.io.IOException;
import java.util.ArrayList;
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

public class BilingualTermExtractor {

    private Map<String, List<Integer>> sourceMap;
    private Map<String, List<Integer>> targetMap;
    private List<String> sourceStopWords;
    private List<String> targetStopWords;
    private String srcLang;
    private String tgtLang;
    private int segId;

    public static void main(String[] args) {
        try {
            BilingualTermExtractor instance = new BilingualTermExtractor(
                    "/Users/rmraya/Samples/Patricia/81268977_03b_Probeübersetzung Ausgangstext (Los 4 EN-FR).docx.xlf");
            instance.matchMaps();
        } catch (SAXException | IOException | ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    public BilingualTermExtractor(String xliffFile) throws SAXException, IOException, ParserConfigurationException {
        sourceMap = new HashMap<>();
        targetMap = new HashMap<>();
        segId = 0;

        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(xliffFile);
        Element root = doc.getRootElement();
        if (!("xliff".equals(root.getName()) && root.getAttributeValue("version").startsWith("2."))) {
            throw new IOException("Selected file is not an XLIFF 2.x document");
        }
        srcLang = root.getAttributeValue("srcLang");
        sourceStopWords = StopWords.getStopWords(srcLang);
        tgtLang = root.getAttributeValue("trgLang");
        targetStopWords = StopWords.getStopWords(tgtLang);
        buildMaps(root);
    }

    private void buildMaps(Element e) {
        if ("segment".equals(e.getName())) {
            Element source = e.getChild("source");
            Element target = e.getChild("target");
            if (source != null && target != null) {
                String sourceText = Utils.pureText(source);
                String targetText = Utils.pureText(target);
                if (!sourceText.isBlank() && !targetText.isBlank()) {
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
                    List<String> targetList = Utils.buildTermsList(targetText, targetStopWords);
                    for (int i = 0; i < targetList.size(); i++) {
                        String term = targetList.get(i);
                        if (targetMap.containsKey(term)) {
                            List<Integer> list = targetMap.get(term);
                            list.add(segId);
                            targetMap.put(term, list);
                        } else {
                            List<Integer> list = new ArrayList<>();
                            list.add(segId);
                            targetMap.put(term, list);
                        }
                    }
                    segId++;
                }
            }
        } else {
            List<Element> children = e.getChildren();
            Iterator<Element> it = children.iterator();
            while (it.hasNext()) {
                buildMaps(it.next());
            }
        }
    }

    public void matchMaps() {
        Set<String> keys = sourceMap.keySet();
        Iterator<String> it = keys.iterator();
        while (it.hasNext()) {
            String key = it.next();
            List<Integer> list = sourceMap.get(key);
            if (list.size() > 1) {
                if (targetMap.containsValue(list)) {
                    System.out.println(key + ": ");
                    Set<String> targetKeys = targetMap.keySet();
                    Iterator<String> tt = targetKeys.iterator();
                    while (tt.hasNext()) {
                        String targetKey = tt.next();
                        List<Integer> targetList = targetMap.get(targetKey);
                        if (targetList.equals(list)) {
                            System.out.println("   " + targetKey);
                        }
                    }
                }
            }
        }
    }
}
