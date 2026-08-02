-- NextGen Digital Banking Platform — V1 Initial Master Database Schema
-- Matches 04-database-erd.md and PostgreSQL 16 standard specifications

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. AUTH MODULE
CREATE TABLE IF NOT EXISTS auth_users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    lockout_until TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_auth_users_email ON auth_users(email);
CREATE INDEX idx_auth_users_username ON auth_users(username);

CREATE TABLE IF NOT EXISTS auth_user_sessions (
    session_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth_users(user_id) ON DELETE CASCADE,
    refresh_token_hash VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    user_agent VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT false
);

-- 2. CUSTOMER MODULE
CREATE TABLE IF NOT EXISTS cust_profiles (
    customer_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES auth_users(user_id) ON DELETE CASCADE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    date_of_birth DATE NOT NULL,
    phone VARCHAR(15) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    kyc_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    risk_category VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cust_addresses (
    address_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES cust_profiles(customer_id) ON DELETE CASCADE,
    address_type VARCHAR(20) NOT NULL,
    street VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    postal_code VARCHAR(10) NOT NULL,
    country VARCHAR(50) NOT NULL DEFAULT 'India'
);

CREATE TABLE IF NOT EXISTS cust_kyc_docs (
    document_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES cust_profiles(customer_id) ON DELETE CASCADE,
    document_type VARCHAR(20) NOT NULL,
    document_number_enc VARCHAR(255) NOT NULL,
    file_reference VARCHAR(255) NOT NULL,
    verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    verified_by UUID NULL REFERENCES auth_users(user_id),
    verified_at TIMESTAMPTZ NULL
);

CREATE TABLE IF NOT EXISTS cust_nominees (
    nominee_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES cust_profiles(customer_id) ON DELETE CASCADE,
    full_name VARCHAR(100) NOT NULL,
    relationship VARCHAR(50) NOT NULL,
    date_of_birth DATE NOT NULL,
    phone VARCHAR(15) NOT NULL,
    allocation_percentage NUMERIC(5,2) NOT NULL DEFAULT 100.00
);

-- 3. ACCOUNT MODULE
CREATE TABLE IF NOT EXISTS acc_accounts (
    account_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_number VARCHAR(12) NOT NULL UNIQUE,
    customer_id UUID NOT NULL REFERENCES cust_profiles(customer_id),
    account_type VARCHAR(20) NOT NULL,
    balance NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    available_balance NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_APPROVAL',
    freeze_reason VARCHAR(255) NULL,
    opened_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMPTZ NULL
);

CREATE INDEX idx_acc_number ON acc_accounts(account_number);
CREATE INDEX idx_acc_customer ON acc_accounts(customer_id);

CREATE TABLE IF NOT EXISTS acc_holds (
    hold_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES acc_accounts(account_id),
    amount NUMERIC(15,2) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. TRANSACTION ENGINE & BENEFICIARIES
CREATE TABLE IF NOT EXISTS txn_transactions (
    transaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_number VARCHAR(30) NOT NULL UNIQUE,
    source_account_id UUID NULL REFERENCES acc_accounts(account_id),
    destination_account_id UUID NULL REFERENCES acc_accounts(account_id),
    amount NUMERIC(15,2) NOT NULL CHECK (amount > 0),
    fee NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    transaction_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'INITIATED',
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    failure_reason VARCHAR(255) NULL,
    narrative VARCHAR(255) NULL,
    executed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_txn_ref_num ON txn_transactions(reference_number);
CREATE INDEX idx_txn_idempotency ON txn_transactions(idempotency_key);
CREATE INDEX idx_txn_src_acc ON txn_transactions(source_account_id);
CREATE INDEX idx_txn_dest_acc ON txn_transactions(destination_account_id);

CREATE TABLE IF NOT EXISTS txn_beneficiaries (
    beneficiary_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES cust_profiles(customer_id),
    beneficiary_name VARCHAR(100) NOT NULL,
    account_number VARCHAR(20) NOT NULL,
    ifsc_code VARCHAR(11) NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    max_transfer_limit NUMERIC(15,2) NOT NULL DEFAULT 50000.00,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_COOLING_OFF',
    added_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. UPI MODULE
CREATE TABLE IF NOT EXISTS upi_profiles (
    upi_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES cust_profiles(customer_id),
    vpa VARCHAR(100) NOT NULL UNIQUE,
    default_account_id UUID NOT NULL REFERENCES acc_accounts(account_id),
    hashed_pin VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE IF NOT EXISTS upi_qr_codes (
    qr_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vpa VARCHAR(100) NOT NULL,
    merchant_name VARCHAR(100) NOT NULL,
    fixed_amount NUMERIC(15,2) NULL,
    qr_payload_string TEXT NOT NULL,
    expires_at TIMESTAMPTZ NULL
);

-- 6. LOAN MODULE
CREATE TABLE IF NOT EXISTS loan_applications (
    loan_application_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES cust_profiles(customer_id),
    requested_amount NUMERIC(15,2) NOT NULL CHECK (requested_amount > 0),
    tenure_months INTEGER NOT NULL CHECK (tenure_months > 0),
    loan_type VARCHAR(20) NOT NULL,
    monthly_income NUMERIC(15,2) NOT NULL,
    employment_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    ai_risk_score INTEGER NULL,
    ai_eligibility_decision VARCHAR(30) NULL,
    ai_repayment_probability NUMERIC(5,4) NULL,
    reviewed_by UUID NULL REFERENCES auth_users(user_id),
    rejection_reason VARCHAR(255) NULL,
    applied_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS loan_accounts (
    loan_account_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_application_id UUID NOT NULL UNIQUE REFERENCES loan_applications(loan_application_id),
    customer_id UUID NOT NULL REFERENCES cust_profiles(customer_id),
    disbursed_account_id UUID NOT NULL REFERENCES acc_accounts(account_id),
    principal_amount NUMERIC(15,2) NOT NULL,
    interest_rate NUMERIC(5,2) NOT NULL,
    tenure_months INTEGER NOT NULL,
    emi_amount NUMERIC(15,2) NOT NULL,
    outstanding_principal NUMERIC(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    disbursed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS loan_emi_schedules (
    emi_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_account_id UUID NOT NULL REFERENCES loan_accounts(loan_account_id),
    installment_number INTEGER NOT NULL,
    due_date DATE NOT NULL,
    principal_component NUMERIC(15,2) NOT NULL,
    interest_component NUMERIC(15,2) NOT NULL,
    total_emi NUMERIC(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    paid_at TIMESTAMPTZ NULL
);

-- 7. CARD MODULE
CREATE TABLE IF NOT EXISTS card_cards (
    card_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES acc_accounts(account_id),
    customer_id UUID NOT NULL REFERENCES cust_profiles(customer_id),
    masked_number VARCHAR(19) NOT NULL,
    card_type VARCHAR(20) NOT NULL,
    expiry_date DATE NOT NULL,
    cvv_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    block_reason VARCHAR(50) NULL,
    daily_pos_limit NUMERIC(15,2) NOT NULL DEFAULT 50000.00,
    daily_atm_limit NUMERIC(15,2) NOT NULL DEFAULT 25000.00,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 8. NOTIFICATIONS & AUDIT
CREATE TABLE IF NOT EXISTS notif_notifications (
    notification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id UUID NOT NULL REFERENCES auth_users(user_id),
    channel VARCHAR(20) NOT NULL,
    title VARCHAR(150) NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_logs (
    audit_log_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID NULL REFERENCES auth_users(user_id),
    actor_role VARCHAR(30) NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_name VARCHAR(50) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    old_value_json JSONB NULL,
    new_value_json JSONB NULL,
    ip_address VARCHAR(45) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_actor ON audit_logs(actor_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp);

-- 9. TRANSACTIONAL OUTBOX TABLE
CREATE TABLE IF NOT EXISTS outbox_events (
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ NULL,
    retry_count INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_status_created ON outbox_events(status, created_at) WHERE status = 'PENDING';
