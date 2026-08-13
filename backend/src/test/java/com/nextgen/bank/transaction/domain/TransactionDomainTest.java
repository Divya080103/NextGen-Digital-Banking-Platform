package com.nextgen.bank.transaction.domain;

import com.nextgen.bank.common.enums.TransactionStatus;
import com.nextgen.bank.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit tests for the Transaction domain state machine (06-state-machines.md §5).
 * No Spring context required.
 */
class TransactionDomainTest {

    private Transaction txn;

    @BeforeEach
    void setUp() {
        txn = new Transaction(
                "TXN20260812000001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("1000.00"),
                TransactionType.INTERNAL_TRANSFER,
                UUID.randomUUID().toString(),
                "Rent");
    }

    @Test
    @DisplayName("new transaction starts INITIATED with zero fee")
    void newTransaction_isInitiated() {
        assertThat(txn.getStatus()).isEqualTo(TransactionStatus.INITIATED);
        assertThat(txn.getFee()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("INITIATED → PROCESSING → COMPLETED: happy path sets executedAt")
    void fullHappyPath_completes() {
        txn.markProcessing();
        assertThat(txn.getStatus()).isEqualTo(TransactionStatus.PROCESSING);

        txn.markCompleted();
        assertThat(txn.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(txn.getExecutedAt()).isNotNull();
    }

    @Test
    @DisplayName("markProcessing() twice is rejected")
    void markProcessing_fromProcessing_throws() {
        txn.markProcessing();
        assertThatThrownBy(txn::markProcessing)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PROCESSING");
    }

    @Test
    @DisplayName("markCompleted() from INITIATED is rejected (must pass through PROCESSING)")
    void markCompleted_fromInitiated_throws() {
        assertThatThrownBy(txn::markCompleted)
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("markFailed() from PROCESSING records the reason")
    void markFailed_fromProcessing_setsReason() {
        txn.markProcessing();
        txn.markFailed("INSUFFICIENT_FUNDS");
        assertThat(txn.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(txn.getFailureReason()).isEqualTo("INSUFFICIENT_FUNDS");
    }

    @Test
    @DisplayName("markFailed() on a COMPLETED transaction is rejected")
    void markFailed_fromCompleted_throws() {
        txn.markProcessing();
        txn.markCompleted();
        assertThatThrownBy(() -> txn.markFailed("late"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("COMPLETED → REVERSED: reverse() succeeds")
    void reverse_fromCompleted_succeeds() {
        txn.markProcessing();
        txn.markCompleted();
        txn.reverse();
        assertThat(txn.getStatus()).isEqualTo(TransactionStatus.REVERSED);
    }

    @Test
    @DisplayName("reverse() is only allowed from COMPLETED")
    void reverse_fromInitiated_throws() {
        assertThatThrownBy(txn::reverse)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("COMPLETED");
    }
}
