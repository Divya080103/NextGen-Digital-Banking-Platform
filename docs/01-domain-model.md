# 01. Domain Model Specification

## 1. System Overview & In-Scope Domains

The **NextGen Digital Banking Platform (v1)** is designed as a **Modular Monolith** containing 9 core Spring Boot domain modules alongside 1 dedicated standalone Python microservice (**AI Loan Intelligence Service**). 

The in-scope domains and their primary responsibilities are:
1. **Identity & Access Management (`auth`)**: User credentials, JWT session management, RBAC, password resets, and session security.
2. **Customer Management (`customer`)**: Personal profiles, KYC validation, addresses, and nominee records.
3. **Account Management (`account`)**: Savings & Current account lifecycles, balance management, freezing, closures, and statement generation.
4. **Transaction Engine (`transaction`)**: Ledger management, internal transfers, deposits, withdrawals, daily limits, duplicate transaction validation, and beneficiary records.
5. **UPI & QR Payments (`upi`)**: VPA (Virtual Payment Address) creation, bank account linking, UPI PIN verification, QR code generation/scanning, and UPI transaction orchestration.
6. **Loan Management (`loan`)**: Loan application processing, rule-based eligibility evaluation, staff approvals, EMI scheduling, and repayment tracking.
7. **AI Loan Intelligence (`ai-loan-service`)**: Standalone Python FastAPI microservice calculating risk scores, creditworthiness, repayment probabilities, and loan eligibility recommendations.
8. **Card Management (`card`)**: Debit and Credit card issuance, PIN management, card status toggles (active/blocked), and card daily limits.
9. **Notifications (`notification`)**: In-app, Email, and SMS event-driven notifications with template rendering.
10. **Audit & Compliance (`audit`)**: Immutable auditing of all sensitive administrative, customer, financial, and authentication operations.

---

## 2. Shared Value Objects

Value Objects are immutable objects defined by their attributes rather than a persistent identity.

| Value Object | Attributes & Format | Invariants / Validation Rules |
| :--- | :--- | :--- |
| **`Money`** | `BigDecimal amount`, `Currency currency` (ISO-4217, e.g., 'INR') | Amount must have precision (15,2), non-negative unless explicitly debit balance, matching currency for operations. |
| **`Phone`** | `String countryCode` (+91), `String number` (10 digits) | Format: `^\+91[6-9]\d{9}$`. Must be a valid Indian mobile number. |
| **`Email`** | `String value` | Standard RFC 5322 regex validation. Lowercase normalized. |
| **`PAN`** | `String value` | Format: `^[A-Z]{5}[0-9]{4}[A-Z]{1}$` (e.g., `ABCDE1234F`). |
| **`Aadhaar`** | `String value` (Encrypted at rest) | 12-digit numerical string passing Verhoeff algorithm check. |
| **`Address`** | `street`, `city`, `state`, `postalCode`, `country` | Postal code must match Indian PIN format `^[1-9][0-9]{5}$`. |
| **`UPI_ID`** (VPA) | `String vpa` (e.g., `user@nextgen`) | Format: `^[a-zA-Z0-9.\-_]{2,256}@[a-zA-Z]{2,64}$`. |
| **`UPI_PIN`** | `String hashedPin` | 4 or 6-digit PIN, stored as BCrypt hash. |
| **`CardNumber`** | `String maskedNumber`, `String encryptedPAN` | 16-digit PCI-DSS compliant handling. Display masked `XXXX-XXXX-XXXX-1234`. |

---

## 3. Detailed Domain Entities

### 3.1. Identity & Access (`auth`)
* **`User`**
  * `userId`: UUID (PK)
  * `username`: String (Unique)
  * `email`: Email (Unique)
  * `passwordHash`: String (BCrypt)
  * `role`: Enum (`CUSTOMER`, `BANK_STAFF`, `ADMIN`, `AUDITOR`)
  * `isActive`: Boolean
  * `failedLoginAttempts`: Integer
  * `lockoutUntil`: Instant
  * `createdAt`: Instant
  * `updatedAt`: Instant

* **`UserSession`**
  * `sessionId`: UUID (PK)
  * `userId`: UUID (FK)
  * `refreshTokenHash`: String
  * `ipAddress`: String
  * `userAgent`: String
  * `expiresAt`: Instant
  * `isRevoked`: Boolean

---

### 3.2. Customer Management (`customer`)
* **`Customer`**
  * `customerId`: UUID (PK)
  * `userId`: UUID (FK, Unique to `User`)
  * `firstName`: String
  * `lastName`: String
  * `dateOfBirth`: LocalDate
  * `phone`: Phone
  * `email`: Email
  * `kycStatus`: Enum (`PENDING`, `VERIFIED`, `REJECTED`, `EXPIRED`)
  * `riskCategory`: Enum (`LOW`, `MEDIUM`, `HIGH`)
  * `createdAt`: Instant

* **`CustomerAddress`**
  * `addressId`: UUID (PK)
  * `customerId`: UUID (FK)
  * `type`: Enum (`PERMANENT`, `COMMUNICATION`)
  * `address`: Address

* **`KYCDocument`**
  * `documentId`: UUID (PK)
  * `customerId`: UUID (FK)
  * `documentType`: Enum (`PAN`, `AADHAAR`, `PASSPORT`, `VOTER_ID`)
  * `documentNumber`: String (Encrypted)
  * `fileReference`: String
  * `verificationStatus`: Enum (`PENDING`, `APPROVED`, `REJECTED`)
  * `verifiedBy`: UUID (FK to `User` / Staff)
  * `verifiedAt`: Instant

* **`Nominee`**
  * `nomineeId`: UUID (PK)
  * `customerId`: UUID (FK)
  * `fullName`: String
  * `relationship`: String
  * `dateOfBirth`: LocalDate
  * `phone`: Phone
  * `allocationPercentage`: BigDecimal (Total across nominees = 100.00%)

---

### 3.3. Account Management (`account`)
* **`Account`**
  * `accountId`: UUID (PK)
  * `accountNumber`: String (Unique, 12-digit bank format)
  * `customerId`: UUID (FK)
  * `accountType`: Enum (`SAVINGS`, `CURRENT`)
  * `balance`: Money
  * `availableBalance`: Money (Balance minus pending holds)
  * `currency`: String (`INR`)
  * `status`: Enum (`PENDING_APPROVAL`, `ACTIVE`, `FROZEN`, `CLOSED`)
  * `freezeReason`: String
  * `dailyWithdrawalLimit`: Money
  * `dailyTransferLimit`: Money
  * `openedAt`: Instant
  * `closedAt`: Instant

* **`AccountHold`**
  * `holdId`: UUID (PK)
  * `accountId`: UUID (FK)
  * `amount`: Money
  * `reason`: String (e.g., "Loan Collateral", "Pending Cheque")
  * `status`: Enum (`ACTIVE`, `RELEASED`)
  * `createdAt`: Instant

---

### 3.4. Transaction Engine (`transaction`)
* **`Transaction`**
  * `transactionId`: UUID (PK)
  * `referenceNumber`: String (Unique, e.g., `TXN20260802123456`)
  * `sourceAccountId`: UUID (FK, Nullable for external deposit)
  * `destinationAccountId`: UUID (FK, Nullable for cash withdrawal)
  * `amount`: Money
  * `fee`: Money
  * `transactionType`: Enum (`DEPOSIT`, `WITHDRAWAL`, `INTERNAL_TRANSFER`, `UPI_TRANSFER`, `EMI_PAYMENT`)
  * `status`: Enum (`INITIATED`, `PROCESSING`, `COMPLETED`, `FAILED`, `REVERSED`)
  * `failureReason`: String
  * `idempotencyKey`: String (Unique index)
  * `narrative`: String
  * `executedAt`: Instant

* **`Beneficiary`**
  * `beneficiaryId`: UUID (PK)
  * `customerId`: UUID (FK)
  * `beneficiaryName`: String
  * `accountNumber`: String
  * `ifscCode`: String
  * `bankName`: String
  * `nickname`: String
  * `maxTransferLimit`: Money
  * `status`: Enum (`PENDING_COOLING_OFF`, `ACTIVE`, `DELETED`)
  * `addedAt`: Instant

---

### 3.5. UPI & QR Payments (`upi`)
* **`UPIProfile`**
  * `upiId`: UUID (PK)
  * `customerId`: UUID (FK)
  * `vpa`: UPI_ID (Unique, e.g., `user@nextgen`)
  * `defaultAccountId`: UUID (FK)
  * `hashedPin`: UPI_PIN
  * `status`: Enum (`ACTIVE`, `SUSPENDED`)
  * `createdAt`: Instant

* **`QRCode`**
  * `qrId`: UUID (PK)
  * `vpa`: UPI_ID
  * `merchantName`: String
  * `fixedAmount`: Money (Nullable, for dynamic QR)
  * `qrPayloadString`: String (EMVCo compliance standard)
  * `expiresAt`: Instant (Nullable for static QR)

---

### 3.6. Loan Management (`loan`)
* **`LoanApplication`**
  * `loanApplicationId`: UUID (PK)
  * `customerId`: UUID (FK)
  * `requestedAmount`: Money
  * `tenureMonths`: Integer
  * `loanType`: Enum (`PERSONAL`, `HOME`, `VEHICLE`, `EDUCATION`)
  * `purpose`: String
  * `monthlyIncome`: Money
  * `employmentType`: Enum (`SALARIED`, `SELF_EMPLOYED`, `BUSINESS`, `UNEMPLOYED`)
  * `status`: Enum (`SUBMITTED`, `AI_EVALUATED`, `STAFF_REVIEW`, `APPROVED`, `REJECTED`, `DISBURSED`)
  * `aiRiskScore`: Integer (300 - 900)
  * `aiEligibilityDecision`: Enum (`RECOMMEND_APPROVE`, `RECOMMEND_REJECT`, `MANUAL_REVIEW`)
  * `aiRepaymentProbability`: BigDecimal (0.00 - 1.00)
  * `reviewedBy`: UUID (FK to User / Staff)
  * `rejectionReason`: String
  * `appliedAt`: Instant

* **`LoanAccount`**
  * `loanAccountId`: UUID (PK)
  * `loanApplicationId`: UUID (FK, Unique)
  * `customerId`: UUID (FK)
  * `disbursedAccountId`: UUID (FK)
  * `principalAmount`: Money
  * `interestRate`: BigDecimal (Percentage, e.g., 10.50%)
  * `tenureMonths`: Integer
  * `emiAmount`: Money
  * `outstandingPrincipal`: Money
  * `status`: Enum (`ACTIVE`, `DELINQUENT`, `CLOSED`)
  * `disbursedAt`: Instant

* **`EMISchedule`**
  * `emiId`: UUID (PK)
  * `loanAccountId`: UUID (FK)
  * `installmentNumber`: Integer
  * `dueDate`: LocalDate
  * `principalComponent`: Money
  * `interestComponent`: Money
  * `totalEmi`: Money
  * `status`: Enum (`PENDING`, `PAID`, `OVERDUE`)
  * `paidAt`: Instant

---

### 3.7. AI Loan Intelligence (`ai-loan-service`) - *Standalone Python Microservice Domain Entities*
* **`CreditRiskEvaluationRequest`** (Value Object passed to Python API)
  * `cibilScore`: Integer
  * `monthlyIncome`: BigDecimal
  * `existingLoanEmiTotal`: BigDecimal
  * `repaymentHistoryScore`: Integer (0-100)
  * `employmentType`: String
  * `requestedAmount`: BigDecimal
  * `tenureMonths`: Integer

* **`CreditRiskEvaluationResult`** (Value Object returned from Python API)
  * `riskScore`: Integer (300-900)
  * `riskBand`: Enum (`LOW_RISK`, `MEDIUM_RISK`, `HIGH_RISK`)
  * `eligibility`: Enum (`RECOMMEND_APPROVE`, `RECOMMEND_REJECT`, `MANUAL_REVIEW`)
  * `repaymentProbability`: BigDecimal (0.00 - 1.00)
  * `maxRecommendedAmount`: BigDecimal
  * `scoringModelVersion`: String (`v1.0-rules`)

#### AI Eligibility Decision Mapping Matrix (Python API ↔ Java Entity)

| Python API (`CreditRiskEvaluationResult.eligibility`) | Java Entity (`LoanApplication.aiEligibilityDecision`) | Action / Business Policy |
| :--- | :--- | :--- |
| `RECOMMEND_APPROVE` | `RECOMMEND_APPROVE` | High credit score ($\ge 750$). Fast-track staff review queue. |
| `MANUAL_REVIEW` | `MANUAL_REVIEW` | Moderate credit score ($650 - 749$). Standard staff review queue. |
| `RECOMMEND_REJECT` | `RECOMMEND_REJECT` | Low credit score ($< 650$). Auto-flagged for rejection. |


---

### 3.8. Card Management (`card`)
* **`Card`**
  * `cardId`: UUID (PK)
  * `accountId`: UUID (FK)
  * `customerId`: UUID (FK)
  * `cardNumber`: CardNumber
  * `cardType`: Enum (`DEBIT`, `CREDIT`)
  * `expiryDate`: LocalDate
  * `cvvHash`: String
  * `status`: Enum (`INACTIVE`, `ACTIVE`, `BLOCKED`, `EXPIRED`, `CANCELLED`)
  * `blockReason`: Enum (`LOST`, `STOLEN`, `SUSPICIOUS_ACTIVITY`, `CUSTOMER_REQUEST`)
  * `dailyPosLimit`: Money
  * `dailyAtmLimit`: Money
  * `dailyOnlineLimit`: Money
  * `issuedAt`: Instant

---

### 3.9. Notifications (`notification`)
* **`Notification`**
  * `notificationId`: UUID (PK)
  * `recipientUserId`: UUID (FK)
  * `channel`: Enum (`IN_APP`, `EMAIL`, `SMS`)
  * `title`: String
  * `body`: String
  * `status`: Enum (`PENDING`, `SENT`, `FAILED`)
  * `sentAt`: Instant
  * `createdAt`: Instant

---

### 3.10. Audit & Compliance (`audit`)
* **`AuditLog`**
  * `auditLogId`: UUID (PK)
  * `actorId`: UUID (Nullable for system events)
  * `actorRole`: String
  * `action`: String (e.g., `CUSTOMER_KYC_VERIFIED`, `FUNDS_TRANSFERRED`, `CARD_BLOCKED`)
  * `entityName`: String
  * `entityId`: String
  * `oldValueJson`: String (JSON format)
  * `newValueJson`: String (JSON format)
  * `ipAddress`: String
  * `timestamp`: Instant

---

## 4. Domain Events

| Event Name | Domain Publisher | Payload Summary | Primary Subscribers |
| :--- | :--- | :--- | :--- |
| **`UserRegisteredEvent`** | `auth` | `userId`, `email`, `role`, `timestamp` | `customer`, `notification`, `audit` |
| **`KYCStatusChangedEvent`** | `customer` | `customerId`, `newStatus`, `verifiedBy` | `account`, `notification`, `audit` |
| **`AccountCreatedEvent`** | `account` | `accountId`, `accountNumber`, `customerId`, `type` | `notification`, `audit` |
| **`AccountStatusChangedEvent`**| `account` | `accountId`, `oldStatus`, `newStatus`, `reason` | `card`, `transaction`, `notification`, `audit` |
| **`TransactionCompletedEvent`**| `transaction` | `transactionId`, `referenceNumber`, `sourceAcc`, `destAcc`, `amount` | `account`, `notification`, `audit` |
| **`TransactionFailedEvent`** | `transaction` | `transactionId`, `referenceNumber`, `reason` | `notification`, `audit` |
| **`UPIPaymentProcessedEvent`**| `upi` | `transactionId`, `vpa`, `amount`, `status` | `notification`, `audit` |
| **`LoanSubmittedEvent`** | `loan` | `loanApplicationId`, `customerId`, `requestedAmount` | `ai-loan-service`, `notification`, `audit` |
| **`LoanEvaluatedEvent`** | `loan` | `loanApplicationId`, `aiRiskScore`, `eligibility` | `notification`, `audit` |
| **`LoanApprovedEvent`** | `loan` | `loanApplicationId`, `approvedAmount`, `staffId` | `account`, `notification`, `audit` |
| **`LoanDisbursedEvent`** | `loan` | `loanAccountId`, `disbursedAccountId`, `amount` | `transaction`, `notification`, `audit` |
| **`CardStatusUpdatedEvent`** | `card` | `cardId`, `status`, `reason` | `notification`, `audit` |

---

## 5. Domain Business Invariants & Policies

1. **KYC Required for Account Activation**: A customer profile MUST have `kycStatus == VERIFIED` before any bank account (`SAVINGS` or `CURRENT`) can transition from `PENDING_APPROVAL` to `ACTIVE`.
2. **Strict Non-Negative Balance**: Account balances (`availableBalance`) can NEVER drop below 0 for `SAVINGS` accounts. `CURRENT` accounts may permit overdrafts up to an explicitly set negative limit.
3. **Double-Entry Balance Preservation**: Every transaction debit must have an equal credit. The total delta of source account debit equals destination account credit plus transaction fees.
4. **Beneficiary Cooling-Off Period**: Newly added beneficiaries require a mandatory 30-minute cooling-off period before transfers exceeding ₹10,000 can be processed.
5. **Loan AI Evaluation Gate**: Every submitted loan application MUST be evaluated by the **AI Loan Intelligence Service** before it can be reviewed or approved by Bank Staff.
6. **Immutable Audit Logs**: Audit log entries are append-only. No system API, role, or background job may update or delete records in the `audit_logs` table.
7. **UPI PIN Verification**: Any UPI debit transaction MUST verify the BCrypt hash of the provided `UPI_PIN` against the stored `UPIProfile` before touching account balances.
