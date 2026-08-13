package com.nextgen.bank.transaction.domain;

import com.nextgen.bank.common.enums.TransactionStatus;
import com.nextgen.bank.common.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "txn_transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;

    @Column(name = "reference_number", nullable = false, unique = true, length = 30)
    private String referenceNumber;

    @Column(name = "source_account_id")
    private UUID sourceAccountId;

    @Column(name = "destination_account_id")
    private UUID destinationAccountId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal fee = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.INITIATED;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "narrative", length = 255)
    private String narrative;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    protected Transaction() {
    }

    public Transaction(String referenceNumber,
                       UUID sourceAccountId,
                       UUID destinationAccountId,
                       BigDecimal amount,
                       TransactionType transactionType,
                       String idempotencyKey,
                       String narrative) {
        this.referenceNumber = referenceNumber;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.transactionType = transactionType;
        this.idempotencyKey = idempotencyKey;
        this.narrative = narrative;
        this.fee = BigDecimal.ZERO;
        this.status = TransactionStatus.INITIATED;
    }

    @PrePersist
    protected void onCreate() {
        if (executedAt == null) {
            executedAt = Instant.now();
        }
        if (fee == null) {
            fee = BigDecimal.ZERO;
        }
        if (status == null) {
            status = TransactionStatus.INITIATED;
        }
    }

    // ── State machine (06-state-machines.md §5) ─────────────────────────────

    public void markProcessing() {
        if (this.status != TransactionStatus.INITIATED) {
            throw new BusinessException(
                    "Cannot move to PROCESSING from state: " + this.status,
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INVALID_STATE_TRANSITION"
            );
        }
        this.status = TransactionStatus.PROCESSING;
    }

    public void markCompleted() {
        if (this.status != TransactionStatus.PROCESSING) {
            throw new BusinessException(
                    "Cannot complete transaction from state: " + this.status,
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INVALID_STATE_TRANSITION"
            );
        }
        this.status = TransactionStatus.COMPLETED;
        this.executedAt = Instant.now();
    }

    public void markFailed(String reason) {
        if (this.status == TransactionStatus.COMPLETED || this.status == TransactionStatus.REVERSED) {
            throw new BusinessException(
                    "Cannot fail a transaction already in state: " + this.status,
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INVALID_STATE_TRANSITION"
            );
        }
        this.status = TransactionStatus.FAILED;
        this.failureReason = reason;
    }

    public void reverse() {
        if (this.status != TransactionStatus.COMPLETED) {
            throw new BusinessException(
                    "Only COMPLETED transactions can be reversed. Current state: " + this.status,
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INVALID_STATE_TRANSITION"
            );
        }
        this.status = TransactionStatus.REVERSED;
    }

    // ── Getters / setters ───────────────────────────────────────────────────

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public void setSourceAccountId(UUID sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public UUID getDestinationAccountId() {
        return destinationAccountId;
    }

    public void setDestinationAccountId(UUID destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getNarrative() {
        return narrative;
    }

    public void setNarrative(String narrative) {
        this.narrative = narrative;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }
}
