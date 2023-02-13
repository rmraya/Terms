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

public class Candidate implements Comparable<Candidate> {

    private String term;
    private int frequency;
    private double score;

    public Candidate(String term, int frequency, double score) {
        this.term = term;
        this.frequency = frequency;
        this.score = score;
    }

    public String getTerm() {
        return term;
    }

    public int getFrequency() {
        return frequency;
    }

    public double getScore() {
        return score;
    }

    @Override
    public int compareTo(Candidate o) {
        if (score < o.score) {
            return 1;
        }
        if (score > o.score) {
            return -1;
        }
        return -1 * term.compareTo(o.term);
    }

}
