package com.nextgen.bank.upi.service;

import com.nextgen.bank.upi.dto.ChangeUpiPinRequestDto;
import com.nextgen.bank.upi.dto.CreateUpiProfileRequestDto;
import com.nextgen.bank.upi.dto.GenerateQrRequestDto;
import com.nextgen.bank.upi.dto.QrCodeResponseDto;
import com.nextgen.bank.upi.dto.ScanQrRequestDto;
import com.nextgen.bank.upi.dto.ScannedQrResponseDto;
import com.nextgen.bank.upi.dto.UpiPayRequestDto;
import com.nextgen.bank.upi.dto.UpiPaymentResponseDto;
import com.nextgen.bank.upi.dto.UpiProfileResponseDto;

import java.util.List;
import java.util.UUID;

public interface UpiService {

    UpiProfileResponseDto createProfile(CreateUpiProfileRequestDto request);

    UpiProfileResponseDto changePin(ChangeUpiPinRequestDto request);

    UpiPaymentResponseDto pay(UpiPayRequestDto request, String idempotencyKey);

    QrCodeResponseDto generateQr(GenerateQrRequestDto request);

    ScannedQrResponseDto scanQr(ScanQrRequestDto request);

    List<UpiProfileResponseDto> getProfilesByCustomer(UUID customerId);
}
