package com.nextgen.bank.transaction.service;

import com.nextgen.bank.transaction.dto.DepositRequestDto;
import com.nextgen.bank.transaction.dto.ReverseTransactionRequestDto;
import com.nextgen.bank.transaction.dto.TransactionResponseDto;
import com.nextgen.bank.transaction.dto.TransferRequestDto;
import com.nextgen.bank.transaction.dto.WithdrawalRequestDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface TransactionService {

    TransactionResponseDto transfer(TransferRequestDto request, String idempotencyKey);

    TransactionResponseDto deposit(DepositRequestDto request, String idempotencyKey);

    TransactionResponseDto withdraw(WithdrawalRequestDto request, String idempotencyKey);

    List<TransactionResponseDto> getHistory(UUID accountId);

    TransactionResponseDto reverse(ReverseTransactionRequestDto request);

    /**
     * Public integration point for the UPI module (05-sequence-diagrams.md §4, step 6):
     * UPI delegates fund movement to the Transaction Engine which performs the atomic debit/credit.
     */
    TransactionResponseDto executeUpiPayment(UUID sourceAccountId,
                                             UUID destinationAccountId,
                                             BigDecimal amount,
                                             String narrative,
                                             String idempotencyKey);

    /**
     * BR-UPI-003: total value of COMPLETED UPI debits from an account since the start of the current UTC day.
     * Exposed so the UPI module can enforce its daily ceiling without reaching into the transaction store.
     */
    BigDecimal dailyUpiDebitTotal(UUID accountId);

    /**
     * BR-UPI-003: number of COMPLETED UPI debits from an account since the start of the current UTC day.
     */
    long dailyUpiTransactionCount(UUID accountId);
}
