package com.nextgen.bank.common.vo;

import java.util.Objects;
import java.util.regex.Pattern;

public record Address(
        String street,
        String city,
        String state,
        String postalCode,
        String country
) {
    private static final Pattern PIN_PATTERN = Pattern.compile("^[1-9][0-9]{5}$");

    public Address {
        Objects.requireNonNull(street, "Street cannot be null");
        Objects.requireNonNull(city, "City cannot be null");
        Objects.requireNonNull(state, "State cannot be null");
        Objects.requireNonNull(postalCode, "Postal code cannot be null");
        Objects.requireNonNull(country, "Country cannot be null");

        postalCode = postalCode.trim();
        if ("India".equalsIgnoreCase(country) && !PIN_PATTERN.matcher(postalCode).matches()) {
            throw new IllegalArgumentException("Invalid Indian PIN code format: " + postalCode);
        }
    }
}
