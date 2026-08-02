# 09. Role-Based Access Control (RBAC) Matrix Specification

## 1. Overview

The **NextGen Digital Banking Platform** enforces strict Role-Based Access Control (RBAC) using Spring Security filters. Every HTTP endpoint is secured by role permissions.

### Primary User Roles:
1. **`CUSTOMER`**: Retail customer accessing personal banking, accounts, transactions, cards, loans, and UPI.
2. **`BANK_STAFF`**: Operational bank employee handling KYC document validation, loan review/approval, and account freeze actions.
3. **`ADMIN`**: System administrator managing user access, role assignments, staff onboarding, and system parameters.
4. **`AUDITOR`**: Compliance officer requiring read-only access to audit logs, transaction histories, and regulatory reports.

---

## 2. Master RBAC Endpoint & Action Matrix

| Domain Module | API Endpoint / Business Action | HTTP Method | Customer | Bank Staff | Admin | Auditor | Notes / Constraints |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| **Auth** | `/api/v1/auth/register` | `POST` | ✅ | ✅ | ✅ | ❌ | Public registration for customers. |
| **Auth** | `/api/v1/auth/login` | `POST` | ✅ | ✅ | ✅ | ✅ | Authenticates any valid user. |
| **Auth** | `/api/v1/auth/password-reset` | `POST` | ✅ | ✅ | ✅ | ❌ | Self-service password reset. |
| **Auth** | `/api/v1/admin/users/{id}/role` | `PUT` | ❌ | ❌ | ✅ | ❌ | Admin-only role assignment. |
| **Customer**| `/api/v1/customers/profile` | `GET` | ✅ | ✅ | ✅ | ✅ | Customer accesses own profile; Staff/Admin/Auditor view by ID. |
| **Customer**| `/api/v1/customers/kyc` | `POST` | ✅ | ❌ | ❌ | ❌ | Customer uploads own KYC docs. |
| **Customer**| `/api/v1/staff/kyc/verify` | `POST` | ❌ | ✅ | ✅ | ❌ | Staff verifies uploaded KYC. |
| **Account** | `/api/v1/accounts` | `POST` | ✅ | ✅ | ✅ | ❌ | Requires verified KYC status. |
| **Account** | `/api/v1/accounts/{id}` | `GET` | ✅ | ✅ | ✅ | ✅ | Customer views own account; Staff views any. |
| **Account** | `/api/v1/accounts/{id}/freeze` | `POST` | ❌ | ✅ | ✅ | ❌ | Staff/Admin emergency account freeze. |
| **Account** | `/api/v1/accounts/{id}/unfreeze`| `POST` | ❌ | ✅ | ✅ | ❌ | Requires documented clearance log. |
| **Account** | `/api/v1/accounts/{id}/close` | `POST` | ✅ | ✅ | ❌ | ❌ | Zero balance check required. |
| **Transaction**| `/api/v1/transactions/transfer`| `POST` | ✅ | ❌ | ❌ | ❌ | Self-service fund transfer. |
| **Transaction**| `/api/v1/transactions/history` | `GET` | ✅ | ✅ | ✅ | ✅ | Filtered by owner for Customer. |
| **Transaction**| `/api/v1/staff/transactions/reverse`| `POST` | ❌ | ✅ | ✅ | ❌ | Reverses transaction in error. |
| **Beneficiary**| `/api/v1/beneficiaries` | `POST` | ✅ | ❌ | ❌ | ❌ | Add payee with 30m cooling-off. |
| **UPI** | `/api/v1/upi/profile` | `POST` | ✅ | ❌ | ❌ | ❌ | Create VPA and set UPI PIN. |
| **UPI** | `/api/v1/upi/pay` | `POST` | ✅ | ❌ | ❌ | ❌ | Execute UPI QR or VPA payment. |
| **Loan** | `/api/v1/loans/apply` | `POST` | ✅ | ❌ | ❌ | ❌ | Submits application to Python AI. |
| **Loan** | `/api/v1/staff/loans/pending` | `GET` | ❌ | ✅ | ✅ | ❌ | Staff queue of AI-evaluated loans. |
| **Loan** | `/api/v1/staff/loans/{id}/approve`| `POST` | ❌ | ✅ | ✅ | ❌ | Staff approves sanctioned loan. |
| **Loan** | `/api/v1/staff/loans/{id}/disburse`| `POST` | ❌ | ✅ | ✅ | ❌ | Credits principal to bank account. |
| **Card** | `/api/v1/cards/issue` | `POST` | ✅ | ✅ | ❌ | ❌ | Issue new debit or credit card. |
| **Card** | `/api/v1/cards/{id}/block` | `POST` | ✅ | ✅ | ✅ | ❌ | Emergency card blocking. |
| **Card** | `/api/v1/cards/{id}/limits` | `PUT` | ✅ | ✅ | ❌ | ❌ | Customer can lower daily limits. |
| **Notifications**| `/api/v1/notifications` | `GET` | ✅ | ✅ | ✅ | ❌ | View user alerts. |
| **Audit** | `/api/v1/audit/logs` | `GET` | ❌ | ❌ | ✅ | ✅ | View immutable compliance logs. |
| **Audit** | `/api/v1/audit/reports` | `GET` | ❌ | ❌ | ❌ | ✅ | Compliance & regulatory exports. |

---

## 3. Data-Level Access Control (Ownership Validation)

In addition to endpoint-level role checks, the service layer enforces strict **Object-Level Security**:
* **Rule**: When a request authenticated as `CUSTOMER` invokes an endpoint (e.g. `GET /api/v1/accounts/acc-123`), the service verifies:
  $$\text{Account.customerId} == \text{SecurityContext.authenticatedCustomerId}$$
* **Violation**: If a customer attempts to query or modify an account owned by another customer, the service immediately throws `AccessDeniedException` resulting in HTTP `403 FORBIDDEN` and logs an security incident to `audit_logs`.
