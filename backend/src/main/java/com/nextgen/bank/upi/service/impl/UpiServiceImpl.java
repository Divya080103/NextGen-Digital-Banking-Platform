package com.nextgen.bank.upi.service.impl;

import com.nextgen.bank.account.service.AccountService;
import com.nextgen.bank.common.event.OutboxEventWriter;
import com.nextgen.bank.common.exception.BusinessException;
import com.nextgen.bank.transaction.dto.TransactionResponseDto;
import com.nextgen.bank.transaction.service.TransactionService;
import com.nextgen.bank.upi.domain.QrCode;
import com.nextgen.bank.upi.domain.UpiProfile;
import com.nextgen.bank.upi.dto.ChangeUpiPinRequestDto;
import com.nextgen.bank.upi.dto.CreateUpiProfileRequestDto;
import com.nextgen.bank.upi.dto.GenerateQrRequestDto;
import com.nextgen.bank.upi.dto.QrCodeResponseDto;
import com.nextgen.bank.upi.dto.ScanQrRequestDto;
import com.nextgen.bank.upi.dto.ScannedQrResponseDto;
import com.nextgen.bank.upi.dto.UpiPayRequestDto;
import com.nextgen.bank.upi.dto.UpiPaymentResponseDto;
import com.nextgen.bank.upi.dto.UpiProfileResponseDto;
import com.nextgen.bank.upi.repository.QrCodeRepository;
import com.nextgen.bank.upi.repository.UpiProfileRepository;
import com.nextgen.bank.upi.service.UpiService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional
public class UpiServiceImpl implements UpiService {

    // BR-UPI-001: VPA must be <handle>@nextgen.
    private static final Pattern VPA_PATTERN =
            Pattern.compile("^[a-zA-Z0-9.\\-_]{2,256}@nextgen$");

    // BR-UPI-003: NPCI daily ceilings, per VPA/source account.
    private static final BigDecimal DAILY_UPI_AMOUNT_LIMIT = new BigDecimal("100000.00");
    private static final int DAILY_UPI_TXN_LIMIT = 10;

    // BR-UPI-004: dynamic QR codes expire after 15 minutes.
    private static final long DYNAMIC_QR_TTL_MINUTES = 15;

    private final UpiProfileRepository upiProfileRepository;
    private final QrCodeRepository qrCodeRepository;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final OutboxEventWriter outboxEventWriter;
    private final PasswordEncoder passwordEncoder;
    private final UpiPinAttemptRecorder pinAttemptRecorder;

    public UpiServiceImpl(UpiProfileRepository upiProfileRepository,
                          QrCodeRepository qrCodeRepository,
                          AccountService accountService,
                          TransactionService transactionService,
                          OutboxEventWriter outboxEventWriter,
                          PasswordEncoder passwordEncoder,
                          UpiPinAttemptRecorder pinAttemptRecorder) {
        this.upiProfileRepository = upiProfileRepository;
        this.qrCodeRepository = qrCodeRepository;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.outboxEventWriter = outboxEventWriter;
        this.passwordEncoder = passwordEncoder;
        this.pinAttemptRecorder = pinAttemptRecorder;
    }

    @Override
    public UpiProfileResponseDto createProfile(CreateUpiProfileRequestDto request) {
        String vpa = normalizeAndValidateVpa(request.vpa());

        if (upiProfileRepository.existsByVpa(vpa)) {
            throw new BusinessException("VPA already registered: " + vpa,
                    HttpStatus.CONFLICT, "VPA_ALREADY_EXISTS");
        }

        // Confirm the funding account exists via the Account module's public interface (VERIFY CHECK 6).
        accountService.getAccountById(request.defaultAccountId());

        String hashedPin = passwordEncoder.encode(request.upiPin());
        UpiProfile profile = new UpiProfile(request.customerId(), vpa,
                request.defaultAccountId(), hashedPin);
        UpiProfile saved = upiProfileRepository.save(profile);

        return UpiProfileResponseDto.fromEntity(saved);
    }

    @Override
    public UpiProfileResponseDto changePin(ChangeUpiPinRequestDto request) {
        String vpa = normalizeAndValidateVpa(request.vpa());
        UpiProfile profile = requireProfile(vpa);
        profile.ensureActiveAndUnlocked();

        // BR-UPI-002: an incorrect current PIN counts toward the 3-attempt lockout.
        if (!passwordEncoder.matches(request.currentUpiPin(), profile.getHashedPin())) {
            pinAttemptRecorder.recordFailedAttempt(vpa);
            throw new BusinessException("Current UPI PIN is incorrect",
                    HttpStatus.UNAUTHORIZED, "INVALID_UPI_PIN");
        }

        profile.changePin(passwordEncoder.encode(request.newUpiPin()));
        UpiProfile saved = upiProfileRepository.save(profile);

        Map<String, Object> payload = new HashMap<>();
        payload.put("vpa", saved.getVpa());
        payload.put("customerId", saved.getCustomerId());
        payload.put("timestamp", Instant.now().toString());
        outboxEventWriter.write("UPI", saved.getUpiId().toString(), "UPIPINChangedEvent", payload);

        return UpiProfileResponseDto.fromEntity(saved);
    }

    @Override
    public UpiPaymentResponseDto pay(UpiPayRequestDto request, String idempotencyKey) {
        String payerVpa = normalizeAndValidateVpa(request.payerVpa());
        String payeeVpa = normalizeAndValidateVpa(request.payeeVpa());

        UpiProfile payer = requireProfile(payerVpa);
        payer.ensureActiveAndUnlocked();

        // BR-UPI-002: verify PIN; a wrong PIN increments the lockout counter in its own transaction.
        if (!passwordEncoder.matches(request.upiPin(), payer.getHashedPin())) {
            pinAttemptRecorder.recordFailedAttempt(payerVpa);
            throw new BusinessException("Invalid UPI PIN",
                    HttpStatus.UNAUTHORIZED, "INVALID_UPI_PIN");
        }
        if (payer.getPinFailedAttempts() > 0) {
            payer.resetPinAttempts();
            upiProfileRepository.save(payer);
        }

        UpiProfile payee = requireProfile(payeeVpa);

        UUID sourceAccountId = payer.getDefaultAccountId();
        UUID destinationAccountId = payee.getDefaultAccountId();

        // BR-UPI-004: if paying against a stored dynamic QR, enforce expiry and any fixed amount.
        if (request.qrPayload() != null && !request.qrPayload().isBlank()) {
            enforceQrIntegrity(request.qrPayload(), request.amount());
        }

        enforceDailyLimits(sourceAccountId, request.amount());

        // 05-sequence-diagrams.md §4 step 6: delegate the atomic debit/credit to the Transaction Engine.
        String narrative = "UPI payment to " + payeeVpa;
        TransactionResponseDto txn = transactionService.executeUpiPayment(
                sourceAccountId, destinationAccountId, request.amount(), narrative, idempotencyKey);

        Map<String, Object> payload = new HashMap<>();
        payload.put("upiTransactionId", txn.transactionId());
        payload.put("vpa", payerVpa);
        payload.put("merchantOrPayee", payeeVpa);
        payload.put("amount", request.amount());
        payload.put("referenceNumber", txn.referenceNumber());
        outboxEventWriter.write("UPI", txn.transactionId().toString(),
                "UPIPaymentCompletedEvent", payload);

        return new UpiPaymentResponseDto(
                txn.transactionId(),
                txn.referenceNumber(),
                txn.status(),
                payeeVpa,
                txn.amount(),
                txn.executedAt() != null ? txn.executedAt() : Instant.now());
    }

    @Override
    public QrCodeResponseDto generateQr(GenerateQrRequestDto request) {
        String vpa = normalizeAndValidateVpa(request.vpa());
        requireProfile(vpa);

        // BR-UPI-004: dynamic QR carries a 15-minute expiry; static merchant QR never expires.
        Instant expiresAt = request.dynamic()
                ? Instant.now().plus(DYNAMIC_QR_TTL_MINUTES, ChronoUnit.MINUTES)
                : null;

        String payload = buildQrPayload(vpa, request.merchantName(), request.fixedAmount());
        QrCode qr = new QrCode(vpa, request.merchantName(), request.fixedAmount(), payload, expiresAt);
        QrCode saved = qrCodeRepository.save(qr);

        return QrCodeResponseDto.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ScannedQrResponseDto scanQr(ScanQrRequestDto request) {
        QrCode stored = requireIssuedQr(request.qrPayload());
        return new ScannedQrResponseDto(stored.getVpa(), stored.getMerchantName(),
                stored.getFixedAmount());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UpiProfileResponseDto> getProfilesByCustomer(UUID customerId) {
        return upiProfileRepository.findByCustomerId(customerId)
                .stream()
                .map(UpiProfileResponseDto::fromEntity)
                .toList();
    }

    // ── Business-rule helpers ─────────────────────────────────────────────────

    private void enforceDailyLimits(UUID sourceAccountId, BigDecimal amount) {
        BigDecimal spentToday = transactionService.dailyUpiDebitTotal(sourceAccountId);
        if (spentToday.add(amount).compareTo(DAILY_UPI_AMOUNT_LIMIT) > 0) {
            throw new BusinessException(
                    "Payment would exceed the daily UPI limit of " + DAILY_UPI_AMOUNT_LIMIT,
                    HttpStatus.BAD_REQUEST, "UPI_DAILY_LIMIT_EXCEEDED");
        }
        long countToday = transactionService.dailyUpiTransactionCount(sourceAccountId);
        if (countToday >= DAILY_UPI_TXN_LIMIT) {
            throw new BusinessException(
                    "Daily UPI transaction count limit of " + DAILY_UPI_TXN_LIMIT + " reached",
                    HttpStatus.BAD_REQUEST, "UPI_DAILY_COUNT_EXCEEDED");
        }
    }

    private void enforceQrIntegrity(String qrPayload, BigDecimal requestedAmount) {
        QrCode qr = requireIssuedQr(qrPayload);
        if (qr.getFixedAmount() != null
                && qr.getFixedAmount().compareTo(requestedAmount) != 0) {
            throw new BusinessException(
                    "Amount does not match the fixed amount encoded in the QR code",
                    HttpStatus.BAD_REQUEST, "QR_AMOUNT_MISMATCH");
        }
    }

    /**
     * BR-UPI-004: a QR payload is only trustworthy if this platform issued it. Accepting an
     * arbitrary payload would let a caller forge one, or replay an expired one, since the
     * payload string itself carries no checksum.
     *
     * One payload can map to several rows, because re-issuing the same VPA / merchant / amount
     * renders an identical string. The duplicates are interchangeable for scanning — same payee,
     * same merchant, same amount — so they differ only in expiry, and the live one wins: issuing a
     * fresh dynamic QR revalidates the payload, and the payload is only expired once every QR
     * behind it has lapsed.
     */
    private QrCode requireIssuedQr(String qrPayload) {
        List<QrCode> issued = qrCodeRepository.findAllByQrPayloadString(qrPayload);
        if (issued.isEmpty()) {
            throw new BusinessException("QR code was not issued by this platform",
                    HttpStatus.UNPROCESSABLE_ENTITY, "QR_NOT_RECOGNIZED");
        }
        return issued.stream()
                .filter(qr -> !qr.isExpired())
                .findFirst()
                .orElseThrow(() -> new BusinessException("QR code has expired",
                        HttpStatus.UNPROCESSABLE_ENTITY, "QR_EXPIRED"));
    }

    private String normalizeAndValidateVpa(String rawVpa) {
        if (rawVpa == null) {
            throw new BusinessException("VPA is mandatory", HttpStatus.BAD_REQUEST, "INVALID_VPA");
        }
        String vpa = rawVpa.trim().toLowerCase();
        if (!VPA_PATTERN.matcher(vpa).matches()) {
            throw new BusinessException(
                    "Invalid VPA format: " + rawVpa + ". Must be <handle>@nextgen",
                    HttpStatus.BAD_REQUEST, "INVALID_VPA");
        }
        return vpa;
    }

    private UpiProfile requireProfile(String vpa) {
        return upiProfileRepository.findByVpa(vpa)
                .orElseThrow(() -> new BusinessException("No UPI profile found for VPA: " + vpa,
                        HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND"));
    }

    // ── QR payload serialization ──────────────────────────────────────────────

    private String buildQrPayload(String vpa, String merchantName, BigDecimal fixedAmount) {
        StringBuilder sb = new StringBuilder("upi://pay?pa=").append(vpa)
                .append("&pn=").append(merchantName.replace(" ", "%20"));
        if (fixedAmount != null) {
            sb.append("&am=").append(fixedAmount.toPlainString());
        }
        return sb.toString();
    }
}
