package com.nextgen.bank.upi.dto;

import com.nextgen.bank.common.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UpiPaymentResponseDto(
        UUID upiTransactionId,
        String referenceNumber,
        TransactionStatus status,
        String payeeVpa,
        BigDecimal amount,
        Instant timestamp
) {}
