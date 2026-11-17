# HFDC Bank — Project Status

**Last updated:** 2026-11-17
**Overall status:** Application complete, verified locally, and published to
GitHub. Public deployment is **pending hosting/database credentials**.

> HFDC Bank is a fictional educational banking simulation. It is not a real
> financial institution.

---

## 1. Summary

| Area | Status | Evidence |
| --- | --- | --- |
| Backend (Spring Boot 3.3.5, Java 21) | ✅ Complete | Compiles, packages, runs on port 8080 |
| Database (MySQL 8, Spring JDBC) | ✅ Complete | Schema + idempotent seed; 6 tables; demo data loaded |
| Authentication + CSRF | ✅ Complete | Cookie login, BCrypt, JWT, double-submit CSRF verified |
| Banking features | ✅ Complete | Deposit, withdrawal, atomic transfer, history, beneficiaries |
| Dashboard + profile | ✅ Complete | Live DB figures and chart data |
| Frontend (React 18 + Vite) | ✅ Complete | 18 pages, production build succeeds |
| Automated tests | ✅ Complete | 67/67 passing, including concurrency |
| Documentation | ✅ Complete | README + API/architecture/testing/deployment docs |
| Docker / deployment config | ✅ Prepared | Dockerfile, render.yaml, `.env.example` |
| GitHub repository | ✅ Published | <https://github.com/Yuvaraj-Kumar-9/Net-Banking-Application> (`main` at `e92a121`) |
| Public deployment | ⏸ Blocked | No hosting/database credentials; no URL claimed |

---

## 2. Completed deliverables

### Backend

- Layered package structure under `com.hfdcbank`: `config`, `controller`,
  `service`, `repository`, `entity`, `dto`, `security`, `exception`, `util`.
- Spring JDBC only (`JdbcTemplate`), **no JPA/Hibernate**.
- `DECIMAL(19,2)` / `BigDecimal` money handling with `HALF_UP` normalisation.
- BCrypt(12) password and transaction-PIN hashing.
- JWT (JJWT 0.12.6) in an HttpOnly `hfdc_token` cookie, 1-day expiry.
- Double-submit CSRF (`XSRF-TOKEN` cookie + `X-XSRF-TOKEN` header).
- Bean Validation on every request record.
- Global `ApiError` handler for 400/401/403/404/405/409/500.
- Swagger/OpenAPI at `/swagger-ui.html`.
- Atomic transfer with `TransactionTemplate`, ascending row locks, SQL balance
  guard, two ledger rows and one audit row.
- SPA static serving (`SpaWebConfig`) for single-origin production.

### Database

- `sql/01_schema.sql`: 6 tables, unique keys, indexes, check constraints and
  foreign keys.
- `sql/02_seed.sql`: 2 customers, 2 accounts, 11 ledger rows, 2 transfers,
  2 beneficiaries; idempotent via `INSERT IGNORE`.
- `scripts/dev-db-reset.cmd`: drops/recreates `hfdc_bank`, loads schema/seed,
  creates the local `hfdc_user`/`change_me` login.
- `scripts/start-dev-mysql.cmd`: self-contained MySQL 8 on port 33306.
- Cloud-friendly SQL: the database is selected on the command line, so managed
  providers do not need `CREATE DATABASE` privileges.

### Frontend

- 18 pages: landing, about, services, contact, login, register, forgot password,
  404, dashboard, account, balance, deposit, withdraw, transfer, transactions,
  beneficiaries, profile, change password.
- Shared components: public/protected layouts, navbar, sidebar, logo, hero
  illustration, stat cards, confirmation dialog, notifications, loading states.
- Auth and notification contexts; Axios CSRF interceptor; 401 handling.
- Responsive navy/white/light-blue design system.
- Landing page includes hero, services, why-choose, security, clearly
  illustrative testimonials, FAQ, footer and disclaimer.
- Production build succeeds (single-chunk warning only).
- Vite proxies `/api` to port 8080 in development; production uses same origin.

### Testing

```
Tests run: 67, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Breakdown: Money 4, JWT 5, Auth 14, Beneficiaries 8, Profile 8, Transactions 17,
Transfers 10, Concurrency 1.

The concurrency test proves 20 simultaneous ₹100 transfers from ₹1,000 produce
exactly 10 successes, 10 `InsufficientBalanceException`s, a ₹0 sender, a ₹1,000
receiver and 20 consistent ledger rows.

### Documentation and tooling

- `README.md`: features, stack, architecture, quick start, demo accounts, API
  summary, security, testing, deployment status.
- `docs/API.md`: every endpoint, payloads, CSRF flow, validation and errors.
- `docs/ARCHITECTURE.md`: layers, ERD, transfer transaction, security, frontend.
- `docs/TESTING.md`: test inventory, concurrency proof, manual checklist.
- `docs/DEPLOYMENT.md`: Render/Railway/split options, env vars, hardening.
- `.github/workflows/ci.yml`: backend tests + frontend build.
- `Dockerfile`, `.dockerignore`, `render.yaml`, `.env.example`, `.gitignore`.

---

## 3. Verification evidence

Executed against the real backend and the isolated MySQL instance on port 33306
with cookie + CSRF handling:

- Login, `/auth/me`, dashboard, account, balance.
- Transaction list, detail, filters, pagination.
- Deposit, withdrawal (correct PIN, wrong PIN, over-limit, insufficient balance).
- Transfer (correct PIN, wrong PIN, self, insufficient, unknown account).
- Registration and duplicate email (`409`).
- Beneficiary add/list/search/delete, duplicate (`409`), self (`400`),
  unknown (`404`).
- Profile read/update/revert; change password with wrong current password.
- CSRF: missing header and bogus header both return `403`.
- Unknown API path returns JSON `404`; wrong method returns `405`.
- SPA: `/` and `/dashboard` return the React app; missing static files return
  `404`; Swagger UI returns `200`.
- Production build: `npm run build` and `mvn -Pfrontend -DskipTests package`
  succeed.

The demo database was reset after end-to-end testing, so Aarav starts at
₹1,15,000.00 and Priya at ₹78,000.00.

---

## 4. Current local environment

| Item | Value |
| --- | --- |
| Project root | `C:\Users\personal\Documents\EarnBridge\HFDC-Bank-FullStack` |
| MySQL | `127.0.0.1:33306`, root (no password), `hfdc_bank` seeded |
| App DB user | `hfdc_user` / `change_me` (local only) |
| Backend | Stopped. Start it with `scripts\start-app.cmd` (serves UI + API on port 8080) |
| One-click launcher | `scripts\start-app.cmd` (starts MySQL, auto-seeds a brand-new data dir, runs the jar) |
| Stop script | `scripts\stop-app.cmd` (stops the backend and the dev MySQL on 33306) |
| Frontend dev | Not running; production build in `frontend/dist` |
| Maven | `C:\Users\personal\Documents\EarnBridge\tools\apache-maven-3.9.9` |
| Git | Installed (2.55.0); `main` tracks `origin` at <https://github.com/Yuvaraj-Kumar-9/Net-Banking-Application> |
| Docker | Not installed |
| GitHub CLI | Not installed |

Demo accounts:

| Customer | Email | Password | PIN |
| --- | --- | --- | --- |
| Aarav Sharma | `aarav@hfdcbank.test` | `Demo@1234` | `1234` |
| Priya Nair | `priya@hfdcbank.test` | `Demo@1234` | `4321` |

---

## 5. Pending / blocked

### GitHub publishing — complete

- Repository: <https://github.com/Yuvaraj-Kumar-9/Net-Banking-Application>
  (the repository name differs from the local folder name; the application is
  still HFDC Bank).
- `main` was pushed on 2026-11-17 and tracks `origin/main`.
- The published history contains these commits:
  1. `chore:` scaffold, Maven build, SQL schema and dev scripts
  2. `feat(backend):` layered Spring JDBC banking API with JWT cookie auth
  3. `test(backend):` 67 unit and concurrency tests
  4. `feat(frontend):` responsive React net banking UI
  5. `build:` Docker image, Render blueprint and GitHub Actions CI
  6. `docs:` README, API, architecture, testing and deployment guides
  7. `docs:` record local git history and remaining approval gates
- 145 tracked files. `node_modules`, `target`, `frontend/dist` and local
  database files are excluded by `.gitignore`.

### Public deployment — awaiting credentials/approval

- No Render/Railway/Aiven/TiDB account or credentials were provided.
- No live URL exists and none is claimed.
- Docker is not installed locally, so the image was not built on this machine.
- Required from the user (via the provider dashboards, **never pasted in chat**):
  - hosting account access or an invitation,
  - managed MySQL 8 host/port/database/user/password,
  - approval to create the services.
- Follow `docs/DEPLOYMENT.md` once available, then update the deployment record.

---

## 6. Known limitations

- Deposits are simulated; no payment gateway.
- No email/SMS provider; reset tokens are screen-only in dev mode.
- One savings account per customer; no admin, statements export, scheduled
  payments, 2FA or account closure.
- No rate limiting/CAPTCHA; add at the edge before public use.
- Unknown transaction `type` values are ignored rather than rejected.
- Frontend bundle is one chunk > 500 kB; code-splitting is a future improvement.
- No Flyway/Liquibase; schema is applied from SQL files.
- No MySQL Testcontainers test; concurrency runs on H2 in MySQL mode.
- Free hosting tiers may sleep, causing cold starts.
- Seed/demo credentials are public and must not protect real data.

---

## 7. Next steps

1. Get deployment credentials through the provider dashboard and deploy using
   `docs/DEPLOYMENT.md`.
2. Verify the live checklist (health, login, CSRF, transfer, SPA routes, cookies).
3. Update this file and the README with the verified URL and deployment record.
4. Optional hardening: rate limiting, email provider, Flyway, Testcontainers,
   frontend code-splitting.

---

## 8. How to resume

### Everyday use — one command

```bat
cd /d C:\Users\personal\Documents\EarnBridge\HFDC-Bank-FullStack
scripts\start-app.cmd
```

Wait for `Started HfdcBankApplication`, then open <http://localhost:8080>.
Keep the window open; `Ctrl+C` stops the website. `scripts\stop-app.cmd` stops
both the backend and the dev MySQL instance.

### Developer mode — hot reload on 5173

```bat
REM Start the isolated database (if not running)
scripts\start-dev-mysql.cmd

REM Optional: restore the demo data
scripts\dev-db-reset.cmd

REM Start the API
scripts\run-backend.cmd

REM Start the UI (second terminal)
scripts\run-frontend.cmd
```

Then open <http://localhost:5173>.

Useful checks:

```bash
mvn test
curl -s http://localhost:8080/api/auth/health
```
