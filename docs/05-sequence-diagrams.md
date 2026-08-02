# 05. Sequence Diagrams Specification

## 1. Overview

This document presents end-to-end Mermaid sequence diagrams for critical business flows in the **NextGen Digital Banking Platform**. These diagrams illustrate interaction steps, authentication gates, cross-module communication, synchronous REST calls to the standalone Python AI service, and asynchronous domain events.

---

## 2. Customer Registration, KYC Verification & Account Opening Flow

```mermaid
sequenceDiagram
    autonumber
    actor Customer
    actor Staff as Bank Staff
    participant Auth as Auth Module (Java)
    participant Cust as Customer Module (Java)
    participant Acc as Account Module (Java)
    participant EventBus as Internal Event Bus
    participant Notif as Notification Module

    Customer->>Auth: 1. POST /api/v1/auth/register (Email, Password, Role)
    Auth->>Auth: Validate input & Hash Password (BCrypt)
    Auth-->>Customer: 2. Return 201 Created (userId, JWT Token)
    Auth->>EventBus: Publish UserRegisteredEvent

    Customer->>Cust: 3. POST /api/v1/customers/kyc (Upload PAN, Aadhaar, Address)
    Cust->>Cust: Store Documents (Pending Status)
    Cust-->>Customer: 4. Return 202 Accepted (KYC Status: PENDING)
    Cust->>EventBus: Publish KYCSubmittedEvent

    Staff->>Cust: 5. POST /api/v1/staff/kyc/verify (customerId, status: VERIFIED)
    Cust->>Cust: Update kycStatus = VERIFIED
    Cust->>EventBus: Publish KYCApprovedEvent
    Cust-->>Staff: 6. Return 200 OK (KYC Verified)

    EventBus->>Notif: Trigger Notification (Email: KYC Verified)
    
    Customer->>Acc: 7. POST /api/v1/accounts (customerId, accountType: SAVINGS)
    Acc->>Cust: Synchronous call: getKYCStatus(customerId)
    Cust-->>Acc: Return kycStatus = VERIFIED
    Acc->>Acc: Create Account (Status: ACTIVE, AccountNumber generated)
    Acc->>EventBus: Publish AccountOpenedEvent
    Acc-->>Customer: 8. Return 201 Created (accountNumber, balance: 0.00)

    EventBus->>Notif: Trigger Welcome Kit Email
```

---

## 3. Fund Transfer Flow (With Complete Validation Pipeline)

```mermaid
sequenceDiagram
    autonumber
    actor Customer
    participant Gateway as API Gateway / Auth Filter
    participant Txn as Transaction Engine
    participant Acc as Account Module
    participant EventBus as Internal Event Bus
    participant Notif as Notification Module
    participant Audit as Audit Module

    Customer->>Gateway: 1. POST /api/v1/transactions/transfer
    Note over Customer,Gateway: Headers: Authorization Bearer JWT, X-Idempotency-Key
    Gateway->>Gateway: Validate JWT Signature & RBAC (Role: CUSTOMER)
    Gateway->>Txn: Forward Request Payload

    Txn->>Txn: 2. Check Idempotency Key in DB
    alt Key Already Processed
        Txn-->>Customer: Return Cached Previous Response
    end

    Txn->>Acc: 3. Verify Source Account Active & Balance
    Acc-->>Txn: Source Active, Balance = ₹50,000

    Txn->>Acc: 4. Verify Destination Account Active
    Acc-->>Txn: Destination Active

    Txn->>Txn: 5. Validate Daily Limits & Beneficiary Cooling-off
    alt Validation Fails (e.g. Insufficient Balance / Exceeded Daily Limit)
        Txn-->>Customer: 400 Bad Request (Error Code: LIMIT_EXCEEDED / INSUFFICIENT_FUNDS)
    end

    Txn->>Acc: 6. Execute Atomic Debit & Credit Transaction (@Transactional)
    Acc->>Acc: Debit Source Account Balance (-₹5,000)
    Acc->>Acc: Credit Destination Account Balance (+₹5,000)
    Acc-->>Txn: Balance Update Success

    Txn->>Txn: 7. Mark Transaction COMPLETED & Store Reference Number
    Txn-->>Customer: 200 OK (ReferenceNumber: TXN2026080299, Status: COMPLETED)

    Txn->>EventBus: 8. Publish TransactionCompletedEvent
    
    par Async Processing
        EventBus->>Notif: Send Debit SMS to Sender & Credit SMS to Receiver
    and
        EventBus->>Audit: Append Immutable Audit Record
    end
```

---

## 4. UPI & QR Code Payment Flow

```mermaid
sequenceDiagram
    autonumber
    actor Customer
    participant MobileApp as React Web/Mobile App
    participant UPI as UPI Module
    participant Acc as Account Module
    participant Txn as Transaction Engine
    participant EventBus as Internal Event Bus

    Customer->>MobileApp: 1. Scan Merchant QR Code
    MobileApp->>MobileApp: Parse Payload String (VPA: merchant@nextgen, Amount: ₹500)
    Customer->>MobileApp: 2. Enter UPI PIN & Click Pay

    MobileApp->>UPI: 3. POST /api/v1/upi/pay (vpa, payeeVpa, amount, upiPin)
    UPI->>UPI: 4. Verify UPI PIN (BCrypt match against hashed_pin)
    alt Invalid UPI PIN
        UPI-->>MobileApp: 401 Unauthorized (Invalid UPI PIN)
    end

    UPI->>Acc: 5. Fetch Default Account linked to VPA
    Acc-->>UPI: Account ID & Available Balance

    UPI->>Txn: 6. Delegate Execution to Transaction Engine
    Txn->>Acc: Perform Balance Debit & Credit
    Acc-->>Txn: Success

    Txn-->>UPI: Return Transaction Status (COMPLETED)
    UPI-->>MobileApp: 200 OK (UPI Payment Successful, Ref: UPI2026080201)

    UPI->>EventBus: Publish UPIPaymentCompletedEvent
```

---

## 5. End-to-End Loan Flow (Application → Python AI Scoring → Approval → Disbursement → EMI)

```mermaid
sequenceDiagram
    autonumber
    actor Customer
    actor Staff as Bank Staff
    participant Loan as Loan Module (Java)
    participant AIService as AI Loan Service (Python FastAPI)
    participant Acc as Account Module (Java)
    participant EventBus as Internal Event Bus

    Customer->>Loan: 1. POST /api/v1/loans/apply (Amount: ₹2,000,000, Tenure: 24m, Income, Employment)
    Loan->>Loan: Create LoanApplication (Status: SUBMITTED)

    Loan->>AIService: 2. REST Synchronous POST /api/v1/ai/evaluate-risk
    Note over Loan,AIService: Payload: CIBIL, Monthly Income, Existing Loans, Requested Amount
    AIService->>AIService: Execute Weighted Rule Engine (Risk Score, Eligibility, Repayment Prob)
    AIService-->>Loan: 3. Return Evaluation (Risk Score: 780, Decision: RECOMMEND_APPROVE, Prob: 0.92)

    Loan->>Loan: Update LoanApplication (Status: AI_EVALUATED, aiRiskScore: 780)
    Loan-->>Customer: 4. Return 202 Accepted (Application Submitted & AI Evaluated)

    Staff->>Loan: 5. GET /api/v1/staff/loans/pending (View Pending AI-Scored Applications)
    Staff->>Loan: 6. POST /api/v1/staff/loans/{id}/approve (Status: APPROVED, Approved Amount: ₹2,000,000)
    Loan->>Loan: Update Status = APPROVED

    Staff->>Loan: 7. POST /api/v1/staff/loans/{id}/disburse (disbursedAccountId)
    Loan->>Loan: 8. Create LoanAccount & Generate 24 EMI Schedule Records

    Loan->>Acc: 9. Credit Loan Principal to Customer Account
    Acc-->>Loan: Credit Successful

    Loan->>Loan: Update Status = DISBURSED
    Loan->>EventBus: Publish LoanDisbursedEvent
    Loan-->>Staff: 10. Return 200 OK (Loan Disbursed & EMI Schedule Active)
```

---

## 6. Card Block & Unblock Flow

```mermaid
sequenceDiagram
    autonumber
    actor Customer
    participant Card as Card Module
    participant EventBus as Internal Event Bus
    participant Notif as Notification Module
    participant Audit as Audit Module

    Customer->>Card: 1. POST /api/v1/cards/{cardId}/block (reason: "LOST")
    Card->>Card: Verify Card Ownership & Active Status
    Card->>Card: Update Card Status = BLOCKED, blockReason = "LOST"
    Card-->>Customer: 2. Return 200 OK (Card Blocked Successfully)

    Card->>EventBus: 3. Publish CardStatusUpdatedEvent

    par Async Side Effects
        EventBus->>Notif: Send Immediate SMS/Email Alert: Card Blocked
    and
        EventBus->>Audit: Record Card Blocking Action
    end

    Note over Customer,Card: Customer later finds card & requests unblock

    Customer->>Card: 4. POST /api/v1/cards/{cardId}/unblock
    Card->>Card: Verify Identity / OTP
    Card->>Card: Update Card Status = ACTIVE, blockReason = NULL
    Card-->>Customer: 5. Return 200 OK (Card Reactivated)

    Card->>EventBus: 6. Publish CardStatusUpdatedEvent (ACTIVE)
```
