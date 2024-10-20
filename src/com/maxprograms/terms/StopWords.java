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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

public class StopWords {

    private static JSONObject wordsList;

    private StopWords() {
        // private for security
    }

    public static List<String> getStopWords(String language) throws IOException {
        List<String> result = new ArrayList<>();
        if (wordsList == null) {
            loadWordList();
        }
        String lang = language;
        if (lang.indexOf('-') != -1) {
            lang = lang.substring(0, lang.indexOf('-'));
        }
        if (wordsList.has(lang)) {
            JSONArray array = wordsList.getJSONArray(lang);
            for (int i=0 ; i<array.length() ; i++) {
                result.add(array.getString(i));
            }            
        }
        return result;
    }

    public static Set<String> getLanguages() throws IOException {
        if (wordsList == null) {
            loadWordList();
        }
        return wordsList.keySet();
    }

    private static void loadWordList() throws IOException {
        StringBuilder builder = new StringBuilder();
        try (InputStream stream = StopWords.class.getResourceAsStream("stopWords.json")) {
            try (InputStreamReader reader = new InputStreamReader(stream)) {
                try (BufferedReader buffer = new BufferedReader(reader)) {
                    String line = "";
                    while ((line = buffer.readLine()) != null) {
                        builder.append(line);
                    }
                }
            }
        }
        wordsList = new JSONObject(builder.toString());
    }
}
