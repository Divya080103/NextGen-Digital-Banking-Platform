package com.nextgen.bank.transaction.controller;

import com.nextgen.bank.transaction.dto.DepositRequestDto;
import com.nextgen.bank.transaction.dto.ReverseTransactionRequestDto;
import com.nextgen.bank.transaction.dto.TransactionResponseDto;
import com.nextgen.bank.transaction.dto.TransferRequestDto;
import com.nextgen.bank.transaction.dto.WithdrawalRequestDto;
import com.nextgen.bank.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class TransactionController {

    private static final String IDEMPOTENCY_HEADER = "X-Idempotency-Key";

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/api/v1/transactions/transfer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<TransactionResponseDto> transfer(
            @Valid @RequestBody TransferRequestDto request,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey) {
        return ResponseEntity.ok(transactionService.transfer(request, idempotencyKey));
    }

    @PostMapping("/api/v1/transactions/deposit")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANK_STAFF', 'ADMIN')")
    public ResponseEntity<TransactionResponseDto> deposit(
            @Valid @RequestBody DepositRequestDto request,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey) {
        return ResponseEntity.ok(transactionService.deposit(request, idempotencyKey));
    }

    @PostMapping("/api/v1/transactions/withdraw")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANK_STAFF', 'ADMIN')")
    public ResponseEntity<TransactionResponseDto> withdraw(
            @Valid @RequestBody WithdrawalRequestDto request,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey) {
        return ResponseEntity.ok(transactionService.withdraw(request, idempotencyKey));
    }

    @GetMapping("/api/v1/transactions/history")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANK_STAFF', 'ADMIN', 'AUDITOR')")
    public ResponseEntity<List<TransactionResponseDto>> history(@RequestParam UUID accountId) {
        return ResponseEntity.ok(transactionService.getHistory(accountId));
    }

    @PostMapping("/api/v1/staff/transactions/reverse")
    @PreAuthorize("hasAnyRole('BANK_STAFF', 'ADMIN')")
    public ResponseEntity<TransactionResponseDto> reverse(
            @Valid @RequestBody ReverseTransactionRequestDto request) {
        return ResponseEntity.ok(transactionService.reverse(request));
    }
}
