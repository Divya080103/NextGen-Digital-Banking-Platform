package com.nextgen.bank.upi.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record GenerateQrRequestDto(
        @NotBlank(message = "VPA is mandatory")
        String vpa,

        @NotBlank(message = "Merchant name is mandatory")
        String merchantName,

        BigDecimal fixedAmount,

        boolean dynamic
) {}
