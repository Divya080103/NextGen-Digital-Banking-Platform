package com.nextgen.bank.common.vo;

import java.util.Objects;
import java.util.regex.Pattern;

public record UPI_ID(String value) {

    private static final Pattern UPI_PATTERN = Pattern.compile("^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}$");

    public UPI_ID {
        Objects.requireNonNull(value, "UPI VPA cannot be null");
        value = value.trim().toLowerCase();
        if (!UPI_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid UPI VPA format: " + value + ". Must match handle@bank");
        }
    }
}
