# BillSplit migration checklist

Source: `Billsplit orginal` (read-only). Destination: `Billsplit AI` only.

## Stage 0 — source inventory

- [x] Full-stack expense splitter: React 19 CRA frontend, Node/Express backend, PostgreSQL.
- [x] Workflows: signup/login, trips, add existing users by username, expenses with paid-by/split-to (custom + equal), history, client-side settlements, Gemini trip summary.
- [x] Bugs to fix: plaintext passwords; JWT `insertId` (MySQL-style) on Postgres signup; login JWT uses missing `username` field; login input bound to `formData.name`; hardcoded DB password, JWT secret, Gemini key; unused `bcrypt`; delete/update transaction routes commented out.
- [x] Oracle is not installed locally — compile/tests must not require a live database.

## Stage 1 — Maven WAR scaffold

- [x] `pom.xml`: Java 17, WAR, Jakarta Servlet 6.0 (`provided`), Oracle JDBC, jBCrypt, JSON, JUnit 5.
- [x] Standard layout: `src/main/java`, `src/main/webapp`, `src/test/java`, `database/`, `config/` example only.
- [x] External config (env vars or properties file). No credentials or API keys in source.

## Stage 2 — Oracle schema and config

- [x] Oracle DDL adapted from PostgreSQL (`SERIAL` → `IDENTITY`, `TEXT` → `VARCHAR2`, reserved `DATE` → `txn_date`).
- [x] Unique username (`name`) so login is unambiguous.
- [x] Setup instructions without invented credentials.

## Stage 3 — domain, validation, settlements (DB-free)

- [x] BCrypt password hashing.
- [x] Expense validation (note, matching paid/split totals, non-negative shares).
- [x] Equal-split helper that preserves 2-decimal totals.
- [x] Settlement minimizer ported from original JS.

## Stage 4 — JDBC DAOs and services

- [x] Prepared statements only.
- [x] Session auth + trip membership authorization.
- [x] Signup returns generated Oracle identity (not `insertId`).
- [x] Login by username (`name`) with BCrypt verify.
- [x] Lazy DB connect so the WAR starts without Oracle.

## Stage 5 — servlets and JSON API

- [x] `/api/signup`, `/api/login`, `/api/logout`, `/api/me`
- [x] `/api/trips`, `/api/trips/{id}/users`, `/api/trips/{id}/transactions`
- [x] `/api/trips/{id}/summary` — Gemini if `GEMINI_API_KEY` set; otherwise a configured-off message.

## Stage 6 — vanilla HTML/CSS/JS UI

- [x] Login/signup, trip list/create, members, expenses, equal/custom split, history, settlements, summaries.
- [x] Visual language based on original CSS (teal header, two-column layout, modals).

## Stage 7 — tests and package

- [x] Unit tests for hashing, validation, equal split, settlements, config loading.
- [x] Document/skip Oracle-dependent tests.
- [x] `mvn test package` without a live database.

## Remaining (requires local Oracle)

- [ ] Install Oracle Database 21c XE (or compatible) and create a schema user with `CREATE SESSION`, `CREATE TABLE`, `CREATE SEQUENCE`, and quota.
- [ ] Run `database/oracle-schema.sql` as that user.
- [ ] Set `BILLSPLIT_DB_URL`, `BILLSPLIT_DB_USER`, and `BILLSPLIT_DB_PASSWORD` (or a non-placeholder properties file).
- [ ] Re-run `OracleConnectionTest.connectsWhenOracleIsConfigured` and deploy `target/billsplit.war` to Tomcat 10.1.
- [ ] Optional: set `GEMINI_API_KEY` for live trip summaries.
