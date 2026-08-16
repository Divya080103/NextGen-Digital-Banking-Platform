package com.nextgen.bank.upi.service.impl;

import com.nextgen.bank.upi.domain.UpiProfile;
import com.nextgen.bank.upi.repository.UpiProfileRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-UPI-002: a failed PIN attempt must be persisted even though the surrounding payment/PIN-change
 * transaction rolls back on the resulting authentication error. Runs in its own transaction
 * (REQUIRES_NEW) so the incremented attempt count and any 24h lock survive the outer rollback.
 * Kept in a separate bean so the new-transaction propagation is applied through the Spring proxy.
 */
@Component
public class UpiPinAttemptRecorder {

    private final UpiProfileRepository upiProfileRepository;

    public UpiPinAttemptRecorder(UpiProfileRepository upiProfileRepository) {
        this.upiProfileRepository = upiProfileRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(String vpa) {
        upiProfileRepository.findByVpa(vpa).ifPresent(profile -> {
            profile.registerFailedPinAttempt();
            upiProfileRepository.save(profile);
        });
    }
}
