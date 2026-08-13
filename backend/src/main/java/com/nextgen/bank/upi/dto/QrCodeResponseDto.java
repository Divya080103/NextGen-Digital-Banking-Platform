package com.nextgen.bank.upi.dto;

import com.nextgen.bank.upi.domain.QrCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record QrCodeResponseDto(
        UUID qrId,
        String vpa,
        String merchantName,
        BigDecimal fixedAmount,
        String qrPayloadString,
        Instant expiresAt
) {
    public static QrCodeResponseDto fromEntity(QrCode qr) {
        return new QrCodeResponseDto(
                qr.getQrId(),
                qr.getVpa(),
                qr.getMerchantName(),
                qr.getFixedAmount(),
                qr.getQrPayloadString(),
                qr.getExpiresAt()
        );
    }
}
