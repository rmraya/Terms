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

public class LevenshteinDistance {

    private LevenshteinDistance() {
        // do not instantiate
    }

    public static int distance(String x, String y) {
        int[][] matrix = new int[x.length() + 1][y.length() + 1];
        for (int i = 0; i <= x.length(); i++) {
            for (int j = 0; j <= y.length(); j++) {
                if (i == 0) {
                    matrix[i][j] = j;
                } else if (j == 0) {
                    matrix[i][j] = i;
                } else {
                    matrix[i][j] = min(
                            matrix[i - 1][j - 1] + ((x.charAt(i - 1) == y.charAt(j - 1)) ? 0 : 1),
                            matrix[i - 1][j] + 1,
                            matrix[i][j - 1] + 1);
                }
            }
        }
        return matrix[x.length()][y.length()];
    }

    private static int min(int a, int b, int c) {
        int min = Math.min(a, b);
        return Math.min(min, c);
    }
}
