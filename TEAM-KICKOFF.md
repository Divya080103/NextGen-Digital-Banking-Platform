# NextGen Digital Banking Platform — Team Kickoff & Build Plan

Share this doc (plus the `/docs` folder and `DESIGN-SYSTEM.md`) with the team before the meeting. Everyone reads it beforehand; the meeting is for assigning, agreeing, and resolving questions — not reading it together live.

---

## 1. Pinned tool versions (everyone matches this exactly)

| Tool | Version | Why pin it |
|---|---|---|
| Java | 21 (LTS) | Spring Boot 3.3.x targets this |
| Spring Boot | 3.3.4 | via `pom.xml` — do not bump individually |
| Maven | 3.9.x | build tool |
| Node.js | 20.x LTS | frontend |
| React | 18.3.x | pinned in `package.json`, `npm ci` not `npm install` |
| PostgreSQL | 16.x | local dev via Docker, see below |
| Python | 3.11 | AI service, see `requirements.txt` |
| Git | any recent | workflow matters more than version |

Run everything Dockerized where possible so "works on my machine" isn't a category of bug this team has time for. At minimum, Postgres should run via Docker Compose so all 5 people have identical schema/data, not 5 local installs drifting apart.

---

## 2. Repo folder structure (create this shape first, empty is fine)

```
NextGen-Digital-Banking-Platform/
├── docs/                          # already done — the 12 design docs
├── DESIGN-SYSTEM.md               # already done
├── requirements.txt               # AI service deps — see below, place inside ai-loan-service/
├── backend/
│   ├── src/main/java/com/nextgen/bank/
│   │   ├── common/                # ⚠️ BUILD THIS FIRST, see section 3
│   │   ├── auth/
│   │   ├── customer/
│   │   ├── account/
│   │   ├── card/
│   │   ├── transaction/
│   │   ├── upi/
│   │   ├── loan/
│   │   ├── notification/
│   │   └── audit/
│   └── pom.xml
├── ai-loan-service/
│   ├── app/
│   ├── requirements.txt
│   └── main.py
└── frontend/
    └── src/
        ├── styles/design-system.css     # already done
        ├── components/                  # already done — shared library
        └── modules/                      # per-domain screens, one folder per teammate
            ├── auth-customer/
            ├── account-card/
            ├── transaction/
            ├── loan/
            └── upi-notification-audit/
```

---

## 3. Build this before anyone starts their module: the `common` package

This is the single most important step and it's easy to skip in the excitement of splitting work. In a modular monolith with 5 people working in parallel, if everyone defines their own version of shared things, integration day becomes a disaster. Before module work starts, **one person (or a 30-minute group session) builds and merges `com.nextgen.bank.common`** containing:

- Shared enums that appear in `01-domain-model.md` across multiple modules: `AccountStatus`, `TransactionStatus`, `LoanStatus`, `CardStatus`, `KYCStatus`, the AI eligibility enum
- The domain event base classes and the `outbox_events` write helper (from doc 02/04) — since almost every module publishes events, this needs to exist before module code does
- Shared value objects: `Money`, `Phone`, `Email`, `PAN`, `Aadhaar` (from doc 01, section 2)
- The standard API error response DTO and exception-handling base classes, so every module returns errors in the same shape
- The `X-Idempotency-Key` and `X-Correlation-ID` handling filters (from docs 08 and 12)

**Nobody writes module code until `common` is merged to `main`.** After that, it's frozen unless the whole team agrees to change it — a shared package that keeps shifting under 5 people is worse than no shared package.

---

## 4. Module assignment (matches the rebalanced `03-service-boundaries.md`)

| Teammate | Backend folder(s) | Frontend folder | AI service | Depends on |
|---|---|---|---|---|
| **1** | `backend/.../auth/`, `.../customer/` | `frontend/.../modules/auth-customer/` | — | `common` only — build first, others depend on this |
| **2** | `backend/.../account/`, `.../card/` | `frontend/.../modules/account-card/` | — | Teammate 1 (KYC status check before account activation) |
| **3** | `backend/.../transaction/` | `frontend/.../modules/transaction/` | — | Teammate 2 (account balance/status) |
| **4** | `backend/.../loan/` | `frontend/.../modules/loan/` | `ai-loan-service/` (Python, own repo folder) | Teammate 3 (disbursement triggers a transaction) |
| **5** | `backend/.../upi/`, `.../notification/`, `.../audit/` | `frontend/.../modules/upi-notification-audit/` | — | Teammate 3 (UPI transfers use transaction engine); listens to everyone's events |

Each teammate works **only inside their own folder(s)** plus read-only reference to `common`. If you need something from another module, you call its public `Service` interface (per doc 03) — you do not reach into their package internals. That boundary is what makes 5 people able to work in parallel without stepping on each other.

---

## 5. Git workflow

- `main` = always deployable/demoable
- `develop` = integration branch, everyone merges here first
- Branch naming: `feature/<module>-<short-desc>`, e.g. `feature/auth-jwt-login`, `feature/transaction-fund-transfer`
- One PR per feature, at least one teammate reviews before merge to `develop`
- Merge order matters at first: `common` → Teammate 1 → Teammate 2 → Teammate 3 → Teammate 4/5 in parallel, since later modules depend on earlier ones existing (even as stubs)

---

## 6. Build order & integration checkpoints

Don't let everyone build in isolation for 6 weeks and integrate once at the end — that's when the design-doc mismatches you can't see yet will surface, all at once, right before a deadline.

**Phase 0 (before anyone splits):** `common` package merged. Postgres schema for all tables in `04-database-erd.md` created via one migration script everyone runs.

**Phase 1 (Teammates 1–2 lead, others can start stubs):**
Integration checkpoint: Register → KYC approve → Account created → Account ACTIVE. This is the first sequence diagram in doc 05 — get it working end-to-end with the real UI components before moving on.

**Phase 2 (Teammate 3 joins in):**
Integration checkpoint: a real fund transfer between two accounts, with the idempotency key and row-locking behavior from doc 11 actually working, not just modeled.

**Phase 3 (Teammates 4 and 5 in parallel):**
Integration checkpoint: Loan application → AI service call (or fallback if it's down, per doc 11 scenario 3) → staff approval → disbursement → money actually lands in the account (calls Teammate 3's transaction module). Separately: UPI transfer working, and a notification actually firing off the outbox relay when any of the above happens.

**Phase 4 (everyone):**
Full audit trail check — pick any 3 random actions from earlier phases and confirm they show up correctly in the audit log. This is the cheapest phase to skip and the most expensive to skip badly, since it's evidence your whole event/outbox chain actually works, not just individual modules.

Schedule a short (15–20 min) integration check-in at the end of each phase, all 5 people, screen-share the actual working flow — not a status update, an actual demo of the sequence diagram happening for real.

---

## 7. Kickoff meeting agenda (45–60 min)

1. **(5 min)** Quick framing: what we're building, why scope is frozen where it is (point to the design docs, don't re-litigate scope live)
2. **(10 min)** Walk through `DESIGN-SYSTEM.md` and the built component library together — everyone should see the actual components running once (e.g. `npm run storybook` or open `ComponentShowcase.jsx`) before going off to build screens
3. **(10 min)** Assign modules per section 4 — confirm everyone's clear on their folder and their one upstream dependency
4. **(5 min)** Agree on the `common` package — who builds it (or do it live as a group right after this meeting), and that nobody starts module code before it's merged
5. **(10 min)** Git workflow: branch names, PR review rule, merge order
6. **(5 min)** Set the 4 integration checkpoint dates on the calendar now, not later
7. **(5–10 min)** Open floor for questions on the design docs — better to surface confusion now than mid-sprint

---

## 8. Definition of done (per module, before merging to `develop`)

- Endpoints match `08-api-contracts.md` exactly (path, request/response shape, status codes)
- State transitions match `06-state-machines.md` (no skipping states, no invalid transitions allowed)
- Every action that should emit an event does, into the outbox, per `02-business-events.md`
- Every sensitive action is audit-logged per the RBAC matrix in `09-rbac-matrix.md`
- At least the happy path has an integration test (Testcontainers per `12-testing-and-observability.md`)
- UI built using the shared component library — no one-off colors or fonts
