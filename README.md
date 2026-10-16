# BillSplit (Jakarta / Oracle migration)

Vanilla HTML/CSS/JavaScript client with Jakarta Servlet 6.0, JDBC, and Oracle SQL. This project lives only in `Billsplit AI`. The original Node/React app in `Billsplit orginal` is left unchanged.

## Features

- Signup and login (session cookie, BCrypt password hashes)
- Create trips and add existing users by username
- Add expenses with paid-by and split-to (custom amounts or equal split)
- Expense history and minimized settlements
- Optional AI trip summary when `GEMINI_API_KEY` is set; otherwise the UI reports that summaries are not configured

## Requirements

- JDK 17
- Apache Maven 3.8+
- Apache Tomcat 10.1
- Oracle Database 21c XE (or compatible) when you want to run the app against a live database

Oracle is **not** required to compile, unit-test, or package the WAR.

## Configuration (no secrets in source)

Do not put database passwords or API keys in Java or JS. Use environment variables or an external file.

Environment variables:

| Variable | Purpose |
|---|---|
| `BILLSPLIT_DB_URL` | JDBC URL, for example `jdbc:oracle:thin:@localhost:1521/XEPDB1` |
| `BILLSPLIT_DB_USER` | Oracle schema username |
| `BILLSPLIT_DB_PASSWORD` | Oracle schema password |
| `GEMINI_API_KEY` | Optional Google Gemini key for trip summaries |
| `BILLSPLIT_CONFIG` | Optional path to a properties file |

Example file: `config/application.properties.example`. Copy it to `config/application.properties` (gitignored) or to `$CATALINA_BASE/conf/billsplit.properties` and replace the placeholders with values you choose. Values starting with `YOUR_` are ignored.

Tomcat can be started with:

```sh
export BILLSPLIT_DB_URL='jdbc:oracle:thin:@localhost:1521/XEPDB1'
export BILLSPLIT_DB_USER='your_schema_user'
export BILLSPLIT_DB_PASSWORD='your_schema_password'
# optional
export GEMINI_API_KEY='your_gemini_key'
```

## Oracle schema setup

1. Install Oracle XE and create a schema user (choose the password yourself).
2. Connect as that user.
3. Run `database/oracle-schema.sql`.

The script uses identity columns, `VARCHAR2`, and `txn_date` instead of PostgreSQL `SERIAL` / `TEXT` / `date`.

## Build, test, and package

From `Billsplit AI`:

```sh
mvn test
mvn package
```

The WAR is written to `target/billsplit.war`.

### Tests

Always run without Oracle:

- Password hashing (BCrypt)
- Expense validation
- Equal-split remainder handling
- Settlement minimization
- Config loading and unconfigured Gemini summaries
- `OracleConnectionTest.reportsUnconfiguredWhenDatabaseSettingsAreAbsent`
- `OracleConnectionTest.getConnectionFailsFastWhenDatabaseSettingsAreAbsent`

Requires a live database (skipped unless `BILLSPLIT_DB_URL` is set):

- `OracleConnectionTest.connectsWhenOracleIsConfigured`

DAO/servlet flows against Oracle are unverified until XE is installed.

## Deploy on Tomcat 10.1

1. Build `target/billsplit.war`.
2. Copy it to Tomcat `webapps/`.
3. Set the environment variables above (or place `billsplit.properties` under `conf/`).
4. Start Tomcat with its own scripts.
5. Open `http://localhost:8080/billsplit/`.

If the WAR deploys and Oracle is not configured, `/api/health` still returns JSON with `"databaseConfigured": false`. Authenticated API calls return HTTP 503 until JDBC settings are provided.

## API (session cookie)

| Method | Path | Notes |
|---|---|---|
| POST | `/api/signup` | `{username, email, password}` (password min 8 chars) |
| POST | `/api/login` | `{username, password}` |
| POST | `/api/logout` | |
| GET | `/api/me` | Current session user |
| GET | `/api/health` | Config flags; no DB required |
| GET/POST | `/api/trips` | List / create |
| GET/POST | `/api/trips/{id}/users` | Members; POST `{username}` |
| GET/POST | `/api/trips/{id}/transactions` | History / create expense |
| POST | `/api/trips/{id}/summary` | Gemini or not-configured message |

Trip membership is checked on member and transaction routes.

## Security changes from the original

- Passwords hashed with BCrypt (no plaintext storage)
- HttpOnly session cookie instead of a hardcoded JWT secret
- Prepared statements only
- Generated Oracle identity used on signup (fixes the original `insertId` bug)
- Login uses the `username` field against `users.name` (fixes the original field mismatch)
- Gemini key is server-side and optional
- Usernames are unique so login is unambiguous
