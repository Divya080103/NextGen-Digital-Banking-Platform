package com.nextgen.bank.common.vo;

import java.util.Objects;
import java.util.regex.Pattern;

public record Phone(String value) {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+91[6-9]\\d{9}$");

    public Phone {
        Objects.requireNonNull(value, "Phone number cannot be null");
        value = value.trim();
        if (!PHONE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid Indian mobile number format: " + value + ". Must match +91XXXXXXXXXX");
        }
    }
}
