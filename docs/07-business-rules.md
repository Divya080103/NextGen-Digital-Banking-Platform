# 07. Business Rules & Domain Invariants Specification

## 1. Overview

This document defines the strict business rules, limits, invariants, and validation criteria governing every in-scope domain in the **NextGen Digital Banking Platform (v1)**.

---

## 2. Rule Specifications by Domain

### 2.1. Identity & Access (`auth`)
* **BR-AUTH-001 (Password Complexity)**: Passwords must contain a minimum of 8 characters, at least 1 uppercase letter, 1 lowercase letter, 1 digit, and 1 special character (`@$!%*?&`).
* **BR-AUTH-002 (Account Lockout)**: 5 consecutive failed login attempts lock the user account for 15 minutes.
* **BR-AUTH-003 (JWT Expiry & Refresh)**: Access tokens expire after 15 minutes. Refresh tokens expire after 7 days.
* **BR-AUTH-004 (Role Hierarchy)**:
  * `CUSTOMER`: Can access own profile, accounts, transactions, cards, loans, UPI.
  * `BANK_STAFF`: Can view customer profiles, verify KYC, review loans, freeze/unfreeze accounts.
  * `ADMIN`: System configuration, user management, staff creation.
  * `AUDITOR`: Read-only access to audit logs and compliance reports.

---

### 2.2. Customer & KYC (`customer`)
* **BR-CUST-001 (Unique Identity Constraints)**: Email, Mobile Phone Number, PAN, and Aadhaar numbers MUST be unique across the platform.
* **BR-CUST-002 (KYC Document Requirement)**: Customers must upload at least one Govt Identity Document (PAN required for Indian banking context).
* **BR-CUST-003 (Age Restriction)**: Primary account holders must be at least 18 years old based on `dateOfBirth`.
* **BR-CUST-004 (Nominee Allocation)**: Total percentage allocation across all nominees declared for a customer must equal exactly `100.00%`.

---

### 2.3. Account Management (`account`)
* **BR-ACC-001 (KYC Gate)**: No account can transition to `ACTIVE` status until customer `kycStatus` is `VERIFIED`.
* **BR-ACC-002 (Non-Negative Savings Balance)**: Savings account balances (`availableBalance`) can NEVER drop below ₹0.00.
* **BR-ACC-003 (Current Account Overdraft)**: Current accounts permit overdraft up to a pre-configured sanctioned limit (Default: ₹50,000).
* **BR-ACC-004 (Account Freeze Invariant)**: An account in `FROZEN` status rejects all outgoing debits (transfers, UPI, card POS/ATM) while permitting incoming credits.
* **BR-ACC-005 (Zero-Balance Closure)**: Account closure requires `balance == 0.00` and zero active holds/loans.

---

### 2.4. Transaction Engine (`transaction`)
* **BR-TXN-001 (Idempotency Enforcement)**: Every transfer request must provide a unique `X-Idempotency-Key` header. Repeated requests within 24 hours return the cached response without re-executing debits.
* **BR-TXN-002 (Daily Transaction Limits)**:
  * Retail Savings Account: Maximum ₹100,000 per day total transfers.
  * Single Transaction Max: ₹50,000 per transfer.
* **BR-TXN-003 (Beneficiary Cooling-Off Period)**:
  * Newly added beneficiaries have a **30-minute cooling-off period**.
  * Maximum transfer allowed during cooling-off: ₹10,000 total.
* **BR-TXN-004 (Duplicate Transaction Shield)**: Identical transfers (same source, destination, and amount) within 120 seconds are flagged as potential duplicates and rejected.
* **BR-TXN-005 (Atomic Execution & Rollback)**: Debit and Credit steps must execute inside a single `@Transactional` boundary. If credit fails, debit is automatically rolled back.

---

### 2.5. UPI & QR Payments (`upi`)
* **BR-UPI-001 (VPA Format Validation)**: VPA format must strictly adhere to `<handle>@nextgen`.
* **BR-UPI-002 (UPI PIN Hashing & Attempts)**: UPI PIN must be a 4 or 6-digit PIN stored as a BCrypt hash. 3 incorrect PIN entries temporarily lock UPI transactions for 24 hours.
* **BR-UPI-003 (UPI Daily Limit)**: Standard NPCI limit: Maximum ₹100,000 per VPA per day; maximum 10 UPI transactions per day.
* **BR-UPI-004 (QR Code Integrity)**: Dynamic QR codes expire after 15 minutes. Static merchant QR codes must contain a valid merchant VPA and checksum.

---

### 2.6. Loan Management & AI Loan Intelligence (`loan`)
* **BR-LOAN-001 (AI Scoring Gate)**: Every loan application MUST be evaluated by the **AI Loan Intelligence Service** before Staff review.
* **BR-LOAN-002 (Transparent Rule-Scoring Model for v1)**:
  The Python service calculates a score from 300 to 900 based on weighted metrics:
  * **CIBIL Score (40% Weight)**: Scale 300-900.
  * **Debt-to-Income Ratio (DTI) (30% Weight)**: `(Existing EMIs + New EMI) / Monthly Income`. If DTI > 50%, score penalized heavily.
  * **Repayment History Score (20% Weight)**: Past defaults reduce score.
  * **Employment Stability (10% Weight)**: `SALARIED` (higher score) vs `UNEMPLOYED` (0 score).
* **BR-LOAN-003 (AI Decision Bands)**:
  * Score $\ge 750$: `RECOMMEND_APPROVE` (Low Risk)
  * Score $650 - 749$: `MANUAL_REVIEW` (Medium Risk)
  * Score $< 650$: `RECOMMEND_REJECT` (High Risk)
* **BR-LOAN-004 (Staff Override Policy)**: Bank Staff may approve a loan flagged for `MANUAL_REVIEW`, but CANNOT approve a loan flagged as `RECOMMEND_REJECT` without Senior Admin override and written justification.
* **BR-LOAN-005 (EMI Calculation Invariant)**: EMI calculation uses standard reducing-balance formula:
  $$EMI = P \times r \times \frac{(1+r)^n}{(1+r)^n - 1}$$
  where $P$ is principal, $r$ is monthly interest rate, $n$ is tenure in months.

---

### 2.7. Card Management (`card`)
* **BR-CARD-001 (Card Issuance Limit)**: Maximum 1 active Debit Card and 1 active Credit Card per customer account.
* **BR-CARD-002 (Instant Card Blocking)**: Card block requests execute instantly and take effect in $< 100\text{ ms}$.
* **BR-CARD-003 (Default Card Limits)**:
  * Daily POS Limit: ₹50,000.
  * Daily ATM Limit: ₹25,000.
  * Customers may lower limits, but cannot exceed bank default maximums without Staff approval.

---

### 2.8. Notifications & Audit (`notification`, `audit`)
* **BR-NOTIF-001 (Financial Event Alerts)**: All completed transactions $> \text{₹1,000}$ MUST trigger an instant notification (SMS/Email).
* **BR-AUDIT-001 (Immutable Audit Ledger)**: Audit log entries are strictly append-only. UPDATE and DELETE SQL statements are disabled on the `audit_logs` table via database trigger policies.
