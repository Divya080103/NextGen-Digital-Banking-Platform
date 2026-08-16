package com.nextgen.bank.upi.service;

import com.nextgen.bank.account.service.AccountService;
import com.nextgen.bank.common.event.OutboxEventWriter;
import com.nextgen.bank.common.exception.BusinessException;
import com.nextgen.bank.transaction.service.TransactionService;
import com.nextgen.bank.upi.domain.QrCode;
import com.nextgen.bank.upi.dto.ScanQrRequestDto;
import com.nextgen.bank.upi.dto.ScannedQrResponseDto;
import com.nextgen.bank.upi.repository.QrCodeRepository;
import com.nextgen.bank.upi.repository.UpiProfileRepository;
import com.nextgen.bank.upi.service.impl.UpiPinAttemptRecorder;
import com.nextgen.bank.upi.service.impl.UpiServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * BR-UPI-004 regression cover for QR trust.
 *
 * The payload string is not unique — the same VPA, merchant and amount always render to the same
 * {@code upi://pay?…} string, which is what makes a printed static merchant QR reusable. A
 * single-result lookup therefore blew up with {@code IncorrectResultSizeDataAccessException} (HTTP
 * 500) the moment a QR was issued twice, which is trivially easy to do from the UI.
 */
class UpiQrScanTest {

    private static final String PAYLOAD = "upi://pay?pa=divya@nextgen&pn=Ankit%20Store&am=99.00";

    private QrCodeRepository qrCodeRepository;
    private UpiServiceImpl service;

    @BeforeEach
    void setUp() {
        qrCodeRepository = mock(QrCodeRepository.class);
        service = new UpiServiceImpl(
                mock(UpiProfileRepository.class),
                qrCodeRepository,
                mock(AccountService.class),
                mock(TransactionService.class),
                mock(OutboxEventWriter.class),
                mock(PasswordEncoder.class),
                mock(UpiPinAttemptRecorder.class));
    }

    private static QrCode qr(Instant expiresAt) {
        return new QrCode("divya@nextgen", "Ankit Store", new BigDecimal("99.00"), PAYLOAD, expiresAt);
    }

    private static Instant inFifteenMinutes() {
        return Instant.now().plus(15, ChronoUnit.MINUTES);
    }

    private static Instant anHourAgo() {
        return Instant.now().minus(1, ChronoUnit.HOURS);
    }

    @Test
    @DisplayName("scanning a payload issued twice resolves instead of failing on the duplicate")
    void duplicatePayloadIsNotAnError() {
        when(qrCodeRepository.findAllByQrPayloadString(PAYLOAD))
                .thenReturn(List.of(qr(inFifteenMinutes()), qr(inFifteenMinutes())));

        ScannedQrResponseDto scanned = service.scanQr(new ScanQrRequestDto(PAYLOAD));

        assertThat(scanned.payeeVpa()).isEqualTo("divya@nextgen");
        assertThat(scanned.merchantName()).isEqualTo("Ankit Store");
        assertThat(scanned.amount()).isEqualByComparingTo("99.00");
    }

    @Test
    @DisplayName("re-issuing a lapsed dynamic QR revalidates the payload")
    void aLiveDuplicateWinsOverAnExpiredOne() {
        when(qrCodeRepository.findAllByQrPayloadString(PAYLOAD))
                .thenReturn(List.of(qr(anHourAgo()), qr(inFifteenMinutes())));

        assertThat(service.scanQr(new ScanQrRequestDto(PAYLOAD)).payeeVpa())
                .isEqualTo("divya@nextgen");
    }

    @Test
    @DisplayName("the payload is expired only once every QR behind it has lapsed")
    void allExpiredIsRejected() {
        when(qrCodeRepository.findAllByQrPayloadString(PAYLOAD))
                .thenReturn(List.of(qr(anHourAgo()), qr(anHourAgo())));

        assertThatThrownBy(() -> service.scanQr(new ScanQrRequestDto(PAYLOAD)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("a static QR has no expiry and stays scannable")
    void staticQrNeverExpires() {
        when(qrCodeRepository.findAllByQrPayloadString(PAYLOAD))
                .thenReturn(List.of(qr(null)));

        assertThat(service.scanQr(new ScanQrRequestDto(PAYLOAD)).merchantName())
                .isEqualTo("Ankit Store");
    }

    @Test
    @DisplayName("a payload this platform never issued is refused, not parsed")
    void forgedPayloadIsRejected() {
        String forged = "upi://pay?pa=attacker@nextgen&pn=Not%20Us&am=1.00";
        when(qrCodeRepository.findAllByQrPayloadString(forged)).thenReturn(List.of());

        assertThatThrownBy(() -> service.scanQr(new ScanQrRequestDto(forged)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not issued by this platform");
    }
}
