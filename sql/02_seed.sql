-- ===========================================================================
-- HFDC Bank - DEVELOPMENT-ONLY demo data. NEVER run this in production.
--
-- Demo credentials (also documented in README.md):
--   Customer 1: aarav@hfdcbank.test / Demo@1234   PIN 1234
--   Customer 2: priya@hfdcbank.test / Demo@1234   PIN 4321
--
-- The script is idempotent: INSERT IGNORE means re-running it will not create
-- duplicates or overwrite balances that the demo user has changed.
-- Run it against the hfdc_bank database:
--   mysql -u root -p hfdc_bank < sql/02_seed.sql
-- ===========================================================================


-- Passwords below are BCrypt(12) hashes of "Demo@1234".
-- PINs are BCrypt(12) hashes of "1234" (Aarav) and "4321" (Priya).
INSERT IGNORE INTO customers
  (customer_id, full_name, email, mobile_number, date_of_birth, address,
   password_hash, created_at, updated_at)
VALUES
  ('CID1000000001', 'Aarav Sharma', 'aarav@hfdcbank.test', '9876543210', '1992-04-18',
   '12 Marine Drive, Mumbai, Maharashtra',
   '$2a$12$fU29YBFBP0g7v34r4BRet.dfVa0LNbrTg.1f4kpR1.5hpebkk20ye',
   '2026-01-15 10:00:00', '2026-01-15 10:00:00'),
  ('CID1000000002', 'Priya Nair', 'priya@hfdcbank.test', '9876543211', '1995-09-02',
   '44 MG Road, Bengaluru, Karnataka',
   '$2a$12$fU29YBFBP0g7v34r4BRet.dfVa0LNbrTg.1f4kpR1.5hpebkk20ye',
   '2026-01-16 11:30:00', '2026-01-16 11:30:00');

INSERT IGNORE INTO accounts
  (customer_id, account_number, account_type, balance, transaction_pin_hash,
   account_status, created_at)
VALUES
  ('CID1000000001', 'HFDC1000000001', 'SAVINGS', 115000.00,
   '$2a$12$R0i4j5AymJzN2GEFG6zj3OdyfyEnpCZq1FYZMYry5ZXemA/PGIjJu',
   'ACTIVE', '2026-01-15 10:00:00'),
  ('CID1000000002', 'HFDC1000000002', 'SAVINGS', 78000.00,
   '$2a$12$xNR.tC/qqyLVMotsZwBRAuawo7bega/aG2lbb4ECIlL9t.fralZj6',
   'ACTIVE', '2026-01-16 11:30:00');

-- ---------------------------------------------------------------------------
-- Aarav's ledger: 50,000 + 90,000 - 15,000 - 10,000 + 10,000 - 10,000 = 115,000
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO transactions
  (reference_number, account_id, transaction_type, amount, description,
   transaction_status, created_at)
SELECT 'TXN20260610DEMO01', account_id, 'DEPOSIT', 50000.00,
       'Opening deposit', 'SUCCESS', '2026-06-10 09:15:00'
FROM accounts WHERE account_number = 'HFDC1000000001';

INSERT IGNORE INTO transactions
  (reference_number, account_id, transaction_type, amount, description,
   transaction_status, created_at)
SELECT 'TXN20260705DEMO02', account_id, 'DEPOSIT', 90000.00,
       'Salary credit (simulated)', 'SUCCESS', '2026-07-05 18:40:00'
FROM accounts WHERE account_number = 'HFDC1000000001';

INSERT IGNORE INTO transactions
  (reference_number, account_id, transaction_type, amount, description,
   transaction_status, created_at)
SELECT 'TXN20260812DEMO03', account_id, 'WITHDRAWAL', 15000.00,
       'Cash withdrawal', 'SUCCESS', '2026-08-12 12:05:00'
FROM accounts WHERE account_number = 'HFDC1000000001';

INSERT IGNORE INTO transactions
  (reference_number, account_id, transaction_type, amount, description,
   transaction_status, created_at)
SELECT 'TXN20260920DEMO04', account_id, 'TRANSFER_OUT', 10000.00,
       'Transfer to HFDC1000000002 (Priya Nair)', 'SUCCESS', '2026-09-20 16:20:00'
FROM accounts WHERE account_number = 'HFDC1000000001';

INSERT IGNORE INTO transactions
  (reference_number, account_id, transaction_type, amount, description,
   transaction_status, created_at)
SELECT 'TXN20261002DEMO05', account_id, 'TRANSFER_IN', 10000.00,
       'Transfer from HFDC1000000002 (Priya Nair)', 'SUCCESS', '2026-10-02 11:10:00'
FROM accounts WHERE account_number = 'HFDC1000000001';

INSERT IGNORE INTO transactions
  (reference_number, account_id, transaction_type, amount, description,
   transaction_status, created_at)
SELECT 'TXN20261108DEMO06', account_id, 'WITHDRAWAL', 10000.00,
       'Cash withdrawal', 'SUCCESS', '2026-11-08 17:45:00'
FROM accounts WHERE account_number = 'HFDC1000000001';

-- ---------------------------------------------------------------------------
-- Priya's ledger: 60,000 + 30,000 - 10,000 + 10,000 - 12,000 = 78,000
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO transactions
  (reference_number, account_id, transaction_type, amount, description,
   transaction_status, created_at)
SELECT 'TXN20260615DEMO07', account_id, 'DEPOSIT', 60000.00,
       'Opening deposit', 'SUCCESS', '2026-06-15 10:00:00'
FROM accounts WHERE account_number = 'HFDC1000000002';

INSERT IGNORE INTO transactions
  (reference_number, account_id, transaction_type, amount, description,
   transaction_status, created_at)
SELECT 'TXN20260720DEMO08', account_id, 'DEPOSIT', 30000.00,
       'Freelance income (simulated)', 'SUCCESS', '2026-07-20 14:30:00'
FROM accounts WHERE account_number = 'HFDC1000000002';

INSERT IGNORE INTO transactions
  (reference_number, account_id, transaction_type, amount, description,
   transaction_status, created_at)
SELECT 'TXN20260920DEMO09', account_id, 'TRANSFER_OUT', 10000.00,
       'Transfer to HFDC1000000001 (Aarav Sharma)', 'SUCCESS', '2026-09-20 16:20:00'
FROM accounts WHERE account_number = 'HFDC1000000002';

INSERT IGNORE INTO transactions
  (reference_number, account_id, transaction_type, amount, description,
   transaction_status, created_at)
SELECT 'TXN20261002DEMO10', account_id, 'TRANSFER_IN', 10000.00,
       'Transfer from HFDC1000000001 (Aarav Sharma)', 'SUCCESS', '2026-10-02 11:10:00'
FROM accounts WHERE account_number = 'HFDC1000000002';

INSERT IGNORE INTO transactions
  (reference_number, account_id, transaction_type, amount, description,
   transaction_status, created_at)
SELECT 'TXN20261105DEMO11', account_id, 'WITHDRAWAL', 12000.00,
       'Cash withdrawal', 'SUCCESS', '2026-11-05 19:00:00'
FROM accounts WHERE account_number = 'HFDC1000000002';

-- ---------------------------------------------------------------------------
-- Transfer audit rows (reference is unique, so re-running is safe).
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO transfers
  (reference_number, sender_account_id, receiver_account_id, amount, remarks,
   transfer_status, created_at)
SELECT 'TRF20260920DEMO01', sa.account_id, ra.account_id, 10000.00,
       'Dinner split', 'SUCCESS', '2026-09-20 16:20:00'
FROM accounts sa, accounts ra
WHERE sa.account_number = 'HFDC1000000001' AND ra.account_number = 'HFDC1000000002';

INSERT IGNORE INTO transfers
  (reference_number, sender_account_id, receiver_account_id, amount, remarks,
   transfer_status, created_at)
SELECT 'TRF20261002DEMO02', sa.account_id, ra.account_id, 10000.00,
       'Shared expense', 'SUCCESS', '2026-10-02 11:10:00'
FROM accounts sa, accounts ra
WHERE sa.account_number = 'HFDC1000000002' AND ra.account_number = 'HFDC1000000001';

-- ---------------------------------------------------------------------------
-- Saved beneficiaries (one each way).
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO beneficiaries
  (customer_id, beneficiary_name, beneficiary_account_number, created_at)
VALUES
  ('CID1000000001', 'Priya Nair', 'HFDC1000000002', '2026-09-01 09:00:00'),
  ('CID1000000002', 'Aarav Sharma', 'HFDC1000000001', '2026-09-01 09:05:00');
