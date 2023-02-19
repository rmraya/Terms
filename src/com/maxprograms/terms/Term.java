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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Term implements Comparable<Term> {

    private String term;
    private ArrayList<Integer> offsetSentences;
    private int termFrequency;
    private int acronymFrequency;
    private int upperCaseFreqquency;
    private double position;
    private double frequency;
    private Map<String, Integer> leftWords;
    private Map<String, Integer> rightWords;
    private double different;
    private double relatedness;
    private double score;
    private double casing;

    public Term(String term) {
        this.term = term;
        offsetSentences = new ArrayList<>();
        termFrequency = 0;
        acronymFrequency = 0;
        upperCaseFreqquency = 0;
        leftWords = new HashMap<>();
        rightWords = new HashMap<>();
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

    private double getCasing() {
        return Math.max(upperCaseFreqquency, acronymFrequency) / (1 + Math.log(termFrequency));
    }

    public double getPosition() {
        int sum = 0;
        for (int pos : offsetSentences) {
            sum +=  pos;
        }
        double median = sum / offsetSentences.size();
        return Math.log(Math.log(3 + median));
    }

    public void calcFrequency(double meanFrequency, double sDeviation) {
        frequency = termFrequency / ((meanFrequency + 1) * sDeviation);
    }

    public void addLeft(String word) {
        if (!leftWords.containsKey(word)) {
            leftWords.put(word, 0);
        }
        leftWords.put(word, leftWords.get(word) + 1);
    }

    public void addRight(String word) {
        if (!rightWords.containsKey(word)) {
            rightWords.put(word, 0);
        }
        rightWords.put(word, rightWords.get(word) + 1);
    }

    public void calcDifferent(int sentences) {
        different = offsetSentences.size() / sentences;
    }

    public void calcRelatednes(int maxFrequency) {
        int rightSum = sum(rightWords);
        double wr = rightSum != 0 ? rightWords.size() / rightSum : 0;
        int leftSum = sum(leftWords);
        double wl = leftSum != 0 ? leftWords.size() / leftSum : 0;
        relatedness = 1 + (wr + wl) * frequency / maxFrequency;
    }

    private int sum(Map<String, Integer> map) {
        int result = 0;
        Set<String> keys = map.keySet();
        Iterator<String> it = keys.iterator();
        while (it.hasNext()) {
            result += map.get(it.next());
        }
        return result;
    }

    public void calcScore() {
        casing = getCasing();
        position = getPosition();
        score = relatedness * position / (casing + (frequency / relatedness) + (different / relatedness));
    }

    public String getData() {
        return term + "\t" + score + "\t" + casing + "\t" + position + "\t" + frequency + "\t" + relatedness + "\t"
                + different;
    }

    public double getScore() {
        return score;
    }

    @Override
    public int compareTo(Term o) {
        if (score > o.getScore()) {
            return 1;
        } else if (score < o.getScore()) {
            return -1;
        }
        return 0;
    }
}
