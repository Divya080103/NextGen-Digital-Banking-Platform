# Project Status
Last updated: 2026-08-12 by Claude (Divya's modules)

## Current Phase
Phase 1: Module Development — Account & Card built; Transaction & UPI built end-to-end (backend + frontend UI) and verified running in a browser against a live Postgres

## Component Status
| Component | Status | Notes |
|---|---|---|
| `common` package | Verified | Enums, Money, Phone, Email, PAN, Aadhaar (Verhoeff validated), ApiError DTO, Outbox pattern compiled and passed 3 unit tests (`mvn test`) |
| Database schema (Flyway) | Verified | `V1__initial_schema.sql` + `V2__upi_pin_lockout.sql` applied to live Postgres 16.14. Flyway log: "Successfully applied 2 migrations to schema "public", now at version v2". Hibernate `ddl-auto: validate` passed, so all entity mappings match the live schema |
| `docker-compose.yml` | Configured | Postgres 16 container, empty schema init, Flyway owns schema |
| `DOCKER-GUIDE.md` | Verified | Docker onboarding guide & troubleshooting documentation created |
| `TEAM-MESSAGE.md` | Verified | Team kickoff brief & module assignments |
| `VERIFY-PROMPT.md` | Verified | AI module verification master prompt |
| `AI-SESSION-STARTER.md` | Verified | AI IDE session starter prompt for developer onboarding |
| `WORKFLOW-GUIDE.html` | Verified | Interactive 4-step light-mode HTML workflow guide featuring Antigravity <-> ChatGPT iteration loop, 1-click prompt copiers, and human-in-the-loop developer rules |
| `PROJECT-STATUS.md` & `AGENTS.md` | Verified | Status tracking & AI agent instruction system created |
| frontend shared components | Verified | Refreshed dark-nav / warm-canvas design system, `HorizonCard`, `AskAIBar`, `StatusBadge`, `Button`, `DataTable`, `FormField`. Passed production build (`npm run build`) |
| `auth` module | Not started | Assigned: Teammate 1 (Farooq) |
| `customer` module | Not started | Assigned: Teammate 1 (Farooq) |
| `account` module | Built | Complete backend services, entity state machine, holds, statements, outbox events, and REST controller (`/api/v1/accounts`) |
| `card` module | Built | Complete backend services, card entity, activation/block/unblock, limits, outbox events, REST controller (`/api/v1/cards`), and frontend UI module (`frontend/src/modules/account-card`) |
| `transaction` module | Verified end-to-end against live DB | Deposit/withdraw/transfer/reverse + history. Live HTTP smoke test confirmed: idempotent replay returns the identical `transactionId` with no double debit (BR-TXN-001); ₹50k single-txn cap rejects ₹60k (BR-TXN-002); daily ceiling accepts a cumulative ₹100,000.00 exactly and rejects the next rupee (BR-TXN-002); 120s duplicate shield returns 409 (BR-TXN-004); balances move atomically (BR-TXN-005); FAILED records persist through rollback via the REQUIRES_NEW recorder and appear in history. Assigned: Divya |
| `upi` module | Verified end-to-end against live DB | Profile create, PIN change, pay, QR generate/scan. Live HTTP smoke test confirmed: non-`@nextgen` VPA rejected (BR-UPI-001); wrong PIN increments the counter through rollback, a correct PIN resets it, the 3rd failure sets `pin_locked_until` exactly 24h out and a correct PIN then returns 423 (BR-UPI-002) — the change-PIN endpoint feeds the same counter, so it is not a brute-force bypass; daily count ceiling rejects the 11th payment (BR-UPI-003); forged/unissued QR payloads rejected and amount tampering on a genuine QR rejected (BR-UPI-004). Fund movement delegated to the Transaction Engine. Outbox rows observed: 11 `UPIPaymentCompletedEvent`, 1 `UPIPINChangedEvent`. Assigned: Divya |
| `transaction` frontend module | Verified in browser | `frontend/src/modules/transaction` — account picker, balance card, deposit/withdraw/transfer, editable `X-Idempotency-Key`, history table. A deposit driven from the UI moved the live balance ₹1,60,013.00 → ₹1,65,013.00 and returned `TXN202608121305011127`. Assigned: Divya |
| `upi` frontend module | Verified in browser | `frontend/src/modules/upi-notification-audit` — profile create, send money, change PIN, QR generate/scan. Verified in the browser: QR generate → scan renders payee/merchant/amount; a hand-edited payload surfaces `QR_NOT_RECOGNIZED`; a payment from `ankit@nextgen` → `divya@nextgen` succeeded; `divya@nextgen` correctly returns `UPI_DAILY_COUNT_EXCEEDED` after 10 payments in a day. Assigned: Divya |
| app shell | Verified in browser | `frontend/src/App.jsx` — tab navigation (Transactions / UPI / Design system), acting-customer switcher, `/actuator/health` poll driving an API UP/DOWN badge. Vite dev proxy forwards `/api` and `/actuator` to `localhost:8080` |
| `loan` module | Not started | Assigned: Teammate 4 (Farooq) |
| `notification` module | Not started | Assigned: Teammate 5 (Ankit) |
| `audit` module | Not started | Assigned: Teammate 5 (Ankit) |
| `ai-loan-service` | Scaffolding done | Bare FastAPI app in `ai-loan-service/app/main.py` with `/health` endpoint |
| frontend shared components | Shared library built | `LedgerCard`, `StatusBadge`, `Button`, `DataTable`, `FormField` |

## Local Toolchain (verified 2026-08-12)
- JDK 21 (`openjdk 21.0.12`, keg-only) and Maven 3.9.16 installed via Homebrew. `openjdk@21` is not symlinked, so `export JAVA_HOME=/opt/homebrew/opt/openjdk@21` is required.
- Postgres 16 running via `docker compose up -d` (`nextgen-postgres`); `pg_isready` confirmed accepting connections with an empty schema for Flyway.
- `mvn compile` → **BUILD SUCCESS** (92 source files, Java 21 release target).
- `mvn test` → **BUILD SUCCESS**, `Tests run: 51, Failures: 0, Errors: 0, Skipped: 0`.
- `mvn spring-boot:run` → **`Started NextGenBankingApplication`**, Tomcat on 8080, `/actuator/health` returns `{"status":"UP"}` with `db: UP`.
- Node 26.7.0 / npm 11.19.0 installed via Homebrew. `npm install` → 0 vulnerabilities blocking; `npm run dev` → **`VITE v5.4.21 ready`** on 5173, app served and rendering live API data.
- `brew install node` left `ca-certificates` half-installed (its post-install step is killed inside Homebrew's sandbox), so no CA bundle existed and every `npm install` failed with `UNABLE_TO_GET_ISSUER_CERT_LOCALLY`. Fixed by running the formula's own post-install by hand and linking it where OpenSSL looks:
  ```bash
  /opt/homebrew/Cellar/ca-certificates/*/libexec/post-install \
    /opt/homebrew/Cellar/ca-certificates/*/share/ca-certificates/cacert.pem \
    /opt/homebrew/etc/ca-certificates/cert.pem
  ln -sfn /opt/homebrew/etc/ca-certificates/cert.pem /opt/homebrew/etc/openssl@3/cert.pem
  ```

## How to run locally
```bash
cd backend
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
docker compose -f ../docker-compose.yml up -d     # Postgres 16
mvn spring-boot:run
```
Until the `auth` module lands, Spring Boot's default security blocks every `POST` (its CSRF filter runs before Basic auth, so unauthenticated POSTs come back as `401`). To exercise the APIs, start the app with security auto-config off — runtime only, nothing committed:
```bash
SPRING_AUTOCONFIGURE_EXCLUDE="org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,\
org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration,\
org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration,\
org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration" \
  mvn spring-boot:run
```
Then start the UI in a second terminal and open <http://localhost:5173>:
```bash
cd frontend
npm install
npm run dev
```
Vite proxies `/api` and `/actuator` to `localhost:8080`, so the browser stays same-origin and the backend needs no CORS config.

`acc_accounts.customer_id` is FK-constrained to `cust_profiles`, so opening an account requires a customer row. Until the `customer` module exists, seed one directly (`docker exec -i nextgen-postgres psql -U bank_admin -d nextgen_bank`) — an `auth_users` row first, then a `cust_profiles` row referencing it.

## Known Issues / Blockers
- **Slow Maven Central throughput**: the first dependency resolution failed after 32 min with `Read timed out` (transfers as low as tens of B/s). Re-running with `-Dmaven.wagon.rto=600000 -Dmaven.wagon.http.retryHandler.count=10` succeeded because Maven resumes from the local cache. Use those flags on a cold `~/.m2`.
- **Three pre-existing compile breaks fixed** (the repo did not compile before this work):
  1. `common`: `ResourceNotFoundException` had only a 3-arg constructor while `account`/`card` call a single-arg form in 6 places — added an additive `ResourceNotFoundException(String)`.
  2. `common`: no `PasswordEncoder` bean existed, but `card` and `upi` both inject one (Spring Security does not auto-configure it) — added `common/config/PasswordEncoderConfig` (BCrypt). Without this the app cannot boot.
  3. `account`: `Account.getBalanceAsMoney()`/`getAvailableBalanceAsMoney()` passed `Currency.getInstance(currency)` into `Money(BigDecimal, String)` — 2 type errors. Now pass the `String` currency directly; removed the unused `java.util.Currency` import.
  Flag all three for whole-team awareness per AGENTS.md §3.
- **New migration `V2__upi_pin_lockout.sql`**: adds `pin_failed_attempts`, `pin_locked_until`, `created_at` to `upi_profiles` (required by BR-UPI-002 lockout + entity mapping) plus UPI indexes. Additive only. Applied successfully to the live DB.
- **Auth not wired**: `@PreAuthorize` annotations are in place on transaction/UPI controllers but enforcement depends on Farooq's `auth` SecurityConfig (not yet present). Nothing enforces roles today — `@EnableMethodSecurity` is absent, so every annotation is inert.
- **`PasswordEncoder` bean changes the default dev login** (whole-team awareness): now that `common` exposes a BCrypt `PasswordEncoder`, Spring Boot's `UserDetailsServiceAutoConfiguration` stops prefixing the default in-memory user's password with `{noop}`, so a plaintext `spring.security.user.password` can never match. Farooq's `SecurityConfig` should own this; until then use the auto-config exclusion documented above rather than fighting the default user.
- **`account-card` frontend module does not compile** (owner: Nikhita, flagged not fixed — outside Divya's domain per AGENTS.md §3). `frontend/src/modules/account-card/AccountDashboard.jsx:2` does `import LedgerCard from '../../components/LedgerCard'`, but `components/LedgerCard.jsx` exports only `LedgerCard` and `LedgerCardExample` — there is no default export. esbuild fails the whole dependency scan on it (`No matching export in "src/components/LedgerCard.jsx" for import "default"`), which takes every other screen down with it. The Accounts & Cards tab is therefore left out of `App.jsx` until the import is corrected to `import { LedgerCard } from …` (or `HorizonCard`, which `components/index.js` also aliases as `LedgerCard`).
- **Smoke-test data is present in the local DB**: two seeded customers (`Divya Payer`, `Ankit Payee`), their accounts, UPI profiles `divya@nextgen` / `ankit@nextgen`, and ~30 transactions. Recreate the DB (`docker compose down -v && docker compose up -d`) for a clean slate.
- **BR-UPI-003 daily count trips easily on the seeded data**: the counter is per *source account* and resets at UTC midnight (05:30 IST). `divya@nextgen` has already spent 10/10 for 2026-08-12, so every payment from Divya correctly returns `UPI_DAILY_COUNT_EXCEEDED` — the rule working, not a fault. Pay from `ankit@nextgen` (3/10) or wait for the UTC day to roll over. The UI has no "used today" indicator yet, which makes this look like a failure; worth adding a counter to the Send money panel.
- **UPI PIN state was reset for local usability**: both profiles were left PIN-locked until 2026-08-13 by the BR-UPI-002 lockout evidence, and their PINs had been rotated by the change-PIN tests, so neither could be used from the UI. Both now have `hashed_pin` = BCrypt(`1234`) with the counters cleared, written directly with SQL. `divya@nextgen` has already used its 10 daily UPI payments, so pay from `ankit@nextgen` today or wait for the UTC day to roll over.

## Next Steps
1. Farooq completes Auth & Customer modules — this unblocks real RBAC enforcement (`@EnableMethodSecurity`) and removes the need to seed `cust_profiles` rows by hand.
2. Ankit hooks Event Listeners to the Notification and Audit log pipeline; the outbox is already producing `TransactionInitiated/Completed/Failed`, `UPIPaymentCompleted`, `UPIPINChanged`, and `AccountOpened` events.
3. Ankit builds Beneficiaries (out of scope for Divya's transaction/UPI work).
4. Nikhita fixes the `LedgerCard` default import in `AccountDashboard.jsx` (see Known Issues); the Accounts & Cards tab can then be restored to `App.jsx` in one line.

