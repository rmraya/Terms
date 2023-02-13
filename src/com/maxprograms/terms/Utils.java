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

import java.util.Iterator;
import java.util.List;

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

}
