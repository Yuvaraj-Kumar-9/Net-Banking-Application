# HFDC Bank — API Reference

Base URL (local): `http://localhost:8080/api`

Interactive Swagger UI: `http://localhost:8080/swagger-ui.html`
OpenAPI JSON: `http://localhost:8080/v3/api-docs`

> HFDC Bank is a fictional educational simulation. All money and customer data
> are synthetic.

---

## 1. Conventions

- All endpoints are JSON (`Content-Type: application/json`).
- Money is a JSON number with two decimal places, backed by `DECIMAL(19,2)` and
  Java `BigDecimal`. Never send floats with more than two decimals.
- Dates use ISO-8601: `2026-11-17`, `2026-11-17T14:30:00`.
- Successful list endpoints return either a JSON array or a pagination envelope.
- Errors always use the same shape:

```json
{
  "timestamp": "2026-11-17T15:29:30.037Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid transaction PIN",
  "path": "/api/transactions/withdraw"
}
```

- `401` means no valid session; `403` means the CSRF token is missing/invalid or
  access is denied; `404` is used for both missing resources and resources that
  belong to another customer (no information leak).

---

## 2. Authentication and CSRF

The API is stateless. A successful login sets two cookies:

| Cookie | HttpOnly | Purpose |
| --- | --- | --- |
| `hfdc_token` | **Yes** | Signed JWT (1-day lifetime). JavaScript cannot read it. |
| `XSRF-TOKEN` | No | Double-submit CSRF token, readable by the SPA. |

Every **state-changing** request (`POST`, `PUT`, `PATCH`, `DELETE`) under `/api`
must send the value of `XSRF-TOKEN` in the `X-XSRF-TOKEN` header. The two values
are compared in constant time. Requests authenticated with an
`Authorization: Bearer <jwt>` header are exempt because browsers cannot attach
that header automatically.

### Browser flow

1. `GET /api/auth/csrf` — issues `XSRF-TOKEN`.
2. `POST /api/auth/login` with `X-XSRF-TOKEN` — sets `hfdc_token` and a fresh
   `XSRF-TOKEN`.
3. Call protected endpoints with cookies (`withCredentials` in Axios).
4. `POST /api/auth/logout` with `X-XSRF-TOKEN` — clears both cookies.

### curl example

```bash
BASE=http://localhost:8080/api
JAR=/tmp/hfdc-cookies.txt

# 1. CSRF token
TOKEN=$(curl -s -c "$JAR" "$BASE/auth/csrf" | sed -E 's/.*"csrfToken":"([^"]+)".*/\1/')

# 2. Login
curl -s -b "$JAR" -c "$JAR" \
  -H 'Content-Type: application/json' \
  -H "X-XSRF-TOKEN: $TOKEN" \
  -d '{"email":"aarav@hfdcbank.test","password":"Demo@1234"}' \
  "$BASE/auth/login"

# 3. Protected read
curl -s -b "$JAR" "$BASE/accounts/balance"

# 4. Protected write (reuse the current XSRF cookie value)
CSRF=$(grep XSRF-TOKEN "$JAR" | awk '{print $7}')
curl -s -b "$JAR" \
  -H 'Content-Type: application/json' \
  -H "X-XSRF-TOKEN: $CSRF" \
  -d '{"amount":500.00,"description":"Demo deposit"}' \
  "$BASE/transactions/deposit"
```

### Bearer alternative (Postman/Swagger)

Log in once, read the `hfdc_token` cookie value, then send
`Authorization: Bearer <token>`. CSRF is skipped for Bearer requests.

---

## 3. Authentication endpoints

### `POST /auth/register`

Creates a customer and their default `SAVINGS` account with a zero balance.
Returns **201**. Does **not** log the user in; call `/auth/login` afterwards.
Requires a CSRF header.

```json
{
  "fullName": "Meera Iyer",
  "email": "meera@example.test",
  "mobileNumber": "9876500011",
  "dateOfBirth": "1995-08-21",
  "address": "22 Lake Road, Bengaluru",
  "password": "Strong@123",
  "confirmPassword": "Strong@123",
  "transactionPin": "2580"
}
```

Response:

```json
{
  "email": "meera@example.test",
  "fullName": "Meera Iyer",
  "customerId": "CID1000000003",
  "accountNumber": "HFDC7523512987",
  "message": "Registration successful. Welcome to HFDC Bank."
}
```

Errors: `400` validation/password mismatch, `409` duplicate email or mobile.

---

### `POST /auth/login`

```json
{ "email": "aarav@hfdcbank.test", "password": "Demo@1234" }
```

Response **200** plus `Set-Cookie: hfdc_token=...; HttpOnly` and `XSRF-TOKEN`:

```json
{
  "email": "aarav@hfdcbank.test",
  "fullName": "Aarav Sharma",
  "customerId": "CID1000000001",
  "accountNumber": "HFDC1000000001",
  "message": "Login successful"
}
```

Errors: `400` invalid email/password (same message for both), inactive account.

---

### `POST /auth/logout`

Clears both cookies. Returns:

```json
{ "message": "You have been logged out" }
```

---

### `GET /auth/csrf`

Public. Sets `XSRF-TOKEN` and returns it:

```json
{ "csrfToken": "fitbEt3dHXfRelvAejii2Q4fkqKo-2TO0Wfdx_m-0b4" }
```

---

### `GET /auth/me` — protected

Returns the current customer summary. Used by the SPA to restore a session.

```json
{
  "email": "aarav@hfdcbank.test",
  "fullName": "Aarav Sharma",
  "customerId": "CID1000000001",
  "accountNumber": "HFDC1000000001",
  "message": "authenticated"
}
```

---

### `POST /auth/forgot-password`

```json
{ "email": "aarav@hfdcbank.test" }
```

Always returns **200** with a generic message to prevent account enumeration. In
`DEV_MODE=true` the response also contains `resetToken`:

```json
{
  "message": "A password reset token was generated (demo mode).",
  "resetToken": "0f1e2d...64-hex-characters"
}
```

With `DEV_MODE=false`, `resetToken` is `null` and the token would be emailed by a
real provider (not included in this project).

---

### `POST /auth/reset-password`

```json
{
  "token": "0f1e2d...",
  "newPassword": "NewStrong@123",
  "confirmPassword": "NewStrong@123"
}
```

Tokens expire after 30 minutes and are single use. Errors: `400` invalid/expired
token or password mismatch.

---

### `GET /auth/health`

Public liveness probe:

```json
{ "status": "UP", "bank": "HFDC Bank (educational simulation)" }
```

---

## 4. Account endpoints — all protected

### `GET /accounts/me`

```json
{
  "accountId": 1,
  "customerId": "CID1000000001",
  "accountNumber": "HFDC1000000001",
  "accountType": "SAVINGS",
  "balance": 115000.00,
  "accountStatus": "ACTIVE",
  "createdAt": "2026-06-01T10:00:00"
}
```

### `GET /accounts/balance`

```json
{
  "accountNumber": "HFDC1000000001",
  "accountType": "SAVINGS",
  "balance": 115000.00,
  "lastUpdated": "2026-11-08T23:15:00"
}
```

`lastUpdated` is the newest ledger entry time.

### `GET /accounts/dashboard`

All figures are computed from MySQL. Deposits include `DEPOSIT` and
`TRANSFER_IN`; withdrawals include `WITHDRAWAL` and `TRANSFER_OUT`. The chart
covers up to the last 12 months.

```json
{
  "fullName": "Aarav Sharma",
  "accountNumber": "HFDC1000000001",
  "accountType": "SAVINGS",
  "balance": 115000.00,
  "totalDeposits": 150000.00,
  "totalWithdrawals": 35000.00,
  "transactionCount": 6,
  "recentTransactions": [
    {
      "transactionId": 6,
      "referenceNumber": "TXN20261108DEMO06",
      "transactionType": "WITHDRAWAL",
      "amount": 10000.00,
      "description": "Cash withdrawal",
      "transactionStatus": "SUCCESS",
      "createdAt": "2026-11-08T23:15:00",
      "direction": "DEBIT",
      "relatedParty": null
    }
  ],
  "monthlySummary": [
    { "month": "2026-06", "deposits": 50000.00, "withdrawals": 0.00 }
  ]
}
```

---

## 5. Transaction endpoints — all protected

### `POST /transactions/deposit`

Simulated deposit. Maximum **1,000,000.00** per transaction.

```json
{ "amount": 2500.00, "description": "Freelance payment (simulated)" }
```

Response **201**:

```json
{
  "referenceNumber": "TXN20261117AB12CD34",
  "transactionId": 12,
  "balance": 117500.00,
  "message": "Deposit successful. This is a simulated educational transaction."
}
```

Errors: `400` amount ≤ 0, above limit, inactive account.

---

### `POST /transactions/withdraw`

Requires the BCrypt-verified transaction PIN. Maximum **100,000.00** per
transaction. The SQL update is guarded by `balance >= amount`.

```json
{
  "amount": 1500.00,
  "transactionPin": "1234",
  "description": "ATM withdrawal (simulated)"
}
```

Response **201**:

```json
{
  "referenceNumber": "TXN20261117EF56GH78",
  "transactionId": 13,
  "balance": 116000.00,
  "message": "Withdrawal successful"
}
```

Errors: `400` invalid PIN, insufficient balance, amount outside limits.

---

### `GET /transactions`

Search, filter, sort and paginate **the caller's own** ledger.

| Query | Default | Notes |
| --- | --- | --- |
| `type` | all | `DEPOSIT`, `WITHDRAWAL`, `TRANSFER_IN`, `TRANSFER_OUT` (case-insensitive; unknown values are currently ignored) |
| `from` | — | `yyyy-MM-dd` (midnight) or ISO date-time |
| `to` | — | `yyyy-MM-dd` (end of day) or ISO date-time |
| `search` | — | Case-insensitive `LIKE` on reference number, description and type |
| `sort` | `desc` | `asc` or `desc` by `created_at` |
| `page` | `0` | Zero-based; negative values become 0 |
| `size` | `10` | 1–100; values outside are clamped |

Example: `/api/transactions?type=WITHDRAWAL&from=2026-08-01&to=2026-11-30&search=cash&sort=desc&page=0&size=5`

Response:

```json
{
  "content": [
    {
      "transactionId": 6,
      "referenceNumber": "TXN20261108DEMO06",
      "transactionType": "WITHDRAWAL",
      "amount": 10000.00,
      "description": "Cash withdrawal",
      "transactionStatus": "SUCCESS",
      "createdAt": "2026-11-08T23:15:00",
      "direction": "DEBIT",
      "relatedParty": null
    }
  ],
  "page": 0,
  "size": 5,
  "totalElements": 2,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

### `GET /transactions/{referenceNumber}`

Returns one ledger row **only if it belongs to the caller**. A row owned by
another customer returns `404 Transaction not found`.

```json
{
  "transactionId": 2,
  "referenceNumber": "TXN20260705DEMO02",
  "transactionType": "DEPOSIT",
  "amount": 90000.00,
  "description": "Salary credit (simulated)",
  "transactionStatus": "SUCCESS",
  "createdAt": "2026-07-06T00:10:00",
  "direction": "CREDIT",
  "relatedParty": null
}
```

---

## 6. Transfer endpoints — all protected

### `POST /transfers`

Atomically moves money to another **existing, active** HFDC account. Maximum
**100,000.00** per transfer. The beneficiary account must already exist in the
bank (the seed includes Priya's account for demos).

```json
{
  "beneficiaryAccountNumber": "HFDC1000000002",
  "beneficiaryName": "Priya Nair",
  "amount": 1000.00,
  "remarks": "Dinner split",
  "transactionPin": "1234"
}
```

Response **201**:

```json
{
  "referenceNumber": "TRF20261117JK90LM12",
  "amount": 1000.00,
  "beneficiaryAccountNumber": "HFDC1000000002",
  "beneficiaryName": "Priya Nair",
  "balance": 114000.00,
  "message": "Transfer completed successfully"
}
```

Behaviour and errors:

| Condition | Status | Message |
| --- | --- | --- |
| Own account number | 400 | `You cannot transfer money to your own account` |
| Wrong PIN | 400 | `Invalid transaction PIN` |
| Unknown beneficiary account | 404 | `Beneficiary account not found` |
| Inactive account | 400 | `The beneficiary account is not active` |
| Insufficient balance | 400 | `Insufficient balance for this transfer` |
| Amount ≤ 0 or > 100,000 | 400 | Validation message |

The whole operation runs in one database transaction: lock both account rows in
ascending `account_id` order, debit, credit, insert two ledger rows and one
`transfers` audit row. Any failure rolls everything back.

### `GET /transfers/history`

Returns up to 100 transfer audit rows involving the caller, newest first:

```json
[
  {
    "transactionId": 0,
    "referenceNumber": "TRF20261117JK90LM12",
    "transactionType": "TRANSFER_DEBIT",
    "amount": 1000.00,
    "description": "Dinner split",
    "transactionStatus": "SUCCESS",
    "createdAt": "2026-11-17T14:30:00",
    "direction": "DEBIT",
    "relatedParty": "HFDC1000000002"
  }
]
```

---

## 7. Beneficiary endpoints — all protected

### `POST /beneficiaries`

The target account must exist and be active. You cannot add your own account.

```json
{ "name": "Priya Nair", "accountNumber": "HFDC1000000002" }
```

Response **201**:

```json
{
  "beneficiaryId": 3,
  "beneficiaryName": "Priya Nair",
  "beneficiaryAccountNumber": "HFDC1000000002",
  "createdAt": "2026-11-17T14:35:00"
}
```

Errors: `400` own account/inactive, `404` account not found, `409` already saved.

### `GET /beneficiaries?search=priya`

Returns the caller's saved beneficiaries, optionally filtered by name or account
number:

```json
[
  {
    "beneficiaryId": 3,
    "beneficiaryName": "Priya Nair",
    "beneficiaryAccountNumber": "HFDC1000000002",
    "createdAt": "2026-11-17T14:35:00"
  }
]
```

### `DELETE /beneficiaries/{id}`

Deletes only if the beneficiary belongs to the caller.

```json
{ "message": "Beneficiary removed" }
```

Errors: `404 Beneficiary not found`.

---

## 8. Profile endpoints — all protected

### `GET /profile`

```json
{
  "customerId": "CID1000000001",
  "fullName": "Aarav Sharma",
  "email": "aarav@hfdcbank.test",
  "mobileNumber": "9876543210",
  "dateOfBirth": "1992-04-18",
  "address": "12 Marine Drive, Mumbai",
  "createdAt": "2026-06-01T10:00:00",
  "accountNumber": "HFDC1000000001"
}
```

### `PUT /profile`

Only name, mobile and address are editable; email and customer id are immutable.

```json
{
  "fullName": "Aarav S Sharma",
  "mobileNumber": "9876543210",
  "address": "14 Marine Drive, Mumbai"
}
```

Returns the updated profile. Errors: `409` mobile already used by another
customer.

### `PUT /profile/change-password`

```json
{
  "currentPassword": "Demo@1234",
  "newPassword": "NewStrong@123",
  "confirmPassword": "NewStrong@123"
}
```

```json
{ "message": "Password changed successfully" }
```

Errors: `400` wrong current password, mismatch, or new password equal to the old
one.

---

## 9. Validation rules

| Field | Rule |
| --- | --- |
| Email | Valid email, stored lower-case, unique |
| Mobile | Exactly 10 digits, unique |
| Password | 8+ chars with upper, lower, digit and one of `@#$%^&+=!` |
| Transaction PIN | 4–6 digits |
| Deposit amount | 0.01 – 1,000,000.00 |
| Withdrawal amount | 0.01 – 100,000.00 |
| Transfer amount | 0.01 – 100,000.00 |
| Beneficiary name/account | Non-blank; account must exist and be active |
| Reset token | 64 hex characters, 30-minute expiry, single use |

Bean Validation failures return `400` with all field messages joined by `; `.

---

## 10. Status codes

| Code | Meaning |
| --- | --- |
| 200 | Success |
| 201 | Created (register, deposit, withdrawal, transfer, beneficiary) |
| 400 | Validation or business rule failure |
| 401 | Missing/expired session |
| 403 | CSRF token missing/invalid, or access denied |
| 404 | Resource not found (or not owned by the caller) |
| 405 | HTTP method not supported by the endpoint |
| 409 | Duplicate email, mobile, beneficiary |
| 500 | Unexpected server error (details logged, never returned) |

---

## 11. Quick reference

```
GET    /api/auth/health
GET    /api/auth/csrf
POST   /api/auth/register
POST   /api/auth/login
POST   /api/auth/logout
GET    /api/auth/me
POST   /api/auth/forgot-password
POST   /api/auth/reset-password

GET    /api/accounts/me
GET    /api/accounts/balance
GET    /api/accounts/dashboard

POST   /api/transactions/deposit
POST   /api/transactions/withdraw
GET    /api/transactions
GET    /api/transactions/{referenceNumber}

POST   /api/transfers
GET    /api/transfers/history

POST   /api/beneficiaries
GET    /api/beneficiaries
DELETE /api/beneficiaries/{id}

GET    /api/profile
PUT    /api/profile
PUT    /api/profile/change-password
```
