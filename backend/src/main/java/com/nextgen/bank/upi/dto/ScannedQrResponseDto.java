package com.nextgen.bank.upi.dto;

import java.math.BigDecimal;

public record ScannedQrResponseDto(
        String payeeVpa,
        String merchantName,
        BigDecimal amount
) {}
