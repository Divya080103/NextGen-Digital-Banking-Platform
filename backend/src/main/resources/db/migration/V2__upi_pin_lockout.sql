-- V2 — UPI PIN lockout tracking (BR-UPI-002) and profile creation timestamp.
-- Adds columns required by the upi_profiles entity mapping. Additive only.

ALTER TABLE upi_profiles
    ADD COLUMN IF NOT EXISTS pin_failed_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS pin_locked_until TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_upi_vpa ON upi_profiles(vpa);
CREATE INDEX IF NOT EXISTS idx_upi_customer ON upi_profiles(customer_id);
CREATE INDEX IF NOT EXISTS idx_upi_qr_vpa ON upi_qr_codes(vpa);
