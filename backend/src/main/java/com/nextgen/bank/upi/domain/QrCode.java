package com.nextgen.bank.upi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "upi_qr_codes")
public class QrCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "qr_id", nullable = false, updatable = false)
    private UUID qrId;

    @Column(name = "vpa", nullable = false, length = 100)
    private String vpa;

    @Column(name = "merchant_name", nullable = false, length = 100)
    private String merchantName;

    @Column(name = "fixed_amount", precision = 15, scale = 2)
    private BigDecimal fixedAmount;

    @Column(name = "qr_payload_string", nullable = false, columnDefinition = "TEXT")
    private String qrPayloadString;

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected QrCode() {
    }

    public QrCode(String vpa, String merchantName, BigDecimal fixedAmount,
                  String qrPayloadString, Instant expiresAt) {
        this.vpa = vpa;
        this.merchantName = merchantName;
        this.fixedAmount = fixedAmount;
        this.qrPayloadString = qrPayloadString;
        this.expiresAt = expiresAt;
    }

    /** BR-UPI-004: dynamic QR codes expire; static merchant QR codes (null expiry) never expire. */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public UUID getQrId() {
        return qrId;
    }

    public String getVpa() {
        return vpa;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public BigDecimal getFixedAmount() {
        return fixedAmount;
    }

    public String getQrPayloadString() {
        return qrPayloadString;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
