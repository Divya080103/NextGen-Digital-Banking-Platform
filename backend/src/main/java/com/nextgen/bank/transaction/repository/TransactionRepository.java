package com.nextgen.bank.transaction.repository;

import com.nextgen.bank.common.enums.TransactionStatus;
import com.nextgen.bank.transaction.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    boolean existsByReferenceNumber(String referenceNumber);

    List<Transaction> findBySourceAccountIdOrDestinationAccountIdOrderByExecutedAtDesc(
            UUID sourceAccountId, UUID destinationAccountId);

    /**
     * BR-TXN-002: total amount of completed outgoing transactions for a source account since a cutoff.
     */
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.sourceAccountId = :accountId
              AND t.status = :status
              AND t.executedAt >= :since
            """)
    BigDecimal sumDebitedSince(@Param("accountId") UUID accountId,
                               @Param("status") TransactionStatus status,
                               @Param("since") Instant since);

    /**
     * BR-TXN-004: detect an identical (source, destination, amount) transfer within a short window.
     */
    @Query("""
            SELECT COUNT(t) > 0
            FROM Transaction t
            WHERE t.sourceAccountId = :sourceAccountId
              AND t.destinationAccountId = :destinationAccountId
              AND t.amount = :amount
              AND t.status IN (:statuses)
              AND t.executedAt >= :since
            """)
    boolean existsRecentDuplicate(@Param("sourceAccountId") UUID sourceAccountId,
                                  @Param("destinationAccountId") UUID destinationAccountId,
                                  @Param("amount") BigDecimal amount,
                                  @Param("statuses") List<TransactionStatus> statuses,
                                  @Param("since") Instant since);

    /**
     * BR-UPI-003: count completed transactions for a source account since a cutoff (daily UPI count).
     */
    long countBySourceAccountIdAndTransactionTypeAndStatusAndExecutedAtGreaterThanEqual(
            UUID sourceAccountId,
            com.nextgen.bank.transaction.domain.TransactionType transactionType,
            TransactionStatus status,
            Instant since);

    /**
     * BR-UPI-003: total completed debited amount of a given type for a source account since a cutoff.
     */
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.sourceAccountId = :accountId
              AND t.transactionType = :type
              AND t.status = :status
              AND t.executedAt >= :since
            """)
    BigDecimal sumDebitedSinceByType(@Param("accountId") UUID accountId,
                                     @Param("type") com.nextgen.bank.transaction.domain.TransactionType type,
                                     @Param("status") TransactionStatus status,
                                     @Param("since") Instant since);
}
