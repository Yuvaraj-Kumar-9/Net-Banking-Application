package com.hfdcbank.repository;

import com.hfdcbank.entity.Account;
import com.hfdcbank.util.GeneratedKeys;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/**
 * SQL for accounts. The credit()/debit() methods are atomic: arithmetic is
 * performed by MySQL, and debit() additionally checks the balance in the same
 * UPDATE, so two concurrent debits cannot both spend the same money.
 */
@Repository
public class AccountRepository {

    private static final String COLUMNS =
            "account_id, customer_id, account_number, account_type, balance, "
                    + "transaction_pin_hash, account_status, created_at";

    private static final RowMapper<Account> MAPPER = (rs, rowNum) -> new Account(
            rs.getLong("account_id"),
            rs.getString("customer_id"),
            rs.getString("account_number"),
            rs.getString("account_type"),
            rs.getBigDecimal("balance"),
            rs.getString("transaction_pin_hash"),
            rs.getString("account_status"),
            rs.getTimestamp("created_at").toLocalDateTime()
    );

    private final JdbcTemplate jdbc;

    public AccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Account> findByCustomerId(String customerId) {
        List<Account> rows = jdbc.query(
                "SELECT " + COLUMNS + " FROM accounts WHERE customer_id = ?", MAPPER, customerId);
        return rows.stream().findFirst();
    }

    public Optional<Account> findByAccountNumber(String accountNumber) {
        List<Account> rows = jdbc.query(
                "SELECT " + COLUMNS + " FROM accounts WHERE account_number = ?", MAPPER, accountNumber);
        return rows.stream().findFirst();
    }

    public Optional<Account> findById(long accountId) {
        List<Account> rows = jdbc.query(
                "SELECT " + COLUMNS + " FROM accounts WHERE account_id = ?", MAPPER, accountId);
        return rows.stream().findFirst();
    }

    /**
     * Pessimistic row lock. Call ONLY from inside a @Transactional /
     * TransactionTemplate block: another transfer to the same account blocks
     * until this transaction commits or rolls back.
     */
    public Optional<Account> lockById(long accountId) {
        List<Account> rows = jdbc.query(
                "SELECT " + COLUMNS + " FROM accounts WHERE account_id = ? FOR UPDATE", MAPPER, accountId);
        return rows.stream().findFirst();
    }

    public boolean existsByAccountNumber(String accountNumber) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM accounts WHERE account_number = ?", Integer.class, accountNumber);
        return count != null && count > 0;
    }

    public String findPinHashById(long accountId) {
        return jdbc.queryForObject(
                "SELECT transaction_pin_hash FROM accounts WHERE account_id = ?", String.class, accountId);
    }

    public BigDecimal findBalanceById(long accountId) {
        BigDecimal value = jdbc.queryForObject(
                "SELECT balance FROM accounts WHERE account_id = ?", BigDecimal.class, accountId);
        return value == null ? BigDecimal.ZERO : value;
    }

    public int credit(long accountId, BigDecimal amount) {
        return jdbc.update(
                "UPDATE accounts SET balance = balance + ? WHERE account_id = ? AND account_status = 'ACTIVE'",
                amount, accountId);
    }

    /** Returns 0 when the balance is insufficient - no money leaves the account. */
    public int debit(long accountId, BigDecimal amount) {
        return jdbc.update(
                "UPDATE accounts SET balance = balance - ? "
                        + "WHERE account_id = ? AND account_status = 'ACTIVE' AND balance >= ?",
                amount, accountId, amount);
    }

    public long insert(Account account) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO accounts (customer_id, account_number, account_type, balance, "
                            + "transaction_pin_hash, account_status) VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, account.customerId());
            ps.setString(2, account.accountNumber());
            ps.setString(3, account.accountType());
            ps.setBigDecimal(4, account.balance());
            ps.setString(5, account.transactionPinHash());
            ps.setString(6, account.accountStatus());
            return ps;
        }, keyHolder);
        return GeneratedKeys.require(keyHolder, "account_id",
                "Could not read generated account id");
    }
}
