# 08. API Contracts & Endpoint Specification

## 1. Overview

This document specifies the OpenAPI-style REST API contracts for the **NextGen Digital Banking Platform (v1)**.

### Common Headers & Conventions:
* **Base URL**: `/api/v1`
* **Content-Type**: `application/json`
* **Authorization**: `Bearer <JWT_TOKEN>` (Required for all protected endpoints)
* **Idempotency**: All state-modifying POST endpoints (`/transactions/transfer`, `/upi/pay`, `/loans/apply`) accept `X-Idempotency-Key: <UUID>`.

---

## 2. Master API Endpoint Index

### 2.1. Authentication (`auth`)

#### 1. `POST /api/v1/auth/register`
* **Summary**: Register a new user account.
* **Role Required**: Public
* **Events Emitted**: `UserRegisteredEvent`
* **Request Payload**:
  ```json
  {
    "username": "johndoe",
    "email": "john.doe@example.com",
    "password": "Password123!",
    "role": "CUSTOMER"
  }
  ```
* **Response Payload (201 Created)**:
  ```json
  {
    "userId": "usr_98765432-1111-4000-a000-000000000001",
    "username": "johndoe",
    "email": "john.doe@example.com",
    "role": "CUSTOMER",
    "createdAt": "2026-08-02T19:00:00Z"
  }
  ```
* **Error Codes**: `400 BAD_REQUEST` (Password weak), `409 CONFLICT` (Email/Username exists).

#### 2. `POST /api/v1/auth/login`
* **Summary**: Authenticate user and return JWT tokens.
* **Role Required**: Public
* **Events Emitted**: `UserLoggedInEvent`
* **Request Payload**:
  ```json
  {
    "username": "johndoe",
    "password": "Password123!"
  }
  ```
* **Response Payload (200 OK)**:
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "ref_88888888-9999-4000-a000-000000000001",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
  ```
* **Error Codes**: `401 UNAUTHORIZED` (Invalid credentials), `423 LOCKED` (Account locked).

---

### 2.2. Customer Management (`customer`)

#### 3. `POST /api/v1/customers/kyc`
* **Summary**: Submit KYC identity documents.
* **Role Required**: `CUSTOMER`
* **Events Emitted**: `KYCSubmittedEvent`
* **Request Payload**:
  ```json
  {
    "documentType": "PAN",
    "documentNumber": "ABCDE1234F",
    "fileReference": "s3://bank-kyc-docs/pan_johndoe.pdf"
  }
  ```
* **Response Payload (202 Accepted)**:
  ```json
  {
    "documentId": "doc_11112222-3333-4000-a000-000000000001",
    "kycStatus": "PENDING",
    "submittedAt": "2026-08-02T19:05:00Z"
  }
  ```

#### 4. `POST /api/v1/staff/kyc/verify`
* **Summary**: Staff approves or rejects customer KYC.
* **Role Required**: `BANK_STAFF`, `ADMIN`
* **Events Emitted**: `KYCApprovedEvent` / `KYCRejectedEvent`
* **Request Payload**:
  ```json
  {
    "customerId": "cust_12345678-1111-4000-a000-000000000001",
    "status": "VERIFIED",
    "remarks": "PAN document verified against NSDL portal"
  }
  ```
* **Response Payload (200 OK)**:
  ```json
  {
    "customerId": "cust_12345678-1111-4000-a000-000000000001",
    "kycStatus": "VERIFIED",
    "verifiedAt": "2026-08-02T19:10:00Z"
  }
  ```

---

### 2.3. Account Management (`account`)

#### 5. `POST /api/v1/accounts`
* **Summary**: Open a new bank account.
* **Role Required**: `CUSTOMER` (for self), `BANK_STAFF`
* **Events Emitted**: `AccountOpenedEvent`
* **Request Payload**:
  ```json
  {
    "customerId": "cust_12345678-1111-4000-a000-000000000001",
    "accountType": "SAVINGS",
    "currency": "INR"
  }
  ```
* **Response Payload (201 Created)**:
  ```json
  {
    "accountId": "acc_11112222-3333-4000-a000-000000000001",
    "accountNumber": "501000123456",
    "accountType": "SAVINGS",
    "balance": 0.00,
    "availableBalance": 0.00,
    "status": "ACTIVE",
    "openedAt": "2026-08-02T19:15:00Z"
  }
  ```
* **Error Codes**: `422 UNPROCESSABLE_ENTITY` (KYC not verified).

---

### 2.4. Transaction Engine (`transaction`)

#### 6. `POST /api/v1/transactions/transfer`
* **Summary**: Initiate a fund transfer between accounts.
* **Role Required**: `CUSTOMER`
* **Events Emitted**: `TransactionCompletedEvent` / `TransactionFailedEvent`
* **Headers**: `X-Idempotency-Key: 7f8c2b9a-1111-4000-a000-000000000001`
* **Request Payload**:
  ```json
  {
    "sourceAccountId": "acc_11112222-3333-4000-a000-000000000001",
    "destinationAccountId": "acc_44445555-6666-4000-a000-000000000002",
    "amount": 5000.00,
    "narrative": "Monthly rent payment"
  }
  ```
* **Response Payload (200 OK)**:
  ```json
  {
    "transactionId": "txn_12345678-2222-4000-a000-000000000002",
    "referenceNumber": "TXN20260802998877",
    "status": "COMPLETED",
    "amount": 5000.00,
    "fee": 0.00,
    "executedAt": "2026-08-02T19:20:00Z"
  }
  ```
* **Error Codes**: `400 BAD_REQUEST` (Insufficient funds / Daily limit exceeded), `409 CONFLICT` (Duplicate transaction).

---

### 2.5. UPI & QR Payments (`upi`)

#### 7. `POST /api/v1/upi/pay`
* **Summary**: Pay merchant or individual via UPI / QR code.
* **Role Required**: `CUSTOMER`
* **Request Payload**:
  ```json
  {
    "payeeVpa": "merchant@nextgen",
    "amount": 250.00,
    "upiPin": "1234",
    "qrPayload": "upi://pay?pa=merchant@nextgen&pn=Store&am=250.00"
  }
  ```
* **Response Payload (200 OK)**:
  ```json
  {
    "upiTransactionId": "upi_99990000-1111-4000-a000-000000000001",
    "referenceNumber": "UPI202608020011",
    "status": "COMPLETED",
    "payeeVpa": "merchant@nextgen",
    "amount": 250.00,
    "timestamp": "2026-08-02T19:25:00Z"
  }
  ```

---

### 2.6. Loan Management (`loan`)

#### 8. `POST /api/v1/loans/apply`
* **Summary**: Apply for a personal or home loan.
* **Role Required**: `CUSTOMER`
* **Events Emitted**: `LoanAppliedEvent`, `LoanAIEvaluatedEvent`
* **Request Payload**:
  ```json
  {
    "requestedAmount": 500000.00,
    "tenureMonths": 36,
    "loanType": "PERSONAL",
    "monthlyIncome": 75000.00,
    "employmentType": "SALARIED",
    "purpose": "Home Renovation"
  }
  ```
* **Response Payload (202 Accepted)**:
  ```json
  {
    "loanApplicationId": "loan_77778888-1111-4000-a000-000000000001",
    "status": "AI_EVALUATED",
    "aiRiskScore": 765,
    "aiEligibilityDecision": "RECOMMEND_APPROVE",
    "aiRepaymentProbability": 0.9150,
    "appliedAt": "2026-08-02T19:30:00Z"
  }
  ```

---

### 2.7. Internal Contract: Java Monolith Loan Module ↔ Standalone Python AI Service

#### `POST http://ai-loan-service:8000/api/v1/ai/evaluate-risk`
* **Caller**: Java `loan` module (via REST `RestClient`)
* **Receiver**: Python FastAPI Service
* **Timeout & Fallback Policy**:
  * **Timeout**: 1500 ms read timeout.
  * **Fallback**: If Python AI Service is unavailable (5xx error or connection timeout), Java Loan Module falls back to a **local fallback Java rule evaluator** (`aiRiskScore = 650`, `aiEligibilityDecision = MANUAL_REVIEW`, `isFallback = true`).

* **Request Payload (Java -> Python)**:
  ```json
  {
    "cibilScore": 750,
    "monthlyIncome": 85000.00,
    "existingLoanEmiTotal": 15000.00,
    "repaymentHistoryScore": 95,
    "employmentType": "SALARIED",
    "requestedAmount": 500000.00,
    "tenureMonths": 36
  }
  ```

* **Response Payload (Python -> Java 200 OK)**:
  ```json
  {
    "riskScore": 765,
    "riskBand": "LOW_RISK",
    "eligibility": "RECOMMEND_APPROVE",
    "repaymentProbability": 0.9250,
    "maxRecommendedAmount": 750000.00,
    "scoringModelVersion": "v1.0-rules",
    "calculatedAt": "2026-08-02T19:30:01Z"
  }
  ```

---

### 2.8. Card Management (`card`)

#### 9. `POST /api/v1/cards/{cardId}/block`
* **Summary**: Block a debit or credit card instantly.
* **Role Required**: `CUSTOMER`, `BANK_STAFF`
* **Events Emitted**: `CardStatusUpdatedEvent`
* **Request Payload**:
  ```json
  {
    "reason": "LOST",
    "comments": "Lost wallet while travelling"
  }
  ```
* **Response Payload (200 OK)**:
  ```json
  {
    "cardId": "crd_33334444-1111-4000-a000-000000000001",
    "maskedNumber": "XXXX-XXXX-XXXX-4321",
    "status": "BLOCKED",
    "blockedAt": "2026-08-02T19:35:00Z"
  }
  ```
