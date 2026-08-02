package com.nextgen.bank.common.vo;

import java.util.Objects;

public record UPI_PIN(String hashedPin) {

    public UPI_PIN {
        Objects.requireNonNull(hashedPin, "Hashed PIN cannot be null");
        if (hashedPin.isBlank()) {
            throw new IllegalArgumentException("Hashed PIN cannot be empty");
        }
    }
}
