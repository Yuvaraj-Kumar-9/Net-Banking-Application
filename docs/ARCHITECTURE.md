# HFDC Bank — Architecture

> HFDC Bank is a fictional educational banking simulation, not a real financial
> institution.

---

## 1. System overview

```
┌────────────────────────────── Client ───────────────────────────────┐
│  React 18 single-page application (Vite build)                      │
│  React Router · Axios · Bootstrap 5 · Recharts · Lucide             │
│                                                                     │
│  Reads XSRF-TOKEN cookie → sends X-XSRF-TOKEN header                │
│  Sends hfdc_token HttpOnly cookie automatically                     │
└──────────────────────────────────┬──────────────────────────────────┘
                                   │ HTTPS, JSON, same origin
┌──────────────────────────────────▼──────────────────────────────────┐
│  Spring Boot 3.3.5 on embedded Tomcat                               │
│                                                                     │
│  JwtAuthenticationFilter  →  JWT identity + CSRF enforcement        │
│  Controller               →  HTTP mapping + Bean Validation         │
│  Service                  →  business rules + transaction boundary  │
│  Repository               →  explicit SQL via JdbcTemplate          │
│  GlobalExceptionHandler   →  uniform ApiError JSON                  │
└──────────────────────────────────┬──────────────────────────────────┘
                                   │ JDBC (HikariCP pool)
┌──────────────────────────────────▼──────────────────────────────────┐
│  MySQL 8 / InnoDB                                                   │
│  customers · accounts · transactions · transfers                    │
│  beneficiaries · password_resets                                    │
└─────────────────────────────────────────────────────────────────────┘
```

In production the React bundle is copied into the Spring Boot jar and served from
`classpath:/static`, so the UI and API share one origin. In development Vite runs
on port 5173 and proxies `/api` to port 8080.

---

## 2. Backend layers

All code lives under `com.hfdcbank`.

| Package | Responsibility | Examples |
| --- | --- | --- |
| `config` | Security filter chain, CORS, OpenAPI, SPA static serving | `SecurityConfig`, `CorsConfig`, `OpenApiConfig`, `SpaWebConfig` |
| `controller` | HTTP endpoints only; no business logic or SQL | `AuthController`, `TransferController` |
| `service` | Business rules, ownership checks, transaction boundaries | `TransferService`, `TransactionService` |
| `repository` | Parameterised SQL and row mapping | `AccountRepository`, `TransactionRepository` |
| `entity` | Immutable Java records representing database rows | `Customer`, `Account`, `Transaction` |
| `dto` | Request/response records with validation annotations | `TransferRequest`, `DashboardResponse` |
| `security` | JWT creation/validation, filter, user details, current user | `JwtService`, `JwtAuthenticationFilter` |
| `exception` | Typed business exceptions and global JSON handler | `InvalidPinException`, `GlobalExceptionHandler` |
| `util` | Money rounding, cookies, ID/reference generators, generated keys | `MoneyUtil`, `CookieUtil` |

### Layer rules

1. Controllers never call repositories directly.
2. Repositories never contain business decisions.
3. Services never trust a customer id, account id or balance from the client.
4. DTOs never expose password hashes, PIN hashes or internal ownership ids.
5. Every mutating service method runs in a transaction (declarative
   `@Transactional` or a `TransactionTemplate` when the boundary must be explicit).

### Request lifecycle

```
HTTP request
  → CORS / security filter chain
  → JwtAuthenticationFilter
       · CSRF check for POST/PUT/PATCH/DELETE under /api
       · read hfdc_token cookie or Bearer header
       · load user by email, populate SecurityContext
  → DispatcherServlet → Controller
       · Bean Validation on @Valid request body
  → Service (business rules, @Transactional)
  → Repository (JdbcTemplate)
  → MySQL
  → DTO response
  → JSON (or ApiError on exception)
```

---

## 3. Data model

```
customers
  customer_id (PK, VARCHAR)
  full_name, email (UQ), mobile_number (UQ), date_of_birth, address
  password_hash, created_at, updated_at
        │ 1
        │
        │ 0..1
accounts
  account_id (PK, BIGINT AI)
  customer_id (FK → customers)
  account_number (UQ), account_type, balance DECIMAL(19,2)
  transaction_pin_hash, account_status, created_at
        │ 1
        ├──────────────────────────────┐
        │ 0..*                         │ 0..*
transactions                     transfers
  transaction_id (PK)              transfer_id (PK)
  account_id (FK)                  sender_account_id (FK)
  reference_number (UQ)            receiver_account_id (FK)
  transaction_type                 amount DECIMAL(19,2)
  amount DECIMAL(19,2)             remarks, transfer_status, created_at
  description, status, created_at

customers 1 ──── 0..* beneficiaries
  beneficiary_id (PK), customer_id (FK), beneficiary_name,
  beneficiary_account_number, created_at
  UNIQUE (customer_id, beneficiary_account_number)

customers 1 ──── 0..* password_resets
  reset_id (PK), customer_id (FK), token_hash, expires_at, used, created_at
```

Schema source: [`sql/01_schema.sql`](../sql/01_schema.sql).

### Money handling

- MySQL column type: `DECIMAL(19,2)`.
- Java type: `BigDecimal` end to end.
- `MoneyUtil.money(...)` normalises every incoming amount to scale 2 with
  `RoundingMode.HALF_UP`; `MoneyUtil.isPositive(...)` rejects zero/negative.
- Database check constraints (`balance >= 0`, `amount > 0`) are the last line of
  defence.

### Referential integrity

- `accounts.customer_id → customers.customer_id` (`ON DELETE RESTRICT`).
- `transactions.account_id → accounts.account_id` (`ON DELETE RESTRICT`).
- `transfers` references both sender and receiver accounts (`ON DELETE RESTRICT`).
- `beneficiaries` and `password_resets` cascade when a customer is deleted.
- Indexes: account/date composites for history, unique reference numbers,
  unique email/mobile, unique beneficiary pair.

---

## 4. Atomic fund transfer

`TransferService.transfer(...)` is the most safety-critical path.

### Pre-flight checks (outside the transaction)

1. Amount is positive after `MoneyUtil.money(...)`.
2. Resolve the sender from the JWT subject (never from the request).
3. Reject a transfer to the sender's own account number.
4. Reject an inactive sender account.
5. Verify the transaction PIN with BCrypt.
6. Look up the receiver by account number; reject unknown/inactive receivers.

### Transaction body (`TransactionTemplate`)

```
BEGIN
  firstId  = min(senderId, receiverId)
  secondId = max(senderId, receiverId)

  SELECT ... FROM accounts WHERE account_id = firstId  FOR UPDATE
  SELECT ... FROM accounts WHERE account_id = secondId FOR UPDATE

  if locked sender balance < amount → throw InsufficientBalanceException
  UPDATE accounts SET balance = balance - ? WHERE account_id = senderId
                                              AND balance >= ?
  UPDATE accounts SET balance = balance + ? WHERE account_id = receiverId

  INSERT INTO transactions (... TRANSFER_OUT ...)
  INSERT INTO transactions (... TRANSFER_IN  ...)
  INSERT INTO transfers (... audit row ...)
COMMIT
```

Any exception rolls back **all** statements, so the debit cannot survive without
the credit and the ledger rows.

### Why the locks are ordered

If Aarav (account 1) transfers to Priya (account 2) while Priya transfers to
Aarav at the same instant, opposite lock orders would deadlock. Locking the lower
`account_id` first makes every transaction acquire locks in the same global order,
so one waits instead of forming a cycle.

### Why the debit has a balance predicate

`UPDATE ... WHERE balance >= ?` makes the overdraft check atomic at the database,
even if the balance changed after it was read. The service checks the locked
balance first for a friendly error; the SQL predicate is the hard guarantee.

### Concurrency proof

`TransferConcurrencyTest` runs 20 simultaneous transfers of ₹100.00 from a
₹1,000.00 account against real repositories:

- exactly 10 succeed,
- exactly 10 fail with `InsufficientBalanceException`,
- sender ends at ₹0.00, receiver at ₹1,000.00,
- 10 `TRANSFER_OUT` + 10 `TRANSFER_IN` ledger rows and 10 `transfers` rows.

---

## 5. Authentication and CSRF design

### Why an HttpOnly cookie instead of `localStorage`

A JWT in `localStorage` is readable by any injected script. An `HttpOnly` cookie
is not. The SPA still gets CSRF protection from a separate readable cookie.

### Token

- HMAC-SHA256 via JJWT 0.12.6.
- Subject: the customer's email.
- 1-day expiry (`jwt.expiration-ms`, default 86,400,000 ms).
- Secret from `JWT_SECRET`; there is **no working default**, and the application
  fails fast at startup if the secret is missing or shorter than 32 bytes.
- No roles are stored in the token: this application has one customer role, and
  the account is re-read from MySQL on every request.

### Cookie attributes

| Attribute | `hfdc_token` | `XSRF-TOKEN` |
| --- | --- | --- |
| `HttpOnly` | Yes | No |
| `Path` | `/` | `/` |
| `Max-Age` | 1 day | 1 day |
| `SameSite` | `Lax` (configurable) | `Lax` |
| `Secure` | when `COOKIE_SECURE=true` | when `COOKIE_SECURE=true` |

For a split frontend/backend deployment on different sites, set
`COOKIE_SECURE=true` and `COOKIE_SAME_SITE=none`.

### Double-submit CSRF

- Server sets a random 32-byte URL-safe token in `XSRF-TOKEN`.
- Browser JavaScript copies it to `X-XSRF-TOKEN`.
- `JwtAuthenticationFilter` compares cookie and header in constant time for every
  mutating `/api` request.
- A Bearer header bypasses CSRF because browsers never add it automatically.
- Public auth endpoints still require CSRF for mutations (login, register,
  logout, reset), which prevents login-CSRF.

### Password and PIN storage

- BCrypt strength 12.
- Password hash: `customers.password_hash`.
- Transaction PIN hash: `accounts.transaction_pin_hash`.
- Reset tokens: 32 random bytes, stored only as SHA-256, 30-minute expiry,
  single use.

### Authorisation

- `SecurityConfig` protects `/api/**`; static assets and SPA routes are public.
- Ownership is derived from the JWT subject:
  - `AccountService.currentCustomer()` / `currentAccount()`.
  - Beneficiary delete uses `DELETE ... WHERE beneficiary_id = ? AND customer_id = ?`.
  - Transaction detail checks `tx.accountId() == currentAccount.accountId()` and
    returns `404` for other customers' rows.

---

## 6. Error handling

`GlobalExceptionHandler` maps:

| Exception | Status |
| --- | --- |
| `MethodArgumentNotValidException` | 400 with joined field messages |
| `HttpMessageNotReadableException` | 400 |
| `NoResourceFoundException` | 404 |
| `HttpRequestMethodNotSupportedException` | 405 |
| `DuplicateResourceException` | 409 |
| `ResourceNotFoundException` | 404 |
| `InsufficientBalanceException`, `InvalidPinException`, `BusinessException` | 400 |
| `AuthenticationException` | 401 |
| `AccessDeniedException` | 403 |
| anything else | 500, generic message, full detail logged |

The response shape is always `ApiError`. Stack traces, SQL text and hashes are
never sent to the client. `server.error.include-message` and
`include-stacktrace` are set to `never`.

---

## 7. Frontend architecture

```
frontend/src
├── main.jsx                 React root + BrowserRouter
├── App.jsx                  Public/protected route map
├── components/
│   ├── AppLayout.jsx        Sidebar + top bar for signed-in pages
│   ├── PublicLayout.jsx     Navbar + footer for public pages
│   ├── ProtectedRoute.jsx   Session check via /auth/me
│   ├── Logo.jsx             Original SVG bank mark
│   ├── Navbar / Sidebar     Responsive navigation
│   ├── ConfirmDialog.jsx    Destructive-action confirmation
│   ├── Notification.jsx     Toast-style feedback
│   ├── StatCard.jsx         Dashboard metric cards
│   └── HeroIllustration.jsx Original landing-page artwork
├── context/
│   ├── AuthContext.jsx      Login/logout/session state
│   └── NotificationContext.jsx
├── pages/                   18 pages (public + protected)
├── services/
│   ├── api.js               Axios instance, CSRF interceptor, 401 handling
│   ├── authService.js       ...
│   └── ...                  One module per API area
├── styles/
│   ├── global.css           Design tokens, layout, forms, landing page
│   └── dashboard.css        Signed-in application styles
└── utils/format.js          Currency and date formatting
```

### State management

- No Redux. Two small React contexts cover the only global state: the current
  session and transient notifications.
- Server data is fetched on page mount; the backend is the single source of truth.
- No hardcoded balances, transactions or beneficiaries exist in the frontend.

### API client

- `withCredentials: true` sends the HttpOnly cookie.
- A request interceptor ensures `XSRF-TOKEN` exists and copies it into
  `X-XSRF-TOKEN` for mutations.
- A response interceptor broadcasts `hfdc:unauthorized` on 401 so the auth context
  can clear the session and redirect to login.
- `apiErrorMessage(...)` turns Axios/network failures into human-readable text.

### Routing

- Public: `/`, `/about`, `/services`, `/contact`, `/login`, `/register`,
  `/forgot-password`.
- Protected: `/dashboard`, `/account`, `/balance`, `/deposit`, `/withdraw`,
  `/transfer`, `/transactions`, `/beneficiaries`, `/profile`, `/change-password`.
- `*` renders a branded 404 page.
- `ProtectedRoute` verifies the session with the backend before rendering.

### Design system

- Navy (`#0b1f3a`), white, and light-blue accents; CSS custom properties.
- Responsive breakpoints for sidebar collapse, stacked cards and mobile menus.
- Visible simulation disclaimer on the landing page and footer.
- Testimonials are explicitly labelled as illustrative.

---

## 8. Key design decisions

| Decision | Rationale | Trade-off |
| --- | --- | --- |
| Spring JDBC, no JPA | Every SQL statement is explicit; easy to explain locks and constraints | More manual row mapping |
| HttpOnly JWT cookie | XSS cannot read the session token | Needs CSRF handling |
| Double-submit CSRF | Stateless, no server session store | Requires JavaScript to copy the header |
| BCrypt 12 | Slow hashing resists brute force | ~100 ms per login (acceptable) |
| `TransactionTemplate` for transfers | Makes the transaction boundary obvious and testable | Slightly more verbose than `@Transactional` |
| Ascending row locks | Prevents deadlocks between opposite transfers | Serialises conflicting transfers |
| SQL balance predicate | Atomic overdraft guard | Must inspect update count for failure |
| Same-origin production | No CORS, first-party cookies, simpler deployment | One container serves both |
| H2 only in tests | Real MySQL in production, fast CI concurrency test | SQL must stay portable in tests |
| Simulated deposits | No payment provider or real money | Clearly labelled in UI and API |

---

## 9. Deployment topology

```
Internet
   │ HTTPS
   ▼
Render / Railway web service (Docker)
   ├── Spring Boot jar
   │     ├── /api/**            → controllers
   │     └── /**                → React build (SPA fallback)
   └── outbound TLS
         ▼
   Managed MySQL 8 (Aiven / TiDB Cloud / Railway)
```

Environment variables are supplied by the platform, never committed. See
[`DEPLOYMENT.md`](DEPLOYMENT.md) for the full list and steps.

---

## 10. Observability and operations

- Spring Boot logs at `INFO` for `com.hfdcbank`; security logs at `WARN`.
- Request bodies are never logged (they contain passwords and PINs).
- `/api/auth/health` is the liveness/health-check endpoint.
- HikariCP: maximum 12 connections, minimum idle 2, 20-second timeout.
- The database schema is created by `sql/01_schema.sql`; there is no automatic
  migration tool. Add Flyway/Liquibase before evolving a production schema.
- No distributed tracing, metrics endpoint or centralised logging is included;
  add Actuator + a log drain for a real deployment.

---

## 11. Scalability and future work

The service is stateless (JWT cookie, no server session), so multiple instances
can run behind a load balancer. Areas to strengthen for real production use:

- Rate limiting on login, PIN attempts and password reset.
- Account lockout / exponential backoff after repeated failures.
- Email or SMS delivery for reset tokens, plus verification links.
- Idempotency keys for transfer retries.
- Flyway/Liquibase migrations and automated backups.
- Audit log for every security-sensitive action.
- 2FA for high-value transfers.
- Frontend code-splitting to reduce the initial bundle.
