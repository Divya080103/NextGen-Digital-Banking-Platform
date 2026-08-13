package com.nextgen.bank.account.service;

import com.nextgen.bank.account.dto.AccountHoldRequestDto;
import com.nextgen.bank.account.dto.AccountHoldResponseDto;
import com.nextgen.bank.account.dto.AccountResponseDto;
import com.nextgen.bank.account.dto.FreezeAccountRequestDto;
import com.nextgen.bank.account.dto.OpenAccountRequestDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface AccountService {

    AccountResponseDto openAccount(OpenAccountRequestDto requestDto);

    AccountResponseDto getAccountById(UUID accountId);

    /**
     * Debit an account within the caller's transaction boundary (e.g. Transaction Engine).
     * Enforces account-state and non-negative/overdraft invariants (BR-ACC-002/003/004).
     */
    AccountResponseDto debit(UUID accountId, BigDecimal amount);

    /**
     * Credit an account within the caller's transaction boundary.
     * Permitted for ACTIVE and FROZEN accounts (BR-ACC-004: frozen accepts incoming credits).
     */
    AccountResponseDto credit(UUID accountId, BigDecimal amount);

    AccountResponseDto getAccountByNumber(String accountNumber);

    List<AccountResponseDto> getAccountsByCustomerId(UUID customerId);

    AccountResponseDto freezeAccount(UUID accountId, FreezeAccountRequestDto requestDto);

    AccountResponseDto unfreezeAccount(UUID accountId);

    AccountResponseDto closeAccount(UUID accountId);

    AccountHoldResponseDto placeHold(UUID accountId, AccountHoldRequestDto requestDto);

    void releaseHold(UUID accountId, UUID holdId);

    List<AccountHoldResponseDto> getAccountHolds(UUID accountId);
}
