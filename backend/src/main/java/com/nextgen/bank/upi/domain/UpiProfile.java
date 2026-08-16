package com.nextgen.bank.upi.domain;

import com.nextgen.bank.common.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "upi_profiles")
public class UpiProfile {

    // BR-UPI-002: 3 incorrect PIN entries lock UPI transactions for 24 hours.
    private static final int MAX_PIN_ATTEMPTS = 3;
    private static final long LOCK_HOURS = 24;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "upi_id", nullable = false, updatable = false)
    private UUID upiId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "vpa", nullable = false, unique = true, length = 100)
    private String vpa;

    @Column(name = "default_account_id", nullable = false)
    private UUID defaultAccountId;

    @Column(name = "hashed_pin", nullable = false, length = 255)
    private String hashedPin;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UpiStatus status = UpiStatus.ACTIVE;

    @Column(name = "pin_failed_attempts", nullable = false)
    private int pinFailedAttempts = 0;

    @Column(name = "pin_locked_until")
    private Instant pinLockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UpiProfile() {
    }

    public UpiProfile(UUID customerId, String vpa, UUID defaultAccountId, String hashedPin) {
        this.customerId = customerId;
        this.vpa = vpa;
        this.defaultAccountId = defaultAccountId;
        this.hashedPin = hashedPin;
        this.status = UpiStatus.ACTIVE;
        this.pinFailedAttempts = 0;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = UpiStatus.ACTIVE;
        }
    }

    public boolean isPinLocked() {
        return pinLockedUntil != null && Instant.now().isBefore(pinLockedUntil);
    }

    /**
     * BR-UPI-002: records a wrong PIN entry, locking the profile for 24h once the attempt
     * ceiling is reached.
     */
    public void registerFailedPinAttempt() {
        this.pinFailedAttempts++;
        if (this.pinFailedAttempts >= MAX_PIN_ATTEMPTS) {
            this.pinLockedUntil = Instant.now().plus(LOCK_HOURS, ChronoUnit.HOURS);
        }
    }

    public void resetPinAttempts() {
        this.pinFailedAttempts = 0;
        this.pinLockedUntil = null;
    }

    public void ensureActiveAndUnlocked() {
        if (this.status != UpiStatus.ACTIVE) {
            throw new BusinessException("UPI profile is not active. Status: " + this.status,
                    HttpStatus.UNPROCESSABLE_ENTITY, "UPI_PROFILE_INACTIVE");
        }
        if (isPinLocked()) {
            throw new BusinessException(
                    "UPI transactions are temporarily locked due to repeated incorrect PIN attempts. "
                            + "Locked until: " + pinLockedUntil,
                    HttpStatus.LOCKED, "UPI_PIN_LOCKED");
        }
    }

    public void changePin(String newHashedPin) {
        this.hashedPin = newHashedPin;
        resetPinAttempts();
    }

    // ── Getters / setters ───────────────────────────────────────────────────

    public UUID getUpiId() {
        return upiId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getVpa() {
        return vpa;
    }

    public UUID getDefaultAccountId() {
        return defaultAccountId;
    }

    public void setDefaultAccountId(UUID defaultAccountId) {
        this.defaultAccountId = defaultAccountId;
    }

    public String getHashedPin() {
        return hashedPin;
    }

    public UpiStatus getStatus() {
        return status;
    }

    public void setStatus(UpiStatus status) {
        this.status = status;
    }

    public int getPinFailedAttempts() {
        return pinFailedAttempts;
    }

    public Instant getPinLockedUntil() {
        return pinLockedUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
