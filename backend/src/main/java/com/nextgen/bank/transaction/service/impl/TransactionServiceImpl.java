package com.nextgen.bank.transaction.service.impl;

import com.nextgen.bank.account.service.AccountService;
import com.nextgen.bank.common.enums.TransactionStatus;
import com.nextgen.bank.common.event.OutboxEventWriter;
import com.nextgen.bank.common.exception.BusinessException;
import com.nextgen.bank.transaction.domain.Transaction;
import com.nextgen.bank.transaction.domain.TransactionType;
import com.nextgen.bank.transaction.dto.DepositRequestDto;
import com.nextgen.bank.transaction.dto.ReverseTransactionRequestDto;
import com.nextgen.bank.transaction.dto.TransactionResponseDto;
import com.nextgen.bank.transaction.dto.TransferRequestDto;
import com.nextgen.bank.transaction.dto.WithdrawalRequestDto;
import com.nextgen.bank.transaction.repository.TransactionRepository;
import com.nextgen.bank.transaction.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {

    // BR-TXN-002
    private static final BigDecimal SINGLE_TXN_MAX = new BigDecimal("50000.00");
    private static final BigDecimal DAILY_TRANSFER_LIMIT = new BigDecimal("100000.00");
    // BR-TXN-004
    private static final int DUPLICATE_WINDOW_SECONDS = 120;

    private static final DateTimeFormatter REF_TS =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final OutboxEventWriter outboxEventWriter;
    private final TransactionFailureRecorder failureRecorder;
    private final SecureRandom random = new SecureRandom();

    public TransactionServiceImpl(TransactionRepository transactionRepository,
                                  AccountService accountService,
                                  OutboxEventWriter outboxEventWriter,
                                  TransactionFailureRecorder failureRecorder) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.outboxEventWriter = outboxEventWriter;
        this.failureRecorder = failureRecorder;
    }

    @Override
    public TransactionResponseDto transfer(TransferRequestDto request, String idempotencyKey) {
        String key = resolveKey(idempotencyKey);

        // BR-TXN-001: idempotent replay returns the cached response without re-executing.
        var cached = transactionRepository.findByIdempotencyKey(key);
        if (cached.isPresent()) {
            return TransactionResponseDto.fromEntity(cached.get());
        }

        String referenceNumber = generateReferenceNumber("TXN");
        try {
            validateTransfer(request);

            Transaction txn = new Transaction(
                    referenceNumber,
                    request.sourceAccountId(),
                    request.destinationAccountId(),
                    request.amount(),
                    TransactionType.INTERNAL_TRANSFER,
                    key,
                    request.narrative());
            txn.markProcessing();
            Transaction saved = transactionRepository.save(txn);
            publishInitiated(saved);

            // BR-TXN-005: debit + credit inside the same @Transactional boundary.
            accountService.debit(request.sourceAccountId(), request.amount());
            accountService.credit(request.destinationAccountId(), request.amount());

            saved.markCompleted();
            transactionRepository.save(saved);
            publishCompleted(saved, "INR");

            return TransactionResponseDto.fromEntity(saved);
        } catch (BusinessException ex) {
            failureRecorder.record(referenceNumber, key, request.sourceAccountId(),
                    request.destinationAccountId(), request.amount(),
                    TransactionType.INTERNAL_TRANSFER, request.narrative(), ex.getMessage());
            throw ex;
        }
    }

    @Override
    public TransactionResponseDto executeUpiPayment(UUID sourceAccountId,
                                                    UUID destinationAccountId,
                                                    BigDecimal amount,
                                                    String narrative,
                                                    String idempotencyKey) {
        String key = resolveKey(idempotencyKey);

        var cached = transactionRepository.findByIdempotencyKey(key);
        if (cached.isPresent()) {
            return TransactionResponseDto.fromEntity(cached.get());
        }

        String referenceNumber = generateReferenceNumber("TXN");
        try {
            if (sourceAccountId.equals(destinationAccountId)) {
                throw new BusinessException("Source and destination accounts must differ",
                        HttpStatus.BAD_REQUEST, "SAME_ACCOUNT_TRANSFER");
            }
            enforceDuplicateShield(sourceAccountId, destinationAccountId, amount);

            Transaction txn = new Transaction(referenceNumber, sourceAccountId, destinationAccountId,
                    amount, TransactionType.UPI_TRANSFER, key, narrative);
            txn.markProcessing();
            Transaction saved = transactionRepository.save(txn);
            publishInitiated(saved);

            accountService.debit(sourceAccountId, amount);
            accountService.credit(destinationAccountId, amount);

            saved.markCompleted();
            transactionRepository.save(saved);
            publishCompleted(saved, "INR");

            return TransactionResponseDto.fromEntity(saved);
        } catch (BusinessException ex) {
            failureRecorder.record(referenceNumber, key, sourceAccountId, destinationAccountId,
                    amount, TransactionType.UPI_TRANSFER, narrative, ex.getMessage());
            throw ex;
        }
    }

    @Override
    public TransactionResponseDto deposit(DepositRequestDto request, String idempotencyKey) {
        String key = resolveKey(idempotencyKey);
        var cached = transactionRepository.findByIdempotencyKey(key);
        if (cached.isPresent()) {
            return TransactionResponseDto.fromEntity(cached.get());
        }

        String referenceNumber = generateReferenceNumber("TXN");
        try {
            Transaction txn = new Transaction(referenceNumber, null, request.destinationAccountId(),
                    request.amount(), TransactionType.DEPOSIT, key, request.narrative());
            txn.markProcessing();
            Transaction saved = transactionRepository.save(txn);
            publishInitiated(saved);

            accountService.credit(request.destinationAccountId(), request.amount());

            saved.markCompleted();
            transactionRepository.save(saved);
            publishCompleted(saved, "INR");
            return TransactionResponseDto.fromEntity(saved);
        } catch (BusinessException ex) {
            failureRecorder.record(referenceNumber, key, null, request.destinationAccountId(),
                    request.amount(), TransactionType.DEPOSIT, request.narrative(), ex.getMessage());
            throw ex;
        }
    }

    @Override
    public TransactionResponseDto withdraw(WithdrawalRequestDto request, String idempotencyKey) {
        String key = resolveKey(idempotencyKey);
        var cached = transactionRepository.findByIdempotencyKey(key);
        if (cached.isPresent()) {
            return TransactionResponseDto.fromEntity(cached.get());
        }

        String referenceNumber = generateReferenceNumber("TXN");
        try {
            Transaction txn = new Transaction(referenceNumber, request.sourceAccountId(), null,
                    request.amount(), TransactionType.WITHDRAWAL, key, request.narrative());
            txn.markProcessing();
            Transaction saved = transactionRepository.save(txn);
            publishInitiated(saved);

            accountService.debit(request.sourceAccountId(), request.amount());

            saved.markCompleted();
            transactionRepository.save(saved);
            publishCompleted(saved, "INR");
            return TransactionResponseDto.fromEntity(saved);
        } catch (BusinessException ex) {
            failureRecorder.record(referenceNumber, key, request.sourceAccountId(), null,
                    request.amount(), TransactionType.WITHDRAWAL, request.narrative(), ex.getMessage());
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getHistory(UUID accountId) {
        return transactionRepository
                .findBySourceAccountIdOrDestinationAccountIdOrderByExecutedAtDesc(accountId, accountId)
                .stream()
                .map(TransactionResponseDto::fromEntity)
                .toList();
    }

    @Override
    public TransactionResponseDto reverse(ReverseTransactionRequestDto request) {
        Transaction txn = transactionRepository.findById(request.transactionId())
                .orElseThrow(() -> new BusinessException("Transaction not found: " + request.transactionId(),
                        HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND"));

        // COMPLETED -> REVERSED (06-state-machines.md §5): post a compensating debit/credit.
        txn.reverse();
        transactionRepository.save(txn);

        if (txn.getDestinationAccountId() != null) {
            accountService.debit(txn.getDestinationAccountId(), txn.getAmount());
        }
        if (txn.getSourceAccountId() != null) {
            accountService.credit(txn.getSourceAccountId(), txn.getAmount());
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionId", txn.getTransactionId());
        payload.put("referenceNumber", txn.getReferenceNumber());
        payload.put("reason", request.reason());
        payload.put("reversedAt", Instant.now().toString());
        outboxEventWriter.write("TRANSACTION", txn.getTransactionId().toString(),
                "TransactionReversedEvent", payload);

        return TransactionResponseDto.fromEntity(txn);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal dailyUpiDebitTotal(UUID accountId) {
        return transactionRepository.sumDebitedSinceByType(
                accountId, TransactionType.UPI_TRANSFER, TransactionStatus.COMPLETED, startOfDayUtc());
    }

    @Override
    @Transactional(readOnly = true)
    public long dailyUpiTransactionCount(UUID accountId) {
        return transactionRepository
                .countBySourceAccountIdAndTransactionTypeAndStatusAndExecutedAtGreaterThanEqual(
                        accountId, TransactionType.UPI_TRANSFER, TransactionStatus.COMPLETED, startOfDayUtc());
    }

    // ── Validation ──────────────────────────────────────────────────────────

    private void validateTransfer(TransferRequestDto request) {
        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new BusinessException("Source and destination accounts must differ",
                    HttpStatus.BAD_REQUEST, "SAME_ACCOUNT_TRANSFER");
        }

        // BR-TXN-002: single transaction ceiling.
        if (request.amount().compareTo(SINGLE_TXN_MAX) > 0) {
            throw new BusinessException(
                    "Transfer exceeds single-transaction limit of " + SINGLE_TXN_MAX,
                    HttpStatus.BAD_REQUEST, "LIMIT_EXCEEDED");
        }

        // BR-TXN-004: duplicate-transaction shield.
        enforceDuplicateShield(request.sourceAccountId(), request.destinationAccountId(), request.amount());

        // BR-TXN-002: rolling daily transfer ceiling for the source account.
        BigDecimal todayTotal = transactionRepository.sumDebitedSince(
                request.sourceAccountId(), TransactionStatus.COMPLETED, startOfDayUtc());
        if (todayTotal.add(request.amount()).compareTo(DAILY_TRANSFER_LIMIT) > 0) {
            throw new BusinessException(
                    "Transfer would exceed the daily transfer limit of " + DAILY_TRANSFER_LIMIT,
                    HttpStatus.BAD_REQUEST, "LIMIT_EXCEEDED");
        }
    }

    private void enforceDuplicateShield(UUID sourceAccountId, UUID destinationAccountId, BigDecimal amount) {
        Instant windowStart = Instant.now().minusSeconds(DUPLICATE_WINDOW_SECONDS);
        boolean duplicate = transactionRepository.existsRecentDuplicate(
                sourceAccountId, destinationAccountId, amount,
                List.of(TransactionStatus.COMPLETED, TransactionStatus.PROCESSING),
                windowStart);
        if (duplicate) {
            throw new BusinessException(
                    "A near-identical transfer was submitted within the last "
                            + DUPLICATE_WINDOW_SECONDS + " seconds",
                    HttpStatus.CONFLICT, "DUPLICATE_TRANSACTION");
        }
    }

    // ── Events (via transactional outbox) ────────────────────────────────────

    private void publishInitiated(Transaction txn) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionId", txn.getTransactionId());
        payload.put("referenceNumber", txn.getReferenceNumber());
        payload.put("sourceAccountId", txn.getSourceAccountId());
        payload.put("destinationAccountId", txn.getDestinationAccountId());
        payload.put("amount", txn.getAmount());
        payload.put("idempotencyKey", txn.getIdempotencyKey());
        outboxEventWriter.write("TRANSACTION", txn.getTransactionId().toString(),
                "TransactionInitiatedEvent", payload);
    }

    private void publishCompleted(Transaction txn, String currency) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionId", txn.getTransactionId());
        payload.put("referenceNumber", txn.getReferenceNumber());
        payload.put("sourceAccountId", txn.getSourceAccountId());
        payload.put("destinationAccountId", txn.getDestinationAccountId());
        payload.put("amount", txn.getAmount());
        payload.put("currency", currency);
        payload.put("transactionType", txn.getTransactionType().name());
        payload.put("completedAt", txn.getExecutedAt() != null ? txn.getExecutedAt().toString() : Instant.now().toString());
        outboxEventWriter.write("TRANSACTION", txn.getTransactionId().toString(),
                "TransactionCompletedEvent", payload);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Instant startOfDayUtc() {
        return LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private String resolveKey(String idempotencyKey) {
        return (idempotencyKey == null || idempotencyKey.isBlank())
                ? UUID.randomUUID().toString()
                : idempotencyKey.trim();
    }

    private String generateReferenceNumber(String prefix) {
        String ref;
        int attempts = 0;
        do {
            if (attempts++ > 10) {
                throw new BusinessException("Unable to generate unique reference number",
                        HttpStatus.INTERNAL_SERVER_ERROR, "GEN_REF_FAILED");
            }
            ref = prefix + REF_TS.format(Instant.now()) + String.format("%04d", random.nextInt(10000));
        } while (transactionRepository.existsByReferenceNumber(ref));
        return ref;
    }
}
