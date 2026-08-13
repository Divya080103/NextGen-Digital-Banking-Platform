package com.nextgen.bank.upi.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpiPayRequestDto(
        @NotBlank(message = "Payer VPA is mandatory")
        String payerVpa,

        @NotBlank(message = "Payee VPA is mandatory")
        String payeeVpa,

        @NotNull(message = "Amount is mandatory")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "UPI PIN is mandatory")
        String upiPin,

        String qrPayload
) {}
