# 02. Business Events & Event Storming Specification

## 1. Overview

In the **NextGen Digital Banking Platform**, domain events capture significant state changes across business boundaries. Although implemented as a **Modular Monolith**, business logic is loosely coupled by publishing internal domain events via an in-memory event publisher (`ApplicationEventPublisher` in Spring Boot).

This document serves as the Event Storming record, outlining every major domain event, its publisher, consumers, triggering actions, and side effects.

---

## 2. Master Event Catalog

### 2.1. Authentication & Security Events (`auth`)

#### `UserRegisteredEvent`
* **Trigger**: A new user completes registration via `POST /api/v1/auth/register`.
* **Publisher**: `auth` (Authentication Module)
* **Payload**:
  ```json
  {
    "eventId": "evt_101a0b12-1111-4000-a000-000000000001",
    "userId": "usr_98765432-1111-4000-a000-000000000001",
    "email": "john.doe@example.com",
    "role": "CUSTOMER",
    "timestamp": "2026-08-02T19:00:00Z"
  }
  ```
* **Subscribers & Actions**:
  * **`customer`**: Initializes a draft Customer profile linked to `userId`.
  * **`notification`**: Sends a welcome email containing account setup instructions.
  * **`audit`**: Records user registration audit log.

#### `UserLoggedInEvent`
* **Trigger**: User successfully authenticates via `POST /api/v1/auth/login`.
* **Publisher**: `auth`
* **Payload**: `userId`, `ipAddress`, `userAgent`, `timestamp`
* **Subscribers & Actions**:
  * **`audit`**: Logs login activity with IP address and user-agent.
  * **`notification`**: Sends login alert email/SMS if login occurs from a new IP/device.

#### `PasswordChangedEvent`
* **Trigger**: User changes or resets their password.
* **Publisher**: `auth`
* **Payload**: `userId`, `timestamp`
* **Subscribers & Actions**:
  * **`notification`**: Sends immediate security alert notification.
  * **`audit`**: Logs security credential modification.

---

### 2.2. Customer & KYC Events (`customer`)

#### `KYCSubmittedEvent`
* **Trigger**: Customer uploads identity documents (`PAN`, `Aadhaar`).
* **Publisher**: `customer`
* **Payload**: `customerId`, `documentTypes`, `submittedAt`
* **Subscribers & Actions**:
  * **`notification`**: Notifies customer that KYC documents are under review. Notifies Bank Staff dashboard queue.
  * **`audit`**: Logs document submission.

#### `KYCApprovedEvent`
* **Trigger**: Bank Staff approves customer KYC via `POST /api/v1/staff/kyc/verify`.
* **Publisher**: `customer`
* **Payload**: `customerId`, `verifiedByStaffId`, `verifiedAt`
* **Subscribers & Actions**:
  * **`account`**: Enables account creation eligibility for customer.
  * **`notification`**: Sends KYC verification confirmation email/SMS.
  * **`audit`**: Records compliance audit trail for KYC approval.

#### `KYCRejectedEvent`
* **Trigger**: Bank Staff rejects customer KYC documents.
* **Publisher**: `customer`
* **Payload**: `customerId`, `rejectionReason`, `rejectedByStaffId`, `timestamp`
* **Subscribers & Actions**:
  * **`notification`**: Notifies customer with explicit reason and re-submission link.
  * **`audit`**: Logs compliance failure.

---

### 2.3. Account Events (`account`)

#### `AccountOpenedEvent`
* **Trigger**: Bank Staff or system approves and opens a new savings/current account.
* **Publisher**: `account`
* **Payload**: `accountId`, `accountNumber`, `customerId`, `accountType`, `openedAt`
* **Subscribers & Actions**:
  * **`notification`**: Sends account welcome pack with account number and IFSC.
  * **`audit`**: Logs account opening.

#### `AccountFrozenEvent`
* **Trigger**: Staff or security automated checks trigger account freeze.
* **Publisher**: `account`
* **Payload**: `accountId`, `reason`, `frozenByUserId`, `timestamp`
* **Subscribers & Actions**:
  * **`card`**: Temporarily suspends active debit/credit cards linked to account.
  * **`upi`**: Deactivates VPA transfers linked to account.
  * **`notification`**: Notifies customer about account suspension.
  * **`audit`**: Records emergency freeze action.

#### `AccountClosedEvent`
* **Trigger**: Account closure request processed after zero-balance verification.
* **Publisher**: `account`
* **Payload**: `accountId`, `customerId`, `closedAt`
* **Subscribers & Actions**:
  * **`card`**: Cancels all associated cards.
  * **`upi`**: Deletes VPAs linked to account.
  * **`notification`**: Sends account closure confirmation.
  * **`audit`**: Logs account lifecycle termination.

---

### 2.4. Transaction Engine Events (`transaction`)

#### `TransactionInitiatedEvent`
* **Trigger**: Transfer or payment request received and idempotency check passed.
* **Publisher**: `transaction`
* **Payload**: `transactionId`, `referenceNumber`, `sourceAccountId`, `destinationAccountId`, `amount`, `idempotencyKey`
* **Subscribers & Actions**:
  * **`audit`**: Logs transaction initiation.

#### `TransactionCompletedEvent`
* **Trigger**: Account balances successfully debited and credited atomically.
* **Publisher**: `transaction`
* **Payload**:
  ```json
  {
    "eventId": "evt_202a0b12-2222-4000-a000-000000000002",
    "transactionId": "txn_12345678-2222-4000-a000-000000000002",
    "referenceNumber": "TXN20260802998877",
    "sourceAccountId": "acc_11112222-3333-4000-a000-000000000001",
    "destinationAccountId": "acc_44445555-6666-4000-a000-000000000002",
    "amount": 5000.00,
    "currency": "INR",
    "transactionType": "INTERNAL_TRANSFER",
    "completedAt": "2026-08-02T19:05:00Z"
  }
  ```
* **Subscribers & Actions**:
  * **`notification`**: Triggers real-time debit alert to sender and credit alert to receiver (Email/SMS/Push).
  * **`audit`**: Appends immutable transaction ledger record.

#### `TransactionFailedEvent`
* **Trigger**: Insufficient funds, limit violation, or system error during transaction execution.
* **Publisher**: `transaction`
* **Payload**: `transactionId`, `sourceAccountId`, `amount`, `failureReason`, `failedAt`
* **Subscribers & Actions**:
  * **`notification`**: Sends transaction failure notification to customer with reason.
  * **`audit`**: Records transaction failure for monitoring.

---

### 2.5. UPI & QR Events (`upi`)

#### `UPIPINChangedEvent`
* **Trigger**: Customer sets or changes their UPI PIN via `POST /api/v1/upi/pin`.
* **Publisher**: `upi`
* **Payload**: `vpa`, `customerId`, `timestamp`
* **Subscribers & Actions**:
  * **`notification`**: Sends security notification regarding UPI PIN change.
  * **`audit`**: Logs authentication credential update.

#### `UPIPaymentCompletedEvent`
* **Trigger**: UPI QR or VPA payment successfully executed.
* **Publisher**: `upi`
* **Payload**: `upiTransactionId`, `vpa`, `merchantOrPayee`, `amount`, `referenceNumber`
* **Subscribers & Actions**:
  * **`notification`**: Triggers instant UPI transaction push alert.
  * **`audit`**: Logs UPI transaction detail.

---

### 2.6. Loan & AI Intelligence Events (`loan`)

#### `LoanAppliedEvent`
* **Trigger**: Customer submits a loan application via `POST /api/v1/loans/apply`.
* **Publisher**: `loan`
* **Payload**: `loanApplicationId`, `customerId`, `requestedAmount`, `employmentType`, `monthlyIncome`
* **Subscribers & Actions**:
  * **`loan` (Internal)**: Triggers REST call to **AI Loan Intelligence Service** (`ai-loan-service`).
  * **`notification`**: Sends loan application acknowledgment.
  * **`audit`**: Logs loan application submission.

#### `LoanAIEvaluatedEvent`
* **Trigger**: Response received from AI Loan Intelligence Service.
* **Publisher**: `loan`
* **Payload**: `loanApplicationId`, `aiRiskScore`, `aiEligibilityDecision`, `aiRepaymentProbability`
* **Subscribers & Actions**:
  * **`loan`**: Updates loan application record with AI decision and routes to Staff queue.
  * **`notification`**: Notifies customer of application status update.
  * **`audit`**: Logs AI risk score and evaluation payload.

#### `LoanApprovedEvent`
* **Trigger**: Bank Staff approves loan after reviewing AI score and documents.
* **Publisher**: `loan`
* **Payload**: `loanApplicationId`, `approvedAmount`, `approvedByStaffId`, `interestRate`, `tenureMonths`
* **Subscribers & Actions**:
  * **`notification`**: Sends formal loan offer letter notification.
  * **`audit`**: Logs staff approval action.

#### `LoanDisbursedEvent`
* **Trigger**: Staff executes loan disbursement to customer's account.
* **Publisher**: `loan`
* **Payload**: `loanAccountId`, `disbursedAccountId`, `principalAmount`, `disbursedAt`
* **Subscribers & Actions**:
  * **`transaction`**: Credits loan amount directly into customer's `SAVINGS`/`CURRENT` account.
  * **`notification`**: Sends credit notification for loan disbursement.
  * **`audit`**: Records financial disbursement log.

---

### 2.7. Card Management Events (`card`)

#### `CardBlockedEvent`
* **Trigger**: Customer or Staff blocks a card (e.g. lost, stolen, or suspicious).
* **Publisher**: `card`
* **Payload**: `cardId`, `maskedNumber`, `reason`, `blockedAt`
* **Subscribers & Actions**:
  * **`notification`**: Sends emergency card block confirmation alert.
  * **`audit`**: Logs card status modification.

---

## 3. Event Handling Architecture (Transactional Outbox Pattern)

To guarantee **at-least-once event delivery** across JVM crashes, network dropouts, or database rollbacks, the system uses the **Transactional Outbox Pattern**.

```mermaid
flowchart TD
    subgraph Core Transaction [Atomic Database Transaction]
        DOM[Domain Service Logic] -->|1. Write Entity State| DB_ENTITY[(Domain Tables)]
        DOM -->|2. Insert Event Record| DB_OUTBOX[(outbox_events Table)]
    end

    subgraph Event Relay Worker [Spring Outbox Publisher / Scheduler]
        DB_OUTBOX -->|3. Query PENDING Events| RELAY[Outbox Relay Worker]
        RELAY -->|4. Publish Event| BUS((Spring ApplicationEventPublisher))
        RELAY -->|5. Mark PROCESSED| DB_OUTBOX
    end

    subgraph Consumer Handlers [Module Event Listeners]
        BUS -->|@Async| NOTIF_LISTENER[notification: NotificationEventListener]
        BUS -->|@Async| AUDIT_LISTENER[audit: AuditEventListener]
        BUS -->|@Async| ACC_LISTENER[account: AccountEventListener]
    end
```

### Event Reliability Principles:
1. **Atomic Transactional Persistence**: Every domain event is written to `outbox_events` in the **exact same database transaction** as the core business mutation (e.g., account debit/credit). If the business transaction fails, the outbox record is automatically rolled back.
2. **Crash Resilience**: If the application crashes immediately after database transaction commit, the event remains saved with status `PENDING` in PostgreSQL. Upon application restart (or via background outbox polling), the worker relays pending events to consumers.
3. **At-Least-Once Delivery & Idempotency**: Consumers (listeners) are designed to be idempotent using event ID checks (`eventId`) to safely handle potential duplicate event deliveries.

