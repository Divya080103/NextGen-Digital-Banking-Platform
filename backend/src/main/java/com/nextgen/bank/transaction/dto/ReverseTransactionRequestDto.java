package com.nextgen.bank.transaction.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReverseTransactionRequestDto(
        @NotNull(message = "Transaction ID is mandatory")
        UUID transactionId,

        String reason
) {}
