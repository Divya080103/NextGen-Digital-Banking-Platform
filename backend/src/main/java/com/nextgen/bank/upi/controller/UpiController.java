package com.nextgen.bank.upi.controller;

import com.nextgen.bank.upi.dto.ChangeUpiPinRequestDto;
import com.nextgen.bank.upi.dto.CreateUpiProfileRequestDto;
import com.nextgen.bank.upi.dto.GenerateQrRequestDto;
import com.nextgen.bank.upi.dto.QrCodeResponseDto;
import com.nextgen.bank.upi.dto.ScanQrRequestDto;
import com.nextgen.bank.upi.dto.ScannedQrResponseDto;
import com.nextgen.bank.upi.dto.UpiPayRequestDto;
import com.nextgen.bank.upi.dto.UpiPaymentResponseDto;
import com.nextgen.bank.upi.dto.UpiProfileResponseDto;
import com.nextgen.bank.upi.service.UpiService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/upi")
public class UpiController {

    private static final String IDEMPOTENCY_HEADER = "X-Idempotency-Key";

    private final UpiService upiService;

    public UpiController(UpiService upiService) {
        this.upiService = upiService;
    }

    @PostMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<UpiProfileResponseDto> createProfile(
            @Valid @RequestBody CreateUpiProfileRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(upiService.createProfile(request));
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<UpiProfileResponseDto>> getProfiles(@RequestParam UUID customerId) {
        return ResponseEntity.ok(upiService.getProfilesByCustomer(customerId));
    }

    @PostMapping("/pin")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<UpiProfileResponseDto> changePin(
            @Valid @RequestBody ChangeUpiPinRequestDto request) {
        return ResponseEntity.ok(upiService.changePin(request));
    }

    @PostMapping("/pay")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<UpiPaymentResponseDto> pay(
            @Valid @RequestBody UpiPayRequestDto request,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey) {
        return ResponseEntity.ok(upiService.pay(request, idempotencyKey));
    }

    @PostMapping("/qr")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<QrCodeResponseDto> generateQr(
            @Valid @RequestBody GenerateQrRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(upiService.generateQr(request));
    }

    @PostMapping("/qr/scan")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ScannedQrResponseDto> scanQr(
            @Valid @RequestBody ScanQrRequestDto request) {
        return ResponseEntity.ok(upiService.scanQr(request));
    }
}
