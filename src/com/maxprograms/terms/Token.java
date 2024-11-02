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

import java.util.Locale;

public class Token {

    public static final String NUMBER = "d"; // Digit or number
    public static final String UNPARSABLE = "u"; // unparsable content
    public static final String ACRONYM = "a"; // Acronym
    public static final String UPPERCASE = "U"; // Uppercase
    public static final String PARSABLE = "p"; // Parsable content

    String text;
    String lower;
    String tag;
    boolean stopWord;

    public Token(String token, boolean stopWord, Locale locale, boolean beginsSentence) {
        text = token;
        lower = token.toLowerCase(locale);
        tag = getType(beginsSentence);
        this.stopWord = stopWord;
    }

    public String getText() {
        return text;
    }

    public String getLower() {
        return lower;
    }

    public String getTag() {
        return tag;
    }

    public boolean isStopWord() {
        return stopWord;
    }

    private String getType(boolean beginsSentence) {
        if (isNumber()) {
            return NUMBER;
        }
        if (isAcronym()) {
            return ACRONYM;
        }
        if (isUnparsable()) {
            return UNPARSABLE;
        }
        if (!beginsSentence && Character.isUpperCase(text.charAt(0))) {
            return UPPERCASE;
        }
        return PARSABLE;
    }

    private boolean isAcronym() {
        if (text.length() < 2) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!Character.isUpperCase(c)) {
                return false;
            }
        }
        return true;
    }

    private boolean isUnparsable() {
        int digits = 0;
        int letters = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!(Character.isDigit(c) || Character.isLetter(c))) {
                return true;
            }
            if (Character.isDigit(c)) {
                digits++;
            }
            if (Character.isLetter(c)) {
                letters++;
            }
            if (digits > 0 && letters > 0) {
                return true;
            }
        }
        return false;
    }

    private  boolean isNumber() {
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public boolean isRelatable() {
        return tag.equals(ACRONYM) || tag.equals(PARSABLE) || tag.equals(UPPERCASE);
    }
}
