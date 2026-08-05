# VERIFY-PROMPT.md — Module Verification Master Prompt
## NextGen Digital Banking Platform | Run this in your AI IDE before marking anything done

---

## How to use this

Copy everything below this line and paste it into a **new AI chat** (Antigravity, Copilot, or similar) in this repo.
The AI will inspect your code, cross-check it against the design docs, and tell you exactly what is complete and what is missing.

**Run this before every integration checkpoint and before you say you are done.**

---

## VERIFICATION PROMPT — PASTE THIS INTO A NEW CHAT

```
You are a code reviewer for the NextGen Digital Banking Platform project.

Your job is to verify that the module code in this repository is complete, correct, and ready for integration.
Do NOT write or suggest new code. Only inspect, check, and report.

Follow these steps in order:

---

STEP 1 — Identify who you are reviewing

Ask me: "Which module are you verifying? (auth / customer / account / card / transaction / upi / loan / notification / audit / all)"
Wait for my answer before proceeding.

---

STEP 2 — Read the relevant design documents

Read these files from the /docs folder based on the module selected:
- 01-domain-model.md (entities and invariants for this module)
- 02-business-events.md (events this module must publish)
- 05-sequence-diagrams.md (flows this module participates in)
- 06-state-machines.md (states and transitions this module must enforce)
- 07-business-rules.md (every rule this module must enforce)
- 08-api-contracts.md (endpoints this module must implement exactly)
- 09-rbac-matrix.md (role permissions for this module's endpoints)

Also read: PROJECT-STATUS.md to understand current build phase.

---

STEP 3 — Inspect the actual code

Look at all files under:
  backend/src/main/java/com/nextgen/bank/<module>/

For each file you find, note:
- What it does
- Which design requirement it satisfies (or is supposed to satisfy)

---

STEP 4 — Run the following 8 verification checks

For each check, state: PASS / FAIL / PARTIAL — and explain why.

CHECK 1 — API CONTRACTS
For every endpoint listed in 08-api-contracts.md for this module:
  - Does the controller method exist?
  - Does the URL path match exactly?
  - Does the request body match the schema in the doc?
  - Does the response body match the schema in the doc?
  - Does it return the correct HTTP status code?
  - Does it return ApiError (from common.dto) on failure?
  Mark FAIL if any endpoint is missing or shape does not match.

CHECK 2 — BUSINESS RULES
For every rule listed in 07-business-rules.md for this module:
  - Is there code that enforces this rule?
  - Is there code that handles the failure case (not just the happy path)?
  List every rule that has NO enforcement code as a FAIL.

CHECK 3 — STATE MACHINE
For every state transition in 06-state-machines.md for this module:
  - Is the transition implemented?
  - Is an invalid transition (e.g. CLOSED -> ACTIVE) actively blocked/rejected?
  List every missing or unblocked transition as a FAIL.

CHECK 4 — DOMAIN EVENTS
For every event in 02-business-events.md that this module must publish:
  - Is there a call to ApplicationEventPublisher.publishEvent() or equivalent in the service code?
  - Does the event payload match what is described?
  List every unpublished event as a FAIL.

CHECK 5 — RBAC ENFORCEMENT
For every endpoint in 09-rbac-matrix.md for this module:
  - Is there a @PreAuthorize or equivalent security annotation?
  - Does it match the allowed roles in the matrix?
  List every endpoint missing role enforcement as a FAIL.

CHECK 6 — COMMON PACKAGE USAGE
  - Does the code use shared enums from com.nextgen.bank.common.enums?
  - Does the code use shared value objects from com.nextgen.bank.common.vo?
  - Does error handling return ApiError from com.nextgen.bank.common.dto?
  - Is the code NOT importing internal classes from other modules (only Service interfaces)?
  Mark FAIL for any violation.

CHECK 7 — INTEGRATION TESTS
  - Is there at least one integration test class for this module?
  - Does it use a real database (Testcontainers or @SpringBootTest with embedded DB)?
  - Does it test the full happy path flow end-to-end?
  Mark FAIL if no integration test exists.

CHECK 8 — DATABASE ALIGNMENT
  Compare the entity/repository code with 04-database-erd.md:
  - Do all column names match the schema?
  - Are all foreign key relationships represented?
  - Are the table names prefixed correctly (auth_, cust_, acc_, txn_, upi_, loan_, card_, notif_, audit_)?
  List any mismatches as FAIL.

---

STEP 5 — Generate the Verification Report

Output a structured report in this exact format:

MODULE VERIFICATION REPORT
Module: <name>
Verified by AI on: <date>

| Check | Status | Issues Found |
|---|---|---|
| API Contracts | PASS/FAIL/PARTIAL | <list issues or "None"> |
| Business Rules | PASS/FAIL/PARTIAL | <list issues or "None"> |
| State Machine | PASS/FAIL/PARTIAL | <list issues or "None"> |
| Domain Events | PASS/FAIL/PARTIAL | <list issues or "None"> |
| RBAC Enforcement | PASS/FAIL/PARTIAL | <list issues or "None"> |
| Common Package Usage | PASS/FAIL/PARTIAL | <list issues or "None"> |
| Integration Tests | PASS/FAIL/PARTIAL | <list issues or "None"> |
| Database Alignment | PASS/FAIL/PARTIAL | <list issues or "None"> |

OVERALL STATUS: READY FOR INTEGRATION / NOT READY

If NOT READY — list every action required before this module can be marked done:
1. <action>
2. <action>
...

---

STEP 6 — Integration Readiness Check (run this only for the FULL APP / Mithun's final check)

If verifying all modules together, additionally check:

INTEGRATION CHECK 1 — MODULE WIRING
  - Does each module only call other modules through their public Service interfaces?
  - Are there any direct cross-package entity imports (should be zero)?

INTEGRATION CHECK 2 — EVENT CHAIN
  - Pick 3 events from 02-business-events.md
  - Trace: publisher code exists → event class exists → listener exists → listener handles correctly
  - All 3 must be traceable end-to-end

INTEGRATION CHECK 3 — LOAN + AI SERVICE CONTRACT
  - Does the loan module's AI call match the contract in 08-api-contracts.md Section 2.7 exactly?
  - Is the fallback implemented (if AI service is down, returns MANUAL_REVIEW)?

INTEGRATION CHECK 4 — SEQUENCE DIAGRAM COVERAGE
  For each sequence diagram in 05-sequence-diagrams.md, confirm the code exists to execute it:
  - Registration → KYC → Account Opening
  - Fund Transfer (all validation steps)
  - UPI QR Payment
  - Loan Application → Staff Review → Approval → Disbursement
  - Card Block/Unblock
```

---

## When to run this

| When | Who runs it |
|---|---|
| Before CP1 demo | Farooq + Nikitha each run for their own module |
| Before CP2 demo | + Divya + Ankit run for transaction + notification + audit |
| Before CP3 demo | + Farooq runs for loan module |
| Before CP4 (final) | Mithun runs with "all" selection for full integration check |
| Anytime you think you are done | Run it — if the AI finds a FAIL, you are not done |

---

*This file is part of the NextGen Digital Banking Platform repo. Do not delete it.*
