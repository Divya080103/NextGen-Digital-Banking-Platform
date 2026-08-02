# Agentic Coding Guidelines — NextGen Digital Banking Platform

This file contains mandatory instructions for any AI coding assistant or agent (Antigravity, Claude, Copilot, Cursor, etc.) working on this repository.

---

## 1. Required Reading Before Making Changes

Before inspecting code or writing any implementation:
1. Read `PROJECT-STATUS.md` to understand the current phase, completed components, and active blockers.
2. Read `TEAM-KICKOFF.md` for tool versions, folder assignments, and build phase checkpoints.
3. Read `DESIGN-SYSTEM.md` for UI tokens, typography rules, accessibility requirements, and signature components.
4. Read the relevant technical specifications in `/docs` (12 design files).

---

## 2. Status Tracking & Integrity Rules

- **Maintain `PROJECT-STATUS.md`**: After completing any meaningful unit of work, update the relevant row(s) in `PROJECT-STATUS.md` and update the `Last updated:` header line.
- **Strict Verification Rule**: Only mark a status as **Verified** if an actual terminal command was run and its real output was observed. If a command fails or is blocked (e.g. Docker daemon not active), state explicitly: `"Not verified — blocked by X"`. **NEVER fabricate command output or format expected output to look like a real execution log.**
- **Maintain `CHANGELOG.md`**: Append one dated bullet line to `CHANGELOG.md` for every completed unit of work (newest entries on top).

---

## 3. Architecture & Domain Boundaries

- Do not modify files outside your assigned domain package without explicit alignment (refer to `TEAM-KICKOFF.md` Section 4).
- The `com.nextgen.bank.common` package is shared across all backend modules. Do not make breaking changes to `common` without whole-team agreement.
- Database schema changes must be added as new Flyway migration scripts in `backend/src/main/resources/db/migration/` (e.g., `V2__<description>.sql`). Never alter existing applied migrations.
