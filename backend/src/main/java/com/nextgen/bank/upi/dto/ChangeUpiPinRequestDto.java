package com.nextgen.bank.upi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangeUpiPinRequestDto(
        @NotBlank(message = "VPA is mandatory")
        String vpa,

        @NotBlank(message = "Current UPI PIN is mandatory")
        String currentUpiPin,

        @NotBlank(message = "New UPI PIN is mandatory")
        @Pattern(regexp = "^(\\d{4}|\\d{6})$", message = "UPI PIN must be a 4 or 6 digit number")
        String newUpiPin
) {}
