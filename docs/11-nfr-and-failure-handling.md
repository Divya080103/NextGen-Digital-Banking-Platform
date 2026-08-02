# 11. Non-Functional Requirements & Failure Handling Specification

## 1. Non-Functional Requirements (NFRs)

### 1.1. Security Requirements
* **Data in Transit**: TLS 1.3 encryption required for all external HTTP communication.
* **Data at Rest**: AES-256 encryption for sensitive columns (`document_number_enc` in KYC, CVV hashes in Card table).
* **Password Hashing**: BCrypt with cost factor 12.
* **OWASP Top 10 Protections**:
  * Input sanitization on all REST DTOs using Jakarta Validation constraints (`@NotNull`, `@Size`, `@Pattern`).
  * Parameterized SQL queries via Spring Data JPA to eliminate SQL injection.
  * CORS headers restricted to authorized React frontend origins.

### 1.2. Performance & Latency Targets
* **Authentication & Balance Inquiry**: 95th percentile response time $< 100\text{ ms}$.
* **Fund Transfers & UPI Payments**: 95th percentile response time $< 500\text{ ms}$.
* **AI Risk Evaluation Call**: 95th percentile response time $< 1500\text{ ms}$.
* **Database Connection Pool**: HikariCP configured with maximum pool size of 20 connections per instance.

### 1.3. Maintainability & Code Quality
* **Package Isolation**: Monolith packages enforce strict encapsulation. Packages cannot import internal repository or service implementation classes of other domains.
* **Layered Architecture**: `Controller` $\rightarrow$ `Service Interface` $\rightarrow$ `Service Implementation` $\rightarrow$ `Repository` $\rightarrow$ `Entity`.
* **Boundary Objects**: DTOs used for all API boundaries; entities never returned directly.

---

## 2. Failure Scenarios & Resolution Strategies

### Failure Scenario 1: System Crash After Debit But Before Credit
* **Problem**: Transaction Engine debits Source Account, but server crashes before Destination Account is credited.
* **Resolution Strategy (ACID Transaction Boundaries)**:
  * Debit and Credit methods are wrapped in a single `@Transactional(rollbackFor = Exception.class)` boundary in Spring.
  * Database connection holds uncommitted row locks. If the JVM or server crashes mid-execution, PostgreSQL automatically detects connection loss and performs a complete **Database Rollback**.
  * Neither account balance is modified. The transaction state remains untouched.

### Failure Scenario 2: Network Timeout During Fund Transfer (Client Retry)
* **Problem**: Client initiates transfer; server completes execution, but network drops before HTTP 200 response reaches client. Client retries request.
* **Resolution Strategy (Idempotency Key Validation)**:
  * Client sends `X-Idempotency-Key: <UUID>` header with every transfer.
  * Before processing, `TransactionEngine` checks `txn_transactions` for `idempotency_key`.
  * When key is found, server skips debit/credit and returns the stored successful response payload (`200 OK`) instantly.
  * Eliminates duplicate debits.

### Failure Scenario 3: Python AI Loan Intelligence Service Outage
* **Problem**: Java `loan` module calls `POST http://ai-loan-service:8000/api/v1/ai/evaluate-risk`, but Python service is down or times out.
* **Resolution Strategy (Resilience Fallback Evaluator)**:
  * Java HTTP client is configured with a 1500 ms read timeout.
  * If timeout or HTTP 5xx occurs, `Resilience4j` circuit breaker catches `RestClientException` and invokes `FallbackRuleEvaluator`.
  * Fallback evaluator assigns:
    * `aiRiskScore = 650`
    * `aiEligibilityDecision = MANUAL_REVIEW`
    * `isFallback = true`
  * Loan application proceeds to Bank Staff review queue without crashing or blocking the customer application process.

### Failure Scenario 4: JVM Crash After Transaction Commit or Notification Provider Failure
* **Problem**: Transaction succeeds, but server crashes immediately after commit (or email/SMS notification provider fails).
* **Resolution Strategy (Transactional Outbox Pattern)**:
  * Domain events (e.g. `TransactionCompletedEvent`) are written to `outbox_events` table in the **same database transaction** as the financial debit/credit.
  * If the JVM crashes right after commit, the event remains stored in PostgreSQL with status `PENDING`.
  * On JVM restart (or via outbox worker polling), the relay worker fetches `PENDING` outbox records and dispatches them to listeners, updating status to `PROCESSED`.
  * If an external notification provider fails, the notification listener catches the exception, marks the record as `FAILED` in `notif_notifications`, and a background scheduler retries up to 3 attempts.
  * Ensures zero event loss and at-least-once delivery guarantees across crashes.

### Failure Scenario 5: Duplicate Payment Attack (Rapid Double-Clicking)
* **Problem**: User rapidly clicks "Pay" twice within 500 ms, issuing twin requests.
* **Resolution Strategy (Database Row Locking & Idempotency)**:
  * Database queries for source account balance use `SELECT ... FOR UPDATE` row-level locks.
  * Second concurrent transaction waits for first lock release.
  * Second transaction fails duplicate transaction validation (same source, dest, amount within 120s) and returns `409 CONFLICT`.
