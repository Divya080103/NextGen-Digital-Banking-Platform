package com.nextgen.bank.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record WithdrawalRequestDto(
        @NotNull(message = "Source account ID is mandatory")
        UUID sourceAccountId,

        @NotNull(message = "Amount is mandatory")
        @DecimalMin(value = "0.01", message = "Withdrawal amount must be greater than zero")
        BigDecimal amount,

        String narrative
) {}
