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

public class Term {

    private String term;
    private ArrayList<Integer> offsetSentences;
    private int termFrequency;
    private int acronymFrequency;
    private int upperCaseFreqquency;
    private double tCase;
    private double tPos;

    public Term(String term) {
        this.term = term;
        offsetSentences = new ArrayList<>();
        termFrequency = 0;
        acronymFrequency = 0;
        upperCaseFreqquency = 0;
    }

    public String getTerm() {
        return term;
    }

    public void setSentence(int i) {
        offsetSentences.add(i);
    }

    public void increaseFrequency() {
        termFrequency++;
    }

    public void increaseAcronym() {
        acronymFrequency++;
    }

    public void increaseUpperCase() {
        upperCaseFreqquency++;
    }

    public int getTermFrequency() {
        return termFrequency;
    }

    public int getAcronymFrequency() {
        return acronymFrequency;
    }

    public int getUpperCaseFreqquency() {
        return upperCaseFreqquency;
    }

    public void setTCase() {
        tCase = Math.max(upperCaseFreqquency, acronymFrequency) / (1 + Math.log(termFrequency));
    }

    public void setTPos() {
        int sum = 0;
        for (int pos : offsetSentences) {
            sum += pos;
        }
        double median = sum / offsetSentences.size();
        tPos = Math.log(Math.log(3 + median));
    }

    public void setTFNorm(double meanFrequency, double sDeviation) {
        // TODO
    }
}
