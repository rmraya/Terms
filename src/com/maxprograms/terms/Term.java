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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class Term implements Comparable<Term> {

    private String text;
    private Vector<Integer> offsetSentences;
    private int termFrequency;
    private int acronymFrequency;
    private int upperCaseFreqquency;
    private double position;
    private double normalizedFrequency;
    private Map<String, Integer> leftWords;
    private Map<String, Integer> rightWords;
    private double different;
    private double relatedness;
    private double score;
    private double casing;

    public Term(String term) {
        text = term;
        offsetSentences = new Vector<>();
        termFrequency = 0;
        acronymFrequency = 0;
        upperCaseFreqquency = 0;
        leftWords = new HashMap<>();
        rightWords = new HashMap<>();
    }

    public String getText() {
        return text;
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
        double median = Utils.median(offsetSentences);
        return Math.log(Math.log(3 + median));
    }

    public void calcFrequency(double meanFrequency, double sDeviation) {
        normalizedFrequency = termFrequency / (meanFrequency + 1 * sDeviation);
    }

    public double getRelevance() {
        return 1 / (1 + normalizedFrequency);
    }

    public void addLeft(String word) {
        leftWords.computeIfAbsent(word, k -> 0);
        leftWords.put(word, leftWords.get(word) + 1);
    }

    public void addRight(String word) {
        rightWords.computeIfAbsent(word, k -> 0);
        rightWords.put(word, rightWords.get(word) + 1);
    }

    public void calcDifferent(int sentences) {
        double size = offsetSentences.size();
        different = size / sentences;
    }

    public void calcRelatednes(int maxFrequency) {
        int rightSum = sum(rightWords);
        double wr = rightSum != 0 ? rightWords.size() / rightSum : 0;
        int leftSum = sum(leftWords);
        double wl = leftSum != 0 ? leftWords.size() / leftSum : 0;
        relatedness = 1 + (wr + wl) * normalizedFrequency / maxFrequency;
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
        score = relatedness * position / (casing + (normalizedFrequency / relatedness) + (different / relatedness));
    }

    public String getData() {
        return text + ',' + score + ',' + casing + ',' + position + ',' + termFrequency + ',' + getRelevance() + ','
                + relatedness + ',' + different;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    @Override
    public int compareTo(Term o) {
        if (score > o.getScore()) {
            return 1;
        } else if (score < o.getScore()) {
            return -1;
        }
        // same score, sort on term frequency
        if (termFrequency > o.getTermFrequency()) {
            return -1;
        } else if (termFrequency < o.getTermFrequency()) {
            return 1;
        }
        // same frequency, sort on term length
        if (text.length() > o.getText().length()) {
            return -1;
        } else if (text.length() < o.getText().length()) {
            return 1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Term t) {
            return text.equals(t.getText());
        }
        return false;
    }

    @Override
    public int hashCode() {
       return text.hashCode();
    }
}
