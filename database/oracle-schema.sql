-- BillSplit Oracle schema (Oracle Database 21c XE / 19c+)
-- Run as the application schema user after creating that user.
-- Do not run this until Oracle XE is installed and a schema user exists.

-- Example (run as a DBA; replace password yourself):
--   CREATE USER billsplit IDENTIFIED BY "choose-a-strong-password";
--   GRANT CREATE SESSION, CREATE TABLE, CREATE VIEW, CREATE SEQUENCE TO billsplit;
--   ALTER USER billsplit QUOTA UNLIMITED ON USERS;
-- Then connect as billsplit and run the statements below.
-- IDENTITY columns require CREATE SEQUENCE on Oracle 12c+.

CREATE TABLE users (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR2(255) NOT NULL,
    email VARCHAR2(255) NOT NULL,
    password VARCHAR2(255) NOT NULL,
    CONSTRAINT users_email_uk UNIQUE (email),
    CONSTRAINT users_name_uk UNIQUE (name)
);

CREATE TABLE trips (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR2(255) NOT NULL,
    start_date TIMESTAMP
);

CREATE TABLE trip_users (
    trip_id NUMBER NOT NULL,
    user_id NUMBER NOT NULL,
    CONSTRAINT trip_users_pk PRIMARY KEY (trip_id, user_id),
    CONSTRAINT trip_users_trip_fk FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT trip_users_user_fk FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE transactions (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trip_id NUMBER,
    amount NUMBER(10, 2),
    note VARCHAR2(1000),
    txn_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT transactions_trip_fk FOREIGN KEY (trip_id) REFERENCES trips (id)
);

CREATE TABLE transaction_users (
    transaction_id NUMBER NOT NULL,
    user_id NUMBER NOT NULL,
    borrow NUMBER(10, 2) DEFAULT 0,
    lent NUMBER(10, 2) DEFAULT 0,
    CONSTRAINT transaction_users_pk PRIMARY KEY (transaction_id, user_id),
    CONSTRAINT transaction_users_txn_fk FOREIGN KEY (transaction_id) REFERENCES transactions (id),
    CONSTRAINT transaction_users_user_fk FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX trip_users_user_idx ON trip_users (user_id);
CREATE INDEX transactions_trip_idx ON transactions (trip_id);
CREATE INDEX transaction_users_user_idx ON transaction_users (user_id);
