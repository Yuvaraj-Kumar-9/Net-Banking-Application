# HFDC Bank — Full-Stack Net Banking Simulation

> **Banking Beyond Boundaries**

> ⚠️ **Educational disclaimer**
> HFDC Bank is a **fictional educational banking simulation**. It is **not** a
> real financial institution. No real money is held, moved, or insured. All
> accounts, customers, transactions and testimonials are synthetic. Do not enter
> real personal or financial information.

HFDC Bank is a complete, production-shaped net banking application built to
demonstrate secure authentication, transactional money movement, layered Spring
Boot architecture, and a polished React interface. It is deliberately built with
**Spring JDBC (JdbcTemplate) only — no JPA/Hibernate** — so every SQL statement
is explicit and reviewable.

![Java](https://img.shields.io/badge/Java-21-0b1f3a?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-6db33f?style=flat-square)
![MySQL](https://img.shields.io/badge/MySQL-8-00758f?style=flat-square)
![React](https://img.shields.io/badge/React-18-61dafb?style=flat-square)
![Tests](https://img.shields.io/badge/tests-67%20passing-2e7d32?style=flat-square)
![License](https://img.shields.io/badge/license-educational-8e24aa?style=flat-square)

---

## Contents

- [Features](#features)
- [Tech stack](#tech-stack)
- [Architecture](#architecture)
- [Quick start (Windows)](#quick-start-windows)
- [Manual setup](#manual-setup)
- [Demo accounts](#demo-accounts)
- [Project structure](#project-structure)
- [API overview](#api-overview)
- [Security model](#security-model)
- [Testing](#testing)
- [Production build and deployment](#production-build-and-deployment)
- [Documentation](#documentation)
- [Known limitations](#known-limitations)
- [License](#license)

---

## Features

### Customer-facing

- **Landing site** — hero, services, “why choose us”, security, illustrative
  testimonials, FAQ, contact, footer and a visible simulation disclaimer.
- **Registration and login** — BCrypt password hashing, password confirmation,
  strong-password rules, duplicate email/mobile detection.
- **Dashboard** — live balance, deposits, withdrawals, transaction count, recent
  activity and a monthly deposits/withdrawals chart from real database rows.
- **Accounts** — account details and balance enquiry.
- **Deposit** — simulated deposit with validation and an immutable ledger row.
- **Withdraw** — transaction-PIN protected withdrawal with an atomic balance guard.
- **Transfer** — atomic peer-to-peer transfer with PIN validation, row locking,
  two ledger rows and an audit row in a single database transaction.
- **Transaction history** — search, type filter, date range, sort direction,
  pagination and per-transaction detail.
- **Beneficiaries** — add, search, list and delete saved recipients.
- **Profile** — view/update name, mobile and address; change password.
- **Forgot/reset password** — 30-minute single-use token (shown on screen only in
  development mode because there is no email provider).
- **Responsive UI** — navy/white/light-blue design system, mobile navigation,
  accessible forms, loading states, notifications and confirmation dialogs.

### Engineering

- Layered backend: `config`, `controller`, `service`, `repository`, `entity`,
  `dto`, `security`, `exception`, `util`.
- Spring JDBC only; `DECIMAL(19,2)` in MySQL and `BigDecimal` in Java.
- Stateless JWT authentication in an **HttpOnly cookie** plus a **double-submit
  CSRF cookie/header** — no JWT in `localStorage`.
- Method-level Bean Validation with uniform JSON errors.
- OpenAPI/Swagger documentation at `/swagger-ui.html`.
- 67 automated tests, including a real 20-thread concurrency test against the
  actual `JdbcTemplate` repositories.
- One-command local development scripts, Docker image, and Render blueprint.
- SQL schema, idempotent seed data, and deployment/security documentation.

---

## Tech stack

| Layer | Technology |
| --- | --- |
| Language | Java 21 |
| Backend | Spring Boot 3.3.5, Spring Web MVC, Spring Security, Spring JDBC (`JdbcTemplate`), Bean Validation |
| Auth | JJWT 0.12.6 (HMAC-SHA256), BCrypt (strength 12), HttpOnly cookie, double-submit CSRF |
| Database | MySQL 8, InnoDB, `DECIMAL(19,2)`, foreign keys, check constraints |
| API docs | springdoc-openapi 2.6.0 (Swagger UI) |
| Frontend | React 18, React Router 6, Axios, Bootstrap 5, Lucide icons, Recharts, Vite 5 |
| Testing | JUnit 5, Mockito, AssertJ, Spring Boot Test, H2 (test scope only) |
| Build | Maven 3.9+, npm 11 |
| Packaging | Executable Spring Boot jar, multi-stage Docker image |

---

## Architecture

```
┌──────────────────────────── Browser ────────────────────────────┐
│  React 18 SPA (Vite)                                            │
│  React Router · Axios · Bootstrap · Recharts                    │
│  HttpOnly hfdc_token cookie + readable XSRF-TOKEN cookie        │
└───────────────────────────────┬─────────────────────────────────┘
                                │ HTTPS / JSON
┌───────────────────────────────▼─────────────────────────────────┐
│  Spring Boot 3 (embedded Tomcat)                                │
│  Security filter → JWT auth + CSRF check                        │
│  Controllers → Services → Repositories (JdbcTemplate)           │
│  GlobalExceptionHandler → uniform ApiError JSON                 │
└───────────────────────────────┬─────────────────────────────────┘
                                │ JDBC (HikariCP)
┌───────────────────────────────▼─────────────────────────────────┐
│  MySQL 8 (InnoDB)                                               │
│  customers · accounts · transactions · transfers                │
│  beneficiaries · password_resets                                │
└─────────────────────────────────────────────────────────────────┘
```

The Docker image serves the React build and the API from the **same origin**,
which keeps the authentication cookie first-party and removes production CORS.
During development, Vite proxies `/api` to `http://localhost:8080`.

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the layer rules, data
model, transfer sequence and design trade-offs.

---

## Quick start (Windows)

### Prerequisites

- JDK 21 (`java -version`)
- Maven 3.9+ (`mvn -version`, or set `MAVEN_HOME`)
- Node.js 20+ and npm
- MySQL 8 — either an existing server or the bundled isolated instance

### Option A — bundled isolated MySQL (recommended)

The scripts use a self-contained MySQL data directory on port **33306** so your
normal MySQL service is untouched. Edit `MYSQL_BIN` in
`scripts/start-dev-mysql.cmd` if MySQL is installed elsewhere.

```bat
REM 1. Start MySQL on 127.0.0.1:33306 (root, no password - local only)
scripts\start-dev-mysql.cmd

REM 2. Create the schema, seed data and the hfdc_user login
scripts\dev-db-reset.cmd

REM 3. Start the API on http://localhost:8080
scripts\run-backend.cmd

REM 4. In a second terminal, start the UI on http://localhost:5173
scripts\run-frontend.cmd
```

Open **http://localhost:5173** and sign in with a demo account below.

> `run-backend.cmd` uses the documented local credentials `hfdc_user` /
> `change_me`. Override `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` and
> `CORS_ALLOWED_ORIGINS` in your shell before running it if needed.

### Option B — your existing MySQL on 3306

Create the database and app user, then start the backend with your credentials.

```bat
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS hfdc_bank CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p hfdc_bank < sql\01_schema.sql
mysql -u root -p hfdc_bank < sql\02_seed.sql
```

```bat
set DB_URL=jdbc:mysql://127.0.0.1:3306/hfdc_bank?useSSL=false^&allowPublicKeyRetrieval=true^&serverTimezone=UTC^&characterEncoding=utf8
set DB_USER=root
set DB_PASSWORD=your-password
set JWT_SECRET=replace-with-a-long-random-secret-at-least-32-characters
mvn spring-boot:run
```

---

## Manual setup

### Backend

```bash
# from the repository root
export DB_URL='jdbc:mysql://127.0.0.1:3306/hfdc_bank?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8'
export DB_USER='hfdc_user'
export DB_PASSWORD='change_me'
export JWT_SECRET='replace-with-a-long-random-secret-at-least-32-characters'
export CORS_ALLOWED_ORIGINS='http://localhost:5173'
export DEV_MODE='true'

mvn spring-boot:run
```

- API: <http://localhost:8080/api>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Health: <http://localhost:8080/api/auth/health>

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The Vite dev server runs on <http://localhost:5173> and proxies `/api` to port
8080. To point at a different backend, create `frontend/.env.local`:

```
VITE_API_URL=http://localhost:8080/api
```

### Single-jar production build

```bash
cd frontend && npm ci && npm run build && cd ..
mvn -Pfrontend -DskipTests package
java -jar target/hfdc-bank.jar
```

The `-Pfrontend` profile copies `frontend/dist` into the jar, and Spring Boot
serves the SPA (including client-side routes) alongside the API.

---

## Demo accounts

| Customer | Email | Password | Transaction PIN |
| --- | --- | --- | --- |
| Aarav Sharma | `aarav@hfdcbank.test` | `Demo@1234` | `1234` |
| Priya Nair | `priya@hfdcbank.test` | `Demo@1234` | `4321` |

Aarav starts at **₹1,15,000.00** and Priya at **₹78,000.00** after the seed
history. These are **demo credentials for a fictional simulation** and must never
be used in a real deployment.

Seed data includes 11 ledger entries, 2 transfers and 2 beneficiaries. All seed
inserts are idempotent (`INSERT IGNORE`), and `scripts/dev-db-reset.cmd` recreates
the database from scratch.

---

## Project structure

```
HFDC-Bank-FullStack/
├── src/main/java/com/hfdcbank/
│   ├── config/          Security, CORS, OpenAPI, SPA static serving
│   ├── controller/      REST controllers (no business logic)
│   ├── dto/             Request/response records + validation
│   ├── entity/          Immutable Java records mapped from SQL
│   ├── exception/       Business exceptions + global JSON handler
│   ├── repository/      JdbcTemplate SQL and row mappers
│   ├── security/        JWT filter/service, user details, security utils
│   ├── service/         Business rules and transaction boundaries
│   └── util/            Money, cookies, IDs, references, generated keys
├── src/main/resources/  application.properties
├── src/test/            Unit tests + H2 concurrency test
├── frontend/
│   ├── public/          favicon
│   └── src/
│       ├── components/  Layouts, navbar, sidebar, dialogs, logo, charts
│       ├── context/     Auth and notification providers
│       ├── pages/       18 public and protected pages
│       ├── services/    Axios instance + one module per API area
│       ├── styles/      Global design system + dashboard styles
│       └── utils/       Currency/date formatting
├── sql/                 01_schema.sql, 02_seed.sql
├── scripts/             Dev MySQL, DB reset, backend, frontend
├── docs/                API, architecture, testing, deployment
├── Dockerfile           Multi-stage production image
├── render.yaml          Render Blueprint (Docker web service)
├── .env.example         Every supported environment variable
└── PROJECT_STATUS.md    Current progress, verification and blockers
```

---

## API overview

All endpoints are under `/api`. Mutating requests need the `X-XSRF-TOKEN`
header; browser clients receive the matching `XSRF-TOKEN` cookie automatically.

| Area | Endpoints |
| --- | --- |
| Auth | `POST /auth/register`, `POST /auth/login`, `POST /auth/logout`, `GET /auth/csrf`, `GET /auth/me`, `POST /auth/forgot-password`, `POST /auth/reset-password`, `GET /auth/health` |
| Accounts | `GET /accounts/me`, `GET /accounts/balance`, `GET /accounts/dashboard` |
| Transactions | `POST /transactions/deposit`, `POST /transactions/withdraw`, `GET /transactions`, `GET /transactions/{referenceNumber}` |
| Transfers | `POST /transfers`, `GET /transfers/history` |
| Beneficiaries | `POST /beneficiaries`, `GET /beneficiaries`, `DELETE /beneficiaries/{id}` |
| Profile | `GET /profile`, `PUT /profile`, `PUT /profile/change-password` |

Full request/response examples, validation rules and error codes are in
[`docs/API.md`](docs/API.md). Interactive documentation is available at
`/swagger-ui.html` while the backend is running.

---

## Security model

- **Passwords and transaction PINs** are hashed with BCrypt strength 12; hashes
  are never returned by the API or written to logs.
- **JWT** is signed with HMAC-SHA256 and stored in an **HttpOnly** `hfdc_token`
  cookie (`Path=/`, `SameSite=Lax`, `Secure` in production). JavaScript cannot
  read it, so XSS cannot exfiltrate the session token from storage. There is no
  working default secret: startup fails unless `JWT_SECRET` is set to at least
  32 bytes.
- **CSRF** uses the double-submit-cookie pattern: the server sets a readable
  `XSRF-TOKEN` cookie and requires the identical value in `X-XSRF-TOKEN` on every
  `POST`/`PUT`/`PATCH`/`DELETE` under `/api`. Bearer-token requests from API
  tools bypass the check because browsers never attach that header automatically.
- **Authorisation** is always derived from the JWT subject on the server; the
  client never supplies a customer id or account id for ownership checks.
  Transaction detail lookups are scoped to the caller's own account (no IDOR).
- **SQL injection** is prevented by parameterised `JdbcTemplate` queries.
- **Money movement** uses a single database transaction, ascending row-lock
  order, and `UPDATE ... WHERE balance >= ?` guards to prevent overdrafts and
  deadlocks.
- **Password reset** stores only a SHA-256 hash of a 32-byte random token, with a
  30-minute expiry and single use. In development the token is returned to the
  screen; production must set `DEV_MODE=false` and send it by email.
- **Error responses** are uniform and never include stack traces or SQL details.
- **CORS** is restricted to configured origins; same-origin deployment needs none.

Production checklist: rotate demo credentials, use a strong random `JWT_SECRET`,
set `DEV_MODE=false`, `COOKIE_SECURE=true`, enable TLS on MySQL, and do not run
the seed script on a public instance. See
[`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md).

---

## Testing

```bash
mvn test
```

Result at the time of writing:

```
Tests run: 67, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The suite covers money utilities, JWT handling, registration/login/reset,
beneficiary rules, profile updates, deposit/withdrawal/history, transfer
validation, and a real concurrency test. The concurrency test starts 20 threads
that transfer ₹100.00 each from a ₹1,000.00 balance against real `JdbcTemplate`
repositories in H2 MySQL mode: exactly 10 succeed, 10 fail with
`InsufficientBalanceException`, and the ledger/balances remain consistent.

See [`docs/TESTING.md`](docs/TESTING.md) for the full inventory, manual test
checklist and H2/MySQL notes.

---

## Production build and deployment

A multi-stage [`Dockerfile`](Dockerfile) builds the React app, packages it inside
the Spring Boot jar, and runs as a non-root user. A [`render.yaml`](render.yaml)
Blueprint is included for Render with a Docker web service and generated JWT
secret.

**Deployment status:** the application has been built and verified locally, but
it is **not publicly deployed yet**. No live URL is claimed until a deployment is
completed and verified. [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) contains
step-by-step instructions for Render + a managed MySQL 8 provider, Railway, and a
split frontend/backend option, including the exact environment variables and a
post-deployment checklist.

---

## Documentation

| Document | Contents |
| --- | --- |
| [`docs/API.md`](docs/API.md) | Endpoints, payloads, validation, CSRF flow, errors |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Layers, data model, transfer transaction, security and frontend design |
| [`docs/TESTING.md`](docs/TESTING.md) | Test inventory, concurrency proof, manual checklist |
| [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) | Docker, Render, Railway, split deployment, secrets, troubleshooting |
| [`PROJECT_STATUS.md`](PROJECT_STATUS.md) | Current status, verification evidence, blockers, known limitations |

---

## Known limitations

- Deposits are **simulated**; there is no payment gateway.
- No email/SMS provider: password-reset tokens appear on screen only when
  `DEV_MODE=true`.
- One default savings account per customer; no admin console, statements export,
  scheduled payments, 2FA or account closure.
- No rate limiting or CAPTCHA yet; add these at the edge for a public deployment.
- Unknown `type` values on the transaction list are ignored rather than rejected.
- The frontend bundle is a single chunk larger than 500 kB; code-splitting is a
  future optimisation.
- Free hosting tiers may spin down and add cold-start latency.
- Seed/demo data must be removed or rotated before any public exposure.

---

## License

Released for **educational and portfolio use only**. Not affiliated with any real
bank. See the disclaimer at the top of this file.
