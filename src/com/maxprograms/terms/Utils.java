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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.maxprograms.xml.Element;
import com.maxprograms.xml.TextNode;
import com.maxprograms.xml.XMLNode;

public class Utils {

    private Utils() {
        // do not instantiate
    }

    public static String[] fixPath(String[] args) {
		List<String> result = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.startsWith("-")) {
				if (!current.isEmpty()) {
					result.add(current.toString().trim());
					current = new StringBuilder();
				}
				result.add(arg);
			} else {
				current.append(' ');
				current.append(arg);
			}
		}
		if (!current.isEmpty()) {
			result.add(current.toString().trim());
		}
		return result.toArray(new String[result.size()]);
	}

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

    public static double median(List<Integer> list) {
        Collections.sort(list);
        int length = list.size();
        if (length % 2 == 0) {
            // If even, average the two middle elements
            return (list.get(length / 2 - 1) + list.get(length / 2)) / 2.0;
        } else {
            // If odd, return the middle element
            return (double) list.get(length / 2);
        }
    }
}
