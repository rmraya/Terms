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

import java.util.Locale;

public class Token {

    public static final String NUMBER = "d"; // Digit or number
    public static final String UNPARSABLE = "u"; // unparsable content
    public static final String ACRONYM = "a"; // Acronym
    public static final String UPPERCASE = "U"; // Uppercase
    public static final String PARSABLE = "p"; // Parsable content

    String token;
    String lower;
    String tag;
    boolean stopWord;

    public Token(String token, boolean stopWord, Locale locale, boolean beginsSentence) {
        this.token = token;
        lower = token.toLowerCase(locale);
        tag = getType(beginsSentence);
        this.stopWord = stopWord;
    }

    public String getToken() {
        return token;
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
        if (isNumber(token)) {
            return NUMBER;
        }
        if (isAcronym(token)) {
            return ACRONYM;
        }
        if (isUnparsable(token)) {
            return UNPARSABLE;
        }
        if (!beginsSentence && Character.isUpperCase(token.charAt(0))) {
            return UPPERCASE;
        }
        return PARSABLE;
    }

    private boolean isAcronym(String string) {
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (!Character.isUpperCase(c)) {
                return false;
            }
        }
        return true;
    }

    private boolean isUnparsable(String string) {
        int digits = 0;
        int letters = 0;
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
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

    private static boolean isNumber(String string) {
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (!(Character.isDigit(c) || c == '.' || c != ',')) {
                return false;
            }
        }
        return true;
    }
}
