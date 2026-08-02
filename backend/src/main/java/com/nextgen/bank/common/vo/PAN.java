package com.nextgen.bank.common.vo;

import java.util.Objects;
import java.util.regex.Pattern;

public record PAN(String value) {

    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]{1}$");

    public PAN {
        Objects.requireNonNull(value, "PAN cannot be null");
        value = value.trim().toUpperCase();
        if (!PAN_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid PAN format: " + value + ". Must match XXXXX1234X");
        }
    }
}
