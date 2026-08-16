package com.nextgen.bank.upi.domain;

import com.nextgen.bank.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit tests for the UpiProfile domain: PIN-lockout rule BR-UPI-002
 * and activation invariants (06-state-machines.md, 07-business-rules.md).
 */
class UpiProfileDomainTest {

    private UpiProfile profile;

    @BeforeEach
    void setUp() {
        profile = new UpiProfile(UUID.randomUUID(), "alice@nextgen", UUID.randomUUID(), "hashed-pin");
    }

    @Test
    @DisplayName("new profile is ACTIVE, unlocked, zero failed attempts")
    void newProfile_isActiveAndUnlocked() {
        assertThat(profile.getStatus()).isEqualTo(UpiStatus.ACTIVE);
        assertThat(profile.isPinLocked()).isFalse();
        assertThat(profile.getPinFailedAttempts()).isZero();
    }

    @Test
    @DisplayName("BR-UPI-002: 3rd wrong PIN attempt locks the profile for 24h")
    void threeFailedAttempts_locksProfile() {
        profile.registerFailedPinAttempt();
        profile.registerFailedPinAttempt();
        assertThat(profile.isPinLocked()).isFalse();

        profile.registerFailedPinAttempt();
        assertThat(profile.isPinLocked()).isTrue();
        assertThat(profile.getPinLockedUntil()).isNotNull();
    }

    @Test
    @DisplayName("BR-UPI-002: ensureActiveAndUnlocked() throws LOCKED once locked")
    void ensureActiveAndUnlocked_whenLocked_throws() {
        profile.registerFailedPinAttempt();
        profile.registerFailedPinAttempt();
        profile.registerFailedPinAttempt();

        assertThatThrownBy(profile::ensureActiveAndUnlocked)
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                        .isEqualTo(HttpStatus.LOCKED));
    }

    @Test
    @DisplayName("resetPinAttempts() clears counter and lock")
    void resetPinAttempts_clearsLock() {
        profile.registerFailedPinAttempt();
        profile.registerFailedPinAttempt();
        profile.registerFailedPinAttempt();
        assertThat(profile.isPinLocked()).isTrue();

        profile.resetPinAttempts();
        assertThat(profile.isPinLocked()).isFalse();
        assertThat(profile.getPinFailedAttempts()).isZero();
    }

    @Test
    @DisplayName("changePin() updates hash and resets the lockout counter")
    void changePin_resetsAttempts() {
        profile.registerFailedPinAttempt();
        profile.changePin("new-hash");
        assertThat(profile.getHashedPin()).isEqualTo("new-hash");
        assertThat(profile.getPinFailedAttempts()).isZero();
    }

    @Test
    @DisplayName("ensureActiveAndUnlocked() throws when profile is SUSPENDED")
    void ensureActiveAndUnlocked_whenSuspended_throws() {
        profile.setStatus(UpiStatus.SUSPENDED);
        assertThatThrownBy(profile::ensureActiveAndUnlocked)
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }
}
