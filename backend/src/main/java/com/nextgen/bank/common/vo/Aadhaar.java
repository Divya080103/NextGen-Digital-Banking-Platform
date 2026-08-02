package com.nextgen.bank.common.vo;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Aadhaar Value Object with Verhoeff Algorithm Checksum Validation.
 */
public record Aadhaar(String value) {

    private static final Pattern AADHAAR_PATTERN = Pattern.compile("^\\d{12}$");

    // Dihedral group D5 multiplication table
    private static final int[][] D = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
            {1, 2, 3, 4, 0, 6, 7, 8, 9, 5},
            {2, 3, 4, 0, 1, 7, 8, 9, 5, 6},
            {3, 4, 0, 1, 2, 8, 9, 5, 6, 7},
            {4, 0, 1, 2, 3, 9, 5, 6, 7, 8},
            {5, 9, 8, 7, 6, 0, 4, 3, 2, 1},
            {6, 5, 9, 8, 7, 1, 0, 4, 3, 2},
            {7, 6, 5, 9, 8, 2, 1, 0, 4, 3},
            {8, 7, 6, 5, 9, 3, 2, 1, 0, 4},
            {9, 8, 7, 6, 5, 4, 3, 2, 1, 0}
    };

    // Permutation table
    private static final int[][] P = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
            {1, 5, 7, 6, 2, 8, 3, 0, 9, 4},
            {5, 8, 0, 3, 7, 9, 6, 1, 4, 2},
            {8, 9, 1, 6, 0, 4, 3, 5, 2, 7},
            {9, 4, 5, 3, 1, 2, 6, 8, 7, 0},
            {4, 2, 8, 6, 5, 7, 3, 9, 0, 1},
            {2, 7, 9, 3, 8, 0, 6, 4, 1, 5},
            {7, 0, 4, 6, 9, 1, 3, 2, 5, 8}
    };

    public Aadhaar {
        Objects.requireNonNull(value, "Aadhaar number cannot be null");
        value = value.trim();
        if (!AADHAAR_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Aadhaar number must be exactly 12 digits");
        }
        if (!validateVerhoeff(value)) {
            throw new IllegalArgumentException("Invalid Aadhaar number: failed Verhoeff checksum validation");
        }
    }

    /**
     * Validates a number string using the Verhoeff algorithm.
     */
    public static boolean validateVerhoeff(String num) {
        int c = 0;
        int[] myArray = stringToReversedIntArray(num);
        for (int i = 0; i < myArray.length; i++) {
            c = D[c][P[i % 8][myArray[i]]];
        }
        return c == 0;
    }

    private static int[] stringToReversedIntArray(String num) {
        int[] myArray = new int[num.length()];
        for (int i = 0; i < num.length(); i++) {
            myArray[i] = Integer.parseInt(num.substring(num.length() - i - 1, num.length() - i));
        }
        return myArray;
    }

    /**
     * Masked Aadhaar number showing only last 4 digits (e.g., XXXX-XXXX-1234).
     */
    public String getMasked() {
        return "XXXX-XXXX-" + value.substring(8);
    }
}
