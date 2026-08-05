# NextGen Digital Banking Platform — Team Kickoff Brief
### Version 1.0 | Lead: Mithun | Hard Deadline: 15 August 2026

---

> **Read this fully before you write a single line of code.**
> This document covers your assignment, what to build, what NOT to build, how to work together, and what "done" means.
> The design work is complete. Your job is execution.

---

## 1. What We Are Building

**NextGen Digital Banking Platform** — a production-grade digital banking app with:

- Customer registration, login, KYC verification
- Savings & current accounts, debit/credit cards
- Fund transfers, deposits, withdrawals, daily limits
- UPI payments with QR code generation and scanning
- Loan applications with AI-powered risk scoring
- Real-time notifications (email, SMS, in-app)
- Full audit trail for every sensitive action

**The full design (all decisions already made) is in the `/docs` folder — 12 documents.** Read your relevant docs before coding. Do not redesign anything — scope is frozen.

---

## 2. Team Assignment

| Name | Backend Modules | Key Responsibility | Depends On |
|---|---|---|---|
| **Mithun** *(Lead)* | `common` package — already done | Integration, PR reviews, Docker setup, AI loan service (Python) added **last** after all modules work | Nobody — you unblock everyone else |
| **Farooq** | `auth`, `customer` → then `loan` | JWT login, KYC workflow, loan applications + manual approval | Mithun's `common` (already ready) |
| **Nikitha** | `account`, `card` | Account open/freeze/close, card block/unblock | Farooq's customer/KYC module |
| **Divya + Ankit** *(together)* | `transaction`, `upi` | Fund transfers, beneficiaries, UPI QR payments | Nikitha's account module |
| **Ankit** *(solo part)* | `notification`, `audit` | Event listeners, email/SMS/in-app alerts, audit log | Triggered by everyone's events — can start early |

### Divya + Ankit: How to split Transaction

You two own this module together. Here is the suggested split — **you are free to adjust it, but agree before you start and tell Mithun**:

| Person | What to build |
|---|---|
| **Divya** | Core transaction engine: deposit, withdrawal, fund transfer, balance debit/credit, daily limits, duplicate detection, idempotency |
| **Ankit** | Beneficiary management (add/edit/delete/verify), UPI transfer layer on top of transaction engine |

If Ankit wants to use Kafka for the notification/audit event pipeline instead of the default Spring in-memory events — that decision is yours to make together. The design uses Spring `ApplicationEventPublisher` (simpler, no extra infra). If you want Kafka, add the Kafka Docker container to `docker-compose.yml` and inform Mithun. Either approach is fine — just don't switch halfway through.

---

## 3. Build Order — This Sequence Matters

Do **not** build in isolation for 2 weeks and merge at the end. That is how integration fails.

```
Day 1        : Mithun confirms Docker DB is running for everyone → share DOCKER-GUIDE.md
Days 1-5     : Farooq builds Auth + Customer | Nikitha builds Account + Card stubs
Days 3-7     : Divya + Ankit build Transaction core | Ankit starts Notification + Audit listeners
Days 6-10    : Farooq builds Loan (manual approve only) | Divya + Ankit finish UPI
Days 10-14   : Integration week — everything wires together, Mithun reviews and fixes gaps
Day 15       : DEMO DAY — full working flow, end-to-end, in front of everyone
After Day 15 : Mithun adds AI loan scoring on top of the working system
```

---

## 4. The Four Integration Checkpoints

**"I finished my module" is not accepted without a running demo.**
These 4 checkpoints are mandatory — screen-share, actual data flowing, no slides.

| Checkpoint | What runs live | Who must be ready |
|---|---|---|
| **CP1** (~Day 5) | Customer registers → KYC submitted → Staff approves KYC → Account opens and shows ACTIVE | Farooq + Nikitha |
| **CP2** (~Day 8) | Fund transfer: money leaves Account A, lands in Account B, notification fires, audit log entry created | + Divya + Ankit |
| **CP3** (~Day 12) | Loan applied → Staff manually approves → EMI schedule generated → Disbursement hits customer account | + Farooq (loan) |
| **CP4** (Day 15) | Full app: register → KYC → account → transfer → UPI payment → loan → full audit trail | All 5 |

**If a checkpoint fails, work continues until it passes. The deadline does not move.**

---

## 5. What to Read Before You Code

### Farooq
- `docs/01-domain-model.md` — Auth, Customer, Loan sections
- `docs/05-sequence-diagrams.md` — Registration + KYC + Account Opening flow; Loan flow
- `docs/06-state-machines.md` — Loan lifecycle
- `docs/07-business-rules.md` — Auth rules, Loan rules
- `docs/09-rbac-matrix.md` — Who can do what

### Nikitha
- `docs/01-domain-model.md` — Account, Card sections
- `docs/05-sequence-diagrams.md` — Account Opening flow; Card Block/Unblock flow
- `docs/06-state-machines.md` — Account lifecycle, Card lifecycle
- `docs/07-business-rules.md` — Account rules

### Divya + Ankit
- `docs/01-domain-model.md` — Transaction, UPI sections
- `docs/05-sequence-diagrams.md` — Fund Transfer flow (all validation steps); UPI QR Payment flow
- `docs/07-business-rules.md` — Transaction rules (daily limits, duplicate checks, rollback)
- `docs/11-nfr-and-failure-handling.md` — What happens when a transfer crashes halfway
- `docs/02-business-events.md` — Every event your module must publish

### Ankit (notification + audit)
- `docs/02-business-events.md` — This is your entire job description. Every event listed here must have a listener.
- `docs/07-business-rules.md` — Audit rules
- `docs/09-rbac-matrix.md` — What gets audited

### Mithun
- All 12 docs (you own the big picture)
- `docs/03-service-boundaries.md` — The wiring between modules
- `docs/04-database-erd.md` — Full schema, all tables
- `docs/08-api-contracts.md` — Internal contract between Loan module and Python AI service
- `docs/11-nfr-and-failure-handling.md` — Fallback if AI service is down

---

## 6. The Rules That Keep Integration From Breaking

**Rule 1: Stay in your package.**
Your code lives in `com.nextgen.bank.<yourmodule>`. Do not import classes from another person's internal package. Call their public `Service` interface only.

**Rule 2: Your API must match `08-api-contracts.md` exactly.**
Same URL. Same request/response field names. Same HTTP status codes. Same error shape (ApiError from `common`). The frontend connects to these — if your endpoint doesn't match, the frontend breaks.

**Rule 3: Every state change must follow the state machine in `06-state-machines.md`.**
No skipping states. No adding your own states. Enums are already in `common/enums/`.

**Rule 4: Every sensitive action must fire an event.**
See `02-business-events.md`. Every event your module must publish — publish it. Ankit's listeners depend on this. If you don't publish the event, notifications and audit don't work.

**Rule 5: `common` package is read-only.**
Do not modify `com.nextgen.bank.common` without telling Mithun first.

---

## 7. Tech Stack — Match These Exactly

| Tool | Version | Note |
|---|---|---|
| Java | 21 (LTS) | Do not use 17 or 22 |
| Spring Boot | 3.3.4 | Already in `pom.xml` — do not bump |
| Maven | 3.9.x | `mvn clean install` to build |
| PostgreSQL | 16.x | Docker only — see DOCKER-GUIDE.md |
| Node.js | 20.x LTS | Frontend |
| React | 18.3.x | Use `npm ci` not `npm install` |
| Python | 3.11 | AI service only — Mithun's territory |

Run `docker-compose up -d` from the repo root on Day 1. Do not install Postgres locally.

---

## 8. Frontend — Backend First

**Build the working backend API first. Then connect the frontend.**

Each person builds the frontend screens for their own module. Use **only** the shared components in `frontend/src/components/`: HorizonCard, AskAIBar, StatusBadge, Button, DataTable, FormField. Follow `DESIGN-SYSTEM.md`. One-off colours or fonts will be rejected in PR review.

---

## 9. Git Workflow

- `main` = always working and demo-able
- `develop` = everyone merges here first
- Branch naming: `feature/<module>-<short-desc>` e.g. `feature/auth-jwt-login`
- One PR per feature, one teammate reviews before merge to `develop`
- Mithun reviews all cross-module and `common` changes

---

## 10. Definition of Done — Per Person Checklist

**Do not say you are done until ALL of these are true for your module:**

- [ ] Every endpoint in `08-api-contracts.md` returns the correct response shape and HTTP status code
- [ ] Every state machine transition in `06-state-machines.md` is implemented — no invalid transitions are possible
- [ ] Every business rule in `07-business-rules.md` is enforced — test failure cases, not just happy path
- [ ] Every event your module must publish (per `02-business-events.md`) is published and Ankit has confirmed his listener receives it
- [ ] At least the happy path has a working integration test with the real Postgres DB running via Docker
- [ ] The code compiles cleanly: `mvn clean install` passes with zero errors
- [ ] At least one teammate has reviewed and approved your PR
- [ ] You have personally run the relevant sequence diagram from `05-sequence-diagrams.md` end-to-end — it works, not "should work"

**If any of the above is false, you are not done.**

---

## 11. Loan Module — Manual First, AI Later

When a customer applies for a loan (before Mithun integrates AI):

1. Customer submits application → saved with status `UNDER_REVIEW`
2. Staff reviews and manually approves or rejects via staff portal
3. On approval → EMI schedule generated → disbursement amount hits customer's bank account (calls Transaction module)
4. Customer tracks loan status and EMI due dates

No AI scoring until Mithun adds it at the end. Farooq, build the manual workflow cleanly. Mithun slots the AI call in without touching your other code.

---

## 12. Summary — Who Does What by 15 Aug

| Name | Must be complete by 15 August |
|---|---|
| **Mithun** | Docker + DB working for team; all PRs reviewed; app runs end-to-end; AI integration after |
| **Farooq** | Auth (register, login, JWT, password reset) + Customer (profile, KYC, nominee) + Loan (apply, manual approve, EMI, repayment tracking) |
| **Nikitha** | Account (open, freeze, close, statement download) + Card (issue, block, unblock, limits) |
| **Divya** | Transaction core (deposit, withdrawal, transfer, daily limits, idempotency) + UPI (QR generate/scan, UPI pay, UPI PIN) |
| **Ankit** | Beneficiaries (add/edit/delete, part of transaction module) + Notification (all event listeners) + Audit (immutable log for every sensitive action) |

**Demo day is 15 August. The full flow runs live. If it doesn't run, it's not done.**

---

*Lead: Mithun | Design docs: /docs/ folder (12 files) | Verification: Run VERIFY-PROMPT.md in your AI IDE before marking anything done*
