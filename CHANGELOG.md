# Changelog

All notable changes to the NextGen Digital Banking Platform will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]
- 2026-08-02: Executed Design System Refresh — dark-nav / warm-canvas tokens, `HorizonCard`, `AskAIBar`, updated `StatusBadge`, `Button`, `DataTable`, `FormField`, `ComponentShowcase`, and verified frontend build (`npm run build`).
- 2026-08-02: Created Docker onboarding and troubleshooting guide (`DOCKER-GUIDE.md`).
- 2026-08-02: Created project status tracking system (`PROJECT-STATUS.md`, `AGENTS.md`, `CHANGELOG.md`).
- 2026-08-02: Fixed Aadhaar VO with Verhoeff algorithm validation and added unit tests (`AadhaarTest`).
- 2026-08-02: Configured Flyway as single source of truth (`V1__initial_schema.sql`), added Flyway dependencies to `pom.xml`, and updated `docker-compose.yml` to start PostgreSQL with an empty schema.
- 2026-08-02: Scaffolded `com.nextgen.bank.common` shared package, backend folder boundaries, Python `ai-loan-service`, and frontend domain modules.
- 2026-08-02: Generated 12 technical design documents in `/docs`.
