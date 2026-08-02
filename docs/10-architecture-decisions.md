# 10. Architecture Decision Records (ADRs)

## ADR-001: Modular Monolith Architecture over Microservices

### Status
**APPROVED**

### Context
The platform requires core banking features (accounts, transactions, loans, cards, UPI) built by a team of 5 developers over a multi-month period. A distributed microservices architecture introducing 10-12 independent services would add extreme overhead: distributed transactions (Saga pattern), API gateway routing, service discovery, distributed tracing, and complex Kubernetes infrastructure.

### Decision
Build the system as a **Modular Monolith** in Java / Spring Boot using clean domain packages (`auth`, `customer`, `account`, `transaction`, `upi`, `loan`, `card`, `notification`, `audit`).
* **Exception**: Host **AI Loan Intelligence** as a single dedicated Python FastAPI microservice due to specialized ML library requirements.

### Consequences
* **Positive**: Fast local development, single unit deployment, simple ACID database transactions, no network latency between core modules.
* **Negative**: Scaling is monolithic (entire application scales together). Requires strict package boundary enforcement to avoid turning into a "spaghetti monolith".

---

## ADR-002: Single PostgreSQL Database with Schema Prefixes

### Status
**APPROVED**

### Context
Financial transactions require strict ACID compliance, relational foreign key constraints, atomic balance debits/credits, and complex SQL reporting.

### Decision
Use **PostgreSQL** as the primary relational database engine. Prefix tables by domain module (e.g., `auth_`, `cust_`, `acc_`, `txn_`, `loan_`) to ensure logical schema isolation.

### Consequences
* **Positive**: Full ACID guarantees, row-level locking for concurrent transactions, familiar tooling, zero distributed transaction overhead.
* **Negative**: High write load on a single database instance requires proper indexing and connection pooling.

---

## ADR-003: Technology Stack (Spring Boot + React + Python FastAPI)

### Status
**APPROVED**

### Context
We need a robust, enterprise-grade backend for financial logic, an interactive web UI for customers and staff, and a flexible ecosystem for AI risk scoring.

### Decision
1. **Core Backend**: Java 21 + Spring Boot 3.x (Spring Security, Spring Data JPA, Web).
2. **Frontend**: React.js with TypeScript and Vite.
3. **AI Loan Service**: Python 3.11 + FastAPI (Pydantic, NumPy, Scikit-Learn).

### Consequences
* **Positive**: Java offers high type-safety and performance for transactions; React provides a rich UI; Python enables rapid AI model iteration.
* **Negative**: Team must maintain two runtime environments (Java JVM and Python venv).

---

## ADR-004: JWT with Refresh Tokens for Stateless Authentication

### Status
**APPROVED**

### Context
The system requires secure authentication across React web clients and mobile endpoints without server-side HTTP session state.

### Decision
Implement **Stateless JWT (JSON Web Tokens)**. Access tokens expire in 15 minutes; Refresh tokens are stored securely in database table `auth_user_sessions` with 7-day validity.

### Consequences
* **Positive**: Stateless validation at API filters; easy role assertion in token claims.
* **Negative**: Immediate token revocation requires checking a revoked refresh token list.

---

## ADR-005: REST APIs (OpenAPI 3.0) over GraphQL

### Status
**APPROVED**

### Context
Banking operations require strict API contracts, predictable caching, standard HTTP status codes, and idempotency headers for payments.

### Decision
Use **RESTful APIs** documented via OpenAPI 3.0 (Swagger) rather than GraphQL.

### Consequences
* **Positive**: Clear URL semantics, standard HTTP status codes (`200`, `201`, `400`, `401`, `403`, `409`), simple Postman/Swagger testing.
* **Negative**: Slightly higher payload size for complex nested queries compared to GraphQL.

---

## ADR-006: Transactional Outbox Pattern with Spring Event Bus

### Status
**APPROVED**

### Context
Core operations (transfers, loan approvals) need decoupled side effects (sending notifications, appending audit logs) without blocking main thread responses. Purely in-memory event buses lose events if the server crashes immediately after database transaction commit before event handlers process the message.

### Decision
Implement the **Transactional Outbox Pattern**. Domain events are atomically written to the `outbox_events` table within the primary business transaction. A lightweight Spring outbox relay worker reads `PENDING` outbox records and dispatches them via Spring's `ApplicationEventPublisher` to `@Async` listeners, updating event status to `PROCESSED`.

### Consequences
* **Positive**: Guarantees **at-least-once event delivery** across JVM crashes without requiring an external Kafka or RabbitMQ broker for v1; zero event loss during crashes.
* **Negative**: Requires extra database inserts per business transaction and minor outbox table cleanup jobs over time.
