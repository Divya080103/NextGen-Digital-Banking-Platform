package com.nextgen.bank.transaction.dto;

import com.nextgen.bank.common.enums.TransactionStatus;
import com.nextgen.bank.transaction.domain.Transaction;
import com.nextgen.bank.transaction.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponseDto(
        UUID transactionId,
        String referenceNumber,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        BigDecimal fee,
        TransactionType transactionType,
        TransactionStatus status,
        String failureReason,
        String narrative,
        Instant executedAt
) {
    public static TransactionResponseDto fromEntity(Transaction txn) {
        return new TransactionResponseDto(
                txn.getTransactionId(),
                txn.getReferenceNumber(),
                txn.getSourceAccountId(),
                txn.getDestinationAccountId(),
                txn.getAmount(),
                txn.getFee(),
                txn.getTransactionType(),
                txn.getStatus(),
                txn.getFailureReason(),
                txn.getNarrative(),
                txn.getExecutedAt()
        );
    }
}
