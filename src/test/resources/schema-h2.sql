-- ===========================================================================
-- HFDC Bank - H2 schema used by the automated tests only.
-- This mirrors sql/01_schema.sql (MySQL 8) but removes MySQL-only syntax such
-- as UNSIGNED, ENGINE and ON UPDATE. Column names, constraints and types match
-- so the production repositories run unchanged.
-- ===========================================================================

DROP TABLE IF EXISTS password_resets;
DROP TABLE IF EXISTS beneficiaries;
DROP TABLE IF EXISTS transfers;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS customers;

CREATE TABLE customers (
  customer_id    VARCHAR(20)  NOT NULL,
  full_name      VARCHAR(120) NOT NULL,
  email          VARCHAR(160) NOT NULL,
  mobile_number  VARCHAR(20)  NOT NULL,
  date_of_birth  DATE         NOT NULL,
  address        VARCHAR(255) NOT NULL,
  password_hash  VARCHAR(100) NOT NULL,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (customer_id),
  CONSTRAINT uq_customers_email UNIQUE (email),
  CONSTRAINT uq_customers_mobile UNIQUE (mobile_number)
);

CREATE TABLE accounts (
  account_id           BIGINT AUTO_INCREMENT NOT NULL,
  customer_id          VARCHAR(20)  NOT NULL,
  account_number       VARCHAR(20)  NOT NULL,
  account_type         VARCHAR(30)  NOT NULL DEFAULT 'SAVINGS',
  balance              DECIMAL(19,2) NOT NULL DEFAULT 0.00,
  transaction_pin_hash VARCHAR(100) NOT NULL,
  account_status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (account_id),
  CONSTRAINT uq_accounts_number UNIQUE (account_number),
  CONSTRAINT chk_accounts_balance CHECK (balance >= 0),
  CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id)
    REFERENCES customers (customer_id)
);

CREATE TABLE transactions (
  transaction_id     BIGINT AUTO_INCREMENT NOT NULL,
  reference_number   VARCHAR(40)  NOT NULL,
  account_id         BIGINT       NOT NULL,
  transaction_type   VARCHAR(20)  NOT NULL,
  amount             DECIMAL(19,2) NOT NULL,
  description        VARCHAR(255)     NULL,
  transaction_status VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS',
  created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (transaction_id),
  CONSTRAINT uq_transactions_ref UNIQUE (reference_number),
  CONSTRAINT chk_transactions_amount CHECK (amount > 0),
  CONSTRAINT fk_transactions_account FOREIGN KEY (account_id)
    REFERENCES accounts (account_id)
);

CREATE TABLE transfers (
  transfer_id         BIGINT AUTO_INCREMENT NOT NULL,
  reference_number    VARCHAR(40)  NOT NULL,
  sender_account_id   BIGINT       NOT NULL,
  receiver_account_id BIGINT       NOT NULL,
  amount              DECIMAL(19,2) NOT NULL,
  remarks             VARCHAR(255)     NULL,
  transfer_status     VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS',
  created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (transfer_id),
  CONSTRAINT uq_transfers_ref UNIQUE (reference_number),
  CONSTRAINT chk_transfers_amount CHECK (amount > 0),
  CONSTRAINT fk_transfers_sender FOREIGN KEY (sender_account_id)
    REFERENCES accounts (account_id),
  CONSTRAINT fk_transfers_receiver FOREIGN KEY (receiver_account_id)
    REFERENCES accounts (account_id)
);

CREATE TABLE beneficiaries (
  beneficiary_id             BIGINT AUTO_INCREMENT NOT NULL,
  customer_id                VARCHAR(20)  NOT NULL,
  beneficiary_name           VARCHAR(120) NOT NULL,
  beneficiary_account_number VARCHAR(20)  NOT NULL,
  created_at                 DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (beneficiary_id),
  CONSTRAINT uq_beneficiary_pair UNIQUE (customer_id, beneficiary_account_number),
  CONSTRAINT fk_beneficiaries_customer FOREIGN KEY (customer_id)
    REFERENCES customers (customer_id)
);

CREATE TABLE password_resets (
  reset_id    BIGINT AUTO_INCREMENT NOT NULL,
  customer_id VARCHAR(20)  NOT NULL,
  token_hash  VARCHAR(64)  NOT NULL,
  expires_at  DATETIME     NOT NULL,
  used        BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (reset_id),
  CONSTRAINT fk_resets_customer FOREIGN KEY (customer_id)
    REFERENCES customers (customer_id)
);
