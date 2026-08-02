package com.nextgen.bank.common.vo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AadhaarTest {

    @Test
    void validAadhaar_ShouldInstantiate() {
        // 367543801531 is a valid Verhoeff 12-digit number
        assertDoesNotThrow(() -> new Aadhaar("367543801531"));
        Aadhaar aadhaar = new Aadhaar("367543801531");
        assertEquals("367543801531", aadhaar.value());
        assertEquals("XXXX-XXXX-1531", aadhaar.getMasked());
    }

    @Test
    void invalidAadhaar_ChecksumFailure_ShouldThrowException() {
        // 123456789012 fails Verhoeff algorithm checksum
        Exception exception = assertThrows(IllegalArgumentException.class, () -> new Aadhaar("123456789012"));
        assertTrue(exception.getMessage().contains("failed Verhoeff checksum validation"));
    }

    @Test
    void invalidAadhaar_FormatFailure_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> new Aadhaar("12345"));
        assertThrows(IllegalArgumentException.class, () -> new Aadhaar("1234567890123"));
        assertThrows(IllegalArgumentException.class, () -> new Aadhaar("ABCDEFGHIJKL"));
    }
}
