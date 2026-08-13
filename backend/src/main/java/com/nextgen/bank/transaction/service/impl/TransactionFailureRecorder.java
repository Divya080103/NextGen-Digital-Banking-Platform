package com.nextgen.bank.transaction.service.impl;

import com.nextgen.bank.common.event.OutboxEventWriter;
import com.nextgen.bank.transaction.domain.Transaction;
import com.nextgen.bank.transaction.domain.TransactionType;
import com.nextgen.bank.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persists a FAILED transaction record and its TransactionFailedEvent in an independent
 * transaction (REQUIRES_NEW) so the failure trail survives rollback of the main
 * atomic debit/credit boundary (BR-TXN-005). Kept in a separate bean so the
 * REQUIRES_NEW proxy is honoured (self-invocation would bypass it).
 */
@Component
public class TransactionFailureRecorder {

    private final TransactionRepository transactionRepository;
    private final OutboxEventWriter outboxEventWriter;

    public TransactionFailureRecorder(TransactionRepository transactionRepository,
                                      OutboxEventWriter outboxEventWriter) {
        this.transactionRepository = transactionRepository;
        this.outboxEventWriter = outboxEventWriter;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String referenceNumber,
                       String idempotencyKey,
                       UUID sourceAccountId,
                       UUID destinationAccountId,
                       BigDecimal amount,
                       TransactionType type,
                       String narrative,
                       String failureReason) {
        // If a record already exists for this idempotency key, do not attempt a duplicate insert.
        if (transactionRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            return;
        }

        Transaction txn = new Transaction(
                referenceNumber, sourceAccountId, destinationAccountId, amount, type, idempotencyKey, narrative);
        txn.markFailed(failureReason);
        Transaction saved = transactionRepository.save(txn);

        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionId", saved.getTransactionId());
        payload.put("referenceNumber", saved.getReferenceNumber());
        payload.put("sourceAccountId", sourceAccountId);
        payload.put("amount", amount);
        payload.put("failureReason", failureReason);
        payload.put("failedAt", Instant.now().toString());

        outboxEventWriter.write(
                "TRANSACTION",
                saved.getTransactionId().toString(),
                "TransactionFailedEvent",
                payload
        );
    }
}
