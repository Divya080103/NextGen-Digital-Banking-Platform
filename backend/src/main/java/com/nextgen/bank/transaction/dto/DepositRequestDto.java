package com.nextgen.bank.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositRequestDto(
        @NotNull(message = "Destination account ID is mandatory")
        UUID destinationAccountId,

        @NotNull(message = "Amount is mandatory")
        @DecimalMin(value = "0.01", message = "Deposit amount must be greater than zero")
        BigDecimal amount,

        String narrative
) {}
