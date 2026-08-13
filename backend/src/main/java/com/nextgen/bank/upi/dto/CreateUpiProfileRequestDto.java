package com.nextgen.bank.upi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CreateUpiProfileRequestDto(
        @NotNull(message = "Customer ID is mandatory")
        UUID customerId,

        @NotBlank(message = "VPA is mandatory")
        String vpa,

        @NotNull(message = "Default account ID is mandatory")
        UUID defaultAccountId,

        @NotBlank(message = "UPI PIN is mandatory")
        @Pattern(regexp = "^(\\d{4}|\\d{6})$", message = "UPI PIN must be a 4 or 6 digit number")
        String upiPin
) {}
