# HFDC Bank — Testing Guide

> HFDC Bank is a fictional educational banking simulation. Test accounts and
> amounts are synthetic.

---

## 1. Running the suite

```bash
# from the repository root
mvn test
```

Requirements: JDK 21 and Maven 3.9+. MySQL is **not** required: the unit tests use
Mockito, and the concurrency test uses H2 in MySQL compatibility mode (test scope
only).

Latest result:

```
Tests run: 67, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Per class:

| Test class | Tests | Focus |
| --- | ---: | --- |
| `util/MoneyUtilTest` | 4 | Scale, HALF_UP rounding, positive/zero/negative/null |
| `security/JwtServiceTest` | 5 | Subject, tampering, wrong secret, expiry, weak-secret rejection |
| `service/AuthServiceTest` | 14 | Register, duplicates, login, cookies, reset-token privacy |
| `service/BeneficiaryServiceTest` | 8 | Add/list/delete, ownership, inactive/unknown/duplicate |
| `service/ProfileServiceTest` | 8 | Profile mapping, mobile uniqueness, password change rules |
| `service/TransactionServiceTest` | 17 | Deposit, withdrawal, PIN, limits, detail IDOR, pagination |
| `service/TransferServiceTest` | 10 | Atomic transfer, lock order, PIN, self/unknown/inactive/insufficient |
| `service/TransferConcurrencyTest` | 1 | 20 real concurrent transfers against H2 |
| **Total** | **67** | |

Run one class or method:

```bash
mvn -Dtest=TransferServiceTest test
mvn -Dtest=TransferServiceTest#locksAccountsInAscendingIdOrderToAvoidDeadlocks test
```

---

## 2. What each area proves

### Money (`MoneyUtilTest`)

- Every amount is normalised to two decimal places.
- Rounding is `HALF_UP`, matching banking expectations.
- Zero, negative and `null` are rejected as non-positive.

### JWT (`JwtServiceTest`)

- The email is the subject.
- Malformed, tampered and wrong-secret tokens are rejected.
- Expired tokens are rejected.
- Secrets shorter than 256 bits are refused at startup.

### Authentication (`AuthServiceTest`)

- Registration BCrypt-hashes both password and PIN and creates a zero-balance
  `SAVINGS` account.
- Password confirmation mismatch is rejected.
- Duplicate email and mobile are rejected.
- Login sets the HttpOnly JWT cookie and the CSRF cookie.
- Unknown email and wrong password return the **same** error and no cookies.
- Inactive accounts cannot log in.
- Forgot-password stores only a SHA-256 hash of the token and returns the raw
  token only in dev mode.
- An unknown email never reveals whether an account exists.
- Reset rejects mismatched confirmation and invalid/expired tokens, then updates
  the hash and marks the token used.

### Beneficiaries (`BeneficiaryServiceTest`)

- The target account must exist and be active.
- You cannot add your own account.
- Duplicate beneficiary pairs are rejected with `409`.
- Listing passes the search term through.
- Delete is scoped to the caller: another customer's row returns `404`.

### Profile (`ProfileServiceTest`)

- Profile responses contain no hashes.
- Another customer's mobile number is rejected with `409`.
- Keeping your own mobile number is allowed.
- A new mobile number is persisted.
- Password change rejects mismatch, wrong current password, and reuse of the
  current password, then stores a new BCrypt hash.

### Deposits and withdrawals (`TransactionServiceTest`)

- Deposit credits the balance and writes exactly one ledger row.
- A custom description is used when supplied.
- Inactive accounts are rejected.
- Zero/negative amounts fail before the account is loaded.
- A failed credit update fails cleanly.
- Withdrawal verifies the PIN before any debit.
- Over-balance, inactive-account and non-positive withdrawals are rejected.
- Transaction detail is scoped to the owner; another customer's row returns
  `404`, as does an unknown reference.
- Pagination clamps negative pages, caps size at 100 and parses inclusive date
  ranges.

### Transfers (`TransferServiceTest`)

- A successful transfer debits the sender, credits the receiver and writes both
  ledger rows plus the audit row.
- Account rows are locked in ascending `account_id` order.
- Wrong PIN is rejected before any money moves.
- Self-transfer, zero/negative amounts, inactive sender/receiver and unknown
  beneficiary are rejected.
- A transfer above the locked balance is refused.
- If the atomic debit loses a race (0 rows updated), the transaction rolls back.

### Concurrency (`TransferConcurrencyTest`)

The test uses the real `AccountRepository`, `TransactionRepository`,
`TransferService` and `TransactionTemplate` against H2 in MySQL mode
(`schema-h2.sql`). Twenty threads start from the same gate and transfer ₹100.00
from an account holding ₹1,000.00.

Assertions:

- exactly 10 transfers succeed;
- exactly 10 fail with `InsufficientBalanceException`;
- sender final balance is `0.00`;
- receiver final balance is `1000.00`;
- 10 `TRANSFER_OUT` and 10 `TRANSFER_IN` ledger rows exist;
- 10 `transfers` audit rows exist.

This proves the row locks, the `balance >= amount` predicate and the single
transaction boundary prevent both overspending and lost money.

### Cross-driver generated keys

MySQL returns one `GENERATED_KEY`; H2 returns every generated/default column.
`util/GeneratedKeys.require(...)` handles both so the same repositories work in
production and in the concurrency test. This is covered indirectly by the
concurrency test.

---

## 3. Test configuration

| File | Purpose |
| --- | --- |
| `src/test/resources/application-test.properties` | H2 in-memory URL in MySQL mode, schema init, test-only JWT secret |
| `src/test/resources/schema-h2.sql` | Portable schema for the concurrency test |
| `pom.xml` | H2 and `spring-boot-starter-test` are `test` scope only |

H2 is never on the runtime classpath; the packaged application always uses MySQL.

---

## 4. Manual end-to-end verification

The following was executed against the real backend and MySQL dev database with
cookie + CSRF handling. All results matched expectations.

### Authentication and session

| Check | Expected | Verified |
| --- | --- | --- |
| Demo login | `200`, `hfdc_token` + `XSRF-TOKEN` cookies | ✅ |
| `GET /api/auth/me` with cookie | Current customer | ✅ |
| Protected endpoint without cookie | `401` | ✅ |
| Register new customer | `201`, zero balance | ✅ |
| Duplicate email registration | `409` | ✅ |
| Missing CSRF header on mutation | `403 CSRF token missing or invalid` | ✅ |
| Bogus CSRF header | `403` | ✅ |
| Forgot password in dev mode | `200` with reset token | ✅ |
| Reset with invalid token | `400` | ✅ |
| Change password with wrong current | `400 Current password is incorrect` | ✅ |

### Banking operations

| Check | Expected | Verified |
| --- | --- | --- |
| Dashboard figures | Live MySQL values + chart data | ✅ |
| Account and balance | Current account details | ✅ |
| Deposit | `201`, balance and ledger updated | ✅ |
| Withdrawal with correct PIN | `201`, balance reduced | ✅ |
| Withdrawal with wrong PIN | `400 Invalid transaction PIN` | ✅ |
| Withdrawal above limit | `400` validation message | ✅ |
| Withdrawal above balance | `400 Insufficient balance for this withdrawal` | ✅ |
| Transfer with correct PIN | `201`, both ledger rows + audit row | ✅ |
| Transfer with wrong PIN | `400 Invalid transaction PIN` | ✅ |
| Transfer to self | `400 You cannot transfer money to your own account` | ✅ |
| Transfer above balance | `400 Insufficient balance for this transfer` | ✅ |
| Transfer to unknown account | `404 Beneficiary account not found` | ✅ |

### History, beneficiaries and profile

| Check | Expected | Verified |
| --- | --- | --- |
| Transaction list | Paginated envelope | ✅ |
| Type/date/search filters | Filtered rows | ✅ |
| Transaction detail (own row) | `200` | ✅ |
| Missing reference | `404` | ✅ |
| Another customer's reference | `404` (no IDOR) | ✅ |
| Add beneficiary | `201` | ✅ |
| Duplicate beneficiary | `409` | ✅ |
| Own account as beneficiary | `400` | ✅ |
| Unknown beneficiary account | `404` | ✅ |
| Delete beneficiary | `200` | ✅ |
| Delete another customer's beneficiary | `404` | ✅ |
| Profile read/update/revert | `200`, persisted | ✅ |
| Unknown API path (authenticated) | `404` JSON | ✅ |
| Wrong HTTP method | `405` JSON | ✅ |
| React `/` and `/dashboard` | Served from the jar | ✅ |
| Missing static asset | `404` (not SPA fallback) | ✅ |
| Swagger UI | `200` | ✅ |

### Suggested manual checklist for a reviewer

1. Start MySQL, backend and frontend (see README).
2. Log in as Aarav; confirm dashboard balance is ₹1,15,000.00.
3. Deposit ₹500; confirm the balance and a new ledger row.
4. Withdraw ₹200 with PIN `1234`; then retry with PIN `0000` and confirm the
   error.
5. Transfer ₹1,000 to `HFDC1000000002`; log in as Priya and confirm the credit.
6. Filter history by `TRANSFER_OUT` and by date; open a detail view.
7. Add Priya as a beneficiary, search for her, delete the row.
8. Update the address, then change it back.
9. Change the password, log out, and log in with the new password.
10. Resize the browser to a phone width and confirm navigation and forms remain
    usable.
11. Clear cookies and confirm protected pages redirect to login.

> Manual testing mutates the demo database. Run
> `scripts\dev-db-reset.cmd` to restore the seed state.

---

## 5. Known test gaps

- No browser automation (Playwright/Cypress); UI checks are manual.
- No MySQL-specific integration test in CI; the concurrency test uses H2 in MySQL
  mode. A Testcontainers MySQL profile would close this gap.
- No load test for the login/PIN endpoints; add rate limiting before public use.
- No contract tests between the SPA and the API.
- Controller/security filter integration tests are covered indirectly by the
  manual checklist, not by MockMvc tests.

---

## 6. Continuous integration

A GitHub Actions workflow is included at `.github/workflows/ci.yml`. It runs on
every push and pull request:

1. `mvn -B test` on JDK 21 (backend, 67 tests).
2. `npm ci` and `npm run build` on Node 20 (frontend production build).

No database or secrets are required for CI.
