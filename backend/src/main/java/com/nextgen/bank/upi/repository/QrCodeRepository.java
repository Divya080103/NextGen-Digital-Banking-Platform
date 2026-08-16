package com.nextgen.bank.upi.repository;

import com.nextgen.bank.upi.domain.QrCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QrCodeRepository extends JpaRepository<QrCode, UUID> {

    /**
     * Returns every QR issued for this payload, not one.
     *
     * The payload string is deliberately not unique: the same VPA, merchant name and fixed amount
     * always render to the same {@code upi://pay?…} string, which is what makes a printed static
     * merchant QR stable and re-displayable. Re-issuing therefore inserts a second row with an
     * identical payload, and a single-result lookup here throws
     * {@code IncorrectResultSizeDataAccessException}, surfacing as a 500 on scan and on pay.
     */
    List<QrCode> findAllByQrPayloadString(String qrPayloadString);
}
