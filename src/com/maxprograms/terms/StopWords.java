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
