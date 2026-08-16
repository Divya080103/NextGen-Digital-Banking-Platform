package com.nextgen.bank.upi.dto;

import com.nextgen.bank.upi.domain.UpiProfile;
import com.nextgen.bank.upi.domain.UpiStatus;

import java.time.Instant;
import java.util.UUID;

public record UpiProfileResponseDto(
        UUID upiId,
        UUID customerId,
        String vpa,
        UUID defaultAccountId,
        UpiStatus status,
        Instant createdAt
) {
    public static UpiProfileResponseDto fromEntity(UpiProfile profile) {
        return new UpiProfileResponseDto(
                profile.getUpiId(),
                profile.getCustomerId(),
                profile.getVpa(),
                profile.getDefaultAccountId(),
                profile.getStatus(),
                profile.getCreatedAt()
        );
    }
}
