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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import com.maxprograms.xml.Element;
import com.maxprograms.xml.TextNode;
import com.maxprograms.xml.XMLNode;

public class Utils {

    public static final String TERM_SEPARATORS = " \u00A0\r\n\f\t\u2028\u2029,.;\":<>¿?¡!()[]{}=+/*\u00AB\u00BB\u201C\u201D\u201E\uFF00";
    public static final int MAXTERMLENGTH = 5;

    public static String pureText(Element e) {
        StringBuilder sb = new StringBuilder();
        List<XMLNode> content = e.getContent();
        Iterator<XMLNode> it = content.iterator();
        while (it.hasNext()) {
            XMLNode node = it.next();
            if (node.getNodeType() == XMLNode.TEXT_NODE) {
                sb.append(((TextNode) node).getText());
            }
            if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
                Element el = (Element) node;
                if ("pc".equals(el.getName()) || "mrk".equals(el.getName())) {
                    sb.append(pureText(el));
                }
            }
        }
        return sb.toString();
    }

    public static List<String> buildTermsList(String string, List<String> stopWords) {
        List<String> result = new ArrayList<>();
        List<String> words = buildWordList(string);
        for (int i = 0; i < words.size(); i++) {
            StringBuilder termBuilder = new StringBuilder();
            for (int length = 0; length < MAXTERMLENGTH; length++) {
                if (i + length < words.size()) {
                    String word = words.get(i + length);
                    termBuilder.append(word);
                    String term = termBuilder.toString().trim();
                    if (term.isBlank()) {
                        continue;
                    }
                    String[] parts = term.split(" ");
                    if (stopWords.contains(parts[0]) || stopWords.contains(parts[parts.length - 1])) {
                        continue;
                    }
                    char start = term.charAt(0);
                    if (TERM_SEPARATORS.indexOf(start) != -1) {
                        continue;
                    }
                    char end = term.charAt(term.length() - 1);
                    if (TERM_SEPARATORS.indexOf(end) != -1) {
                        continue;
                    }
                    if (!result.contains(term) && !isNumber(term)) {
                        result.add(term);
                    }
                }
            }
        }
        return result;
    }

    private static List<String> buildWordList(String string) {
        List<String> result = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(string, TERM_SEPARATORS, true);
        while (tokenizer.hasMoreElements()) {
            result.add(tokenizer.nextToken());
        }
        return result;
    }

    private static boolean isNumber(String string) {
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (!(c >= '0' && c <= '9') && c != '.') {
                return false;
            }
        }
        return true;
    }
}
