package com.nextgen.bank.upi.dto;

import jakarta.validation.constraints.NotBlank;

public record ScanQrRequestDto(
        @NotBlank(message = "QR payload is mandatory")
        String qrPayload
) {}
