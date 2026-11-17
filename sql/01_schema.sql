-- ===========================================================================
-- HFDC Bank - database schema (MySQL 8)
-- Create the database first, then run this file against it:
--   mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS hfdc_bank
--     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
--   mysql -u root -p hfdc_bank < sql/01_schema.sql
-- All money columns use DECIMAL(19,2). Java maps them to BigDecimal.
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- customers: one row per registered person. Passwords/PINs are never stored
-- here in plain text - only BCrypt hashes.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customers (
  customer_id    VARCHAR(20)  NOT NULL,
  full_name      VARCHAR(120) NOT NULL,
  email          VARCHAR(160) NOT NULL,
  mobile_number  VARCHAR(20)  NOT NULL,
  date_of_birth  DATE         NOT NULL,
  address        VARCHAR(255) NOT NULL,
  password_hash  VARCHAR(100) NOT NULL,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (customer_id),
  UNIQUE KEY uq_customers_email (email),
  UNIQUE KEY uq_customers_mobile (mobile_number)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- accounts: exactly one default savings account is created at registration.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS accounts (
  account_id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  customer_id          VARCHAR(20)  NOT NULL,
  account_number       VARCHAR(20)  NOT NULL,
  account_type         VARCHAR(30)  NOT NULL DEFAULT 'SAVINGS',
  balance              DECIMAL(19,2) NOT NULL DEFAULT 0.00,
  transaction_pin_hash VARCHAR(100) NOT NULL,
  account_status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (account_id),
  UNIQUE KEY uq_accounts_number (account_number),
  KEY idx_accounts_customer (customer_id),
  CONSTRAINT chk_accounts_balance CHECK (balance >= 0),
  CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id)
    REFERENCES customers (customer_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- transactions: immutable ledger of deposits, withdrawals and transfers.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transactions (
  transaction_id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  reference_number   VARCHAR(40)  NOT NULL,
  account_id         BIGINT UNSIGNED NOT NULL,
  transaction_type   VARCHAR(20)  NOT NULL,
  amount             DECIMAL(19,2) NOT NULL,
  description        VARCHAR(255)     NULL,
  transaction_status VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS',
  created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (transaction_id),
  UNIQUE KEY uq_transactions_ref (reference_number),
  KEY idx_transactions_account_created (account_id, created_at),
  KEY idx_transactions_type (transaction_type),
  CONSTRAINT chk_transactions_amount CHECK (amount > 0),
  CONSTRAINT fk_transactions_account FOREIGN KEY (account_id)
    REFERENCES accounts (account_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- transfers: one row per completed transfer (both ledger rows live in
-- transactions). Written and read inside the same DB transaction as the
-- balance changes.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transfers (
  transfer_id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  reference_number    VARCHAR(40)  NOT NULL,
  sender_account_id   BIGINT UNSIGNED NOT NULL,
  receiver_account_id BIGINT UNSIGNED NOT NULL,
  amount              DECIMAL(19,2) NOT NULL,
  remarks             VARCHAR(255)     NULL,
  transfer_status     VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS',
  created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (transfer_id),
  UNIQUE KEY uq_transfers_ref (reference_number),
  KEY idx_transfers_sender (sender_account_id, created_at),
  CONSTRAINT chk_transfers_amount CHECK (amount > 0),
  CONSTRAINT fk_transfers_sender FOREIGN KEY (sender_account_id)
    REFERENCES accounts (account_id) ON DELETE RESTRICT,
  CONSTRAINT fk_transfers_receiver FOREIGN KEY (receiver_account_id)
    REFERENCES accounts (account_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- beneficiaries: saved accounts owned by a customer.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS beneficiaries (
  beneficiary_id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  customer_id                VARCHAR(20)  NOT NULL,
  beneficiary_name           VARCHAR(120) NOT NULL,
  beneficiary_account_number VARCHAR(20)  NOT NULL,
  created_at                 DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (beneficiary_id),
  UNIQUE KEY uq_beneficiary_pair (customer_id, beneficiary_account_number),
  KEY idx_beneficiaries_customer (customer_id),
  CONSTRAINT fk_beneficiaries_customer FOREIGN KEY (customer_id)
    REFERENCES customers (customer_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- password_resets: short-lived tokens for the forgot-password flow.
-- Only a SHA-256 hash of the token is stored, never the token itself.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS password_resets (
  reset_id    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  customer_id VARCHAR(20)  NOT NULL,
  token_hash  VARCHAR(64)  NOT NULL,
  expires_at  DATETIME     NOT NULL,
  used        BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (reset_id),
  KEY idx_resets_hash (token_hash),
  CONSTRAINT fk_resets_customer FOREIGN KEY (customer_id)
    REFERENCES customers (customer_id) ON DELETE CASCADE
) ENGINE=InnoDB;
