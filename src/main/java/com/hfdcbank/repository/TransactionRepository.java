package com.hfdcbank.repository;

import com.hfdcbank.entity.Transaction;
import com.hfdcbank.util.GeneratedKeys;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** SQL for the transactions ledger and the transfers audit table. */
@Repository
public class TransactionRepository {

    public record MonthlyTotals(String month, BigDecimal deposits, BigDecimal withdrawals) {
    }

    public record TransferRow(String referenceNumber, String counterpartyAccountNumber,
                              BigDecimal amount, String remarks, LocalDateTime createdAt, String direction) {
    }

    private static final String COLUMNS =
            "transaction_id, reference_number, account_id, transaction_type, amount, "
                    + "description, transaction_status, created_at";

    /** Same columns with the t. alias, used by the filtered/paginated query. */
    private static final String T_COLUMNS =
            "t.transaction_id, t.reference_number, t.account_id, t.transaction_type, t.amount, "
                    + "t.description, t.transaction_status, t.created_at";

    private static final RowMapper<Transaction> MAPPER = (rs, rowNum) -> new Transaction(
            rs.getLong("transaction_id"),
            rs.getString("reference_number"),
            rs.getLong("account_id"),
            rs.getString("transaction_type"),
            rs.getBigDecimal("amount"),
            rs.getString("description"),
            rs.getString("transaction_status"),
            rs.getTimestamp("created_at").toLocalDateTime()
    );

    private final JdbcTemplate jdbc;

    public TransactionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(long accountId, String reference, String type, BigDecimal amount,
                       String description, String status) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO transactions (reference_number, account_id, transaction_type, "
                            + "amount, description, transaction_status) VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, reference);
            ps.setLong(2, accountId);
            ps.setString(3, type);
            ps.setBigDecimal(4, amount);
            ps.setString(5, description);
            ps.setString(6, status);
            return ps;
        }, keyHolder);
        return GeneratedKeys.require(keyHolder, "transaction_id",
                "Could not read generated transaction id");
    }

    public Optional<Transaction> findByReferenceNumber(String referenceNumber) {
        List<Transaction> rows = jdbc.query(
                "SELECT " + COLUMNS + " FROM transactions WHERE reference_number = ?",
                MAPPER, referenceNumber);
        return rows.stream().findFirst();
    }

    /** Builds the filter clause with bind parameters; sort direction is whitelisted. */
    private static String filterSql(String type, LocalDateTime from, LocalDateTime to, String search) {
        StringBuilder sql = new StringBuilder(" FROM transactions t WHERE t.account_id = ?");
        if (type != null && !type.isBlank() && !"ALL".equalsIgnoreCase(type)) {
            sql.append(" AND t.transaction_type = ?");
        }
        if (from != null) {
            sql.append(" AND t.created_at >= ?");
        }
        if (to != null) {
            sql.append(" AND t.created_at <= ?");
        }
        if (search != null && !search.isBlank()) {
            sql.append(" AND (LOWER(t.reference_number) LIKE LOWER(?) "
                    + "OR LOWER(COALESCE(t.description, '')) LIKE LOWER(?) "
                    + "OR LOWER(t.transaction_type) LIKE LOWER(?))");
        }
        return sql.toString();
    }

    private static Object[] filterArgs(long accountId, String type, LocalDateTime from,
                                       LocalDateTime to, String search) {
        List<Object> args = new ArrayList<>();
        args.add(accountId);
        if (type != null && !type.isBlank() && !"ALL".equalsIgnoreCase(type)) {
            args.add(type.toUpperCase());
        }
        if (from != null) {
            args.add(java.sql.Timestamp.valueOf(from));
        }
        if (to != null) {
            args.add(java.sql.Timestamp.valueOf(to));
        }
        if (search != null && !search.isBlank()) {
            String like = "%" + search + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        return args.toArray();
    }

    public List<Transaction> findFiltered(long accountId, String type, LocalDateTime from,
                                          LocalDateTime to, String search, String sort,
                                          int limit, int offset) {
        String direction = "asc".equalsIgnoreCase(sort) ? "ASC" : "DESC";
        String sql = "SELECT " + T_COLUMNS
                + filterSql(type, from, to, search)
                + " ORDER BY t.created_at " + direction + ", t.transaction_id " + direction
                + " LIMIT ? OFFSET ?";
        Object[] args = filterArgs(accountId, type, from, to, search);
        Object[] allArgs = java.util.Arrays.copyOf(args, args.length + 2);
        allArgs[args.length] = limit;
        allArgs[args.length + 1] = offset;
        return jdbc.query(sql, MAPPER, allArgs);
    }

    public long countFiltered(long accountId, String type, LocalDateTime from,
                              LocalDateTime to, String search) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*)" + filterSql(type, from, to, search),
                Long.class, filterArgs(accountId, type, from, to, search));
        return count == null ? 0L : count;
    }

    public List<Transaction> findRecent(long accountId, int limit) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM transactions WHERE account_id = ? "
                        + "ORDER BY created_at DESC, transaction_id DESC LIMIT ?",
                MAPPER, accountId, limit);
    }

    public BigDecimal sumByType(long accountId, String type) {
        BigDecimal sum = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE account_id = ? "
                        + "AND transaction_type = ? AND transaction_status = 'SUCCESS'",
                BigDecimal.class, accountId, type);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    public long countByAccountId(long accountId) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE account_id = ?", Long.class, accountId);
        return count == null ? 0L : count;
    }

    public LocalDateTime lastUpdated(long accountId) {
        LocalDateTime value = jdbc.queryForObject(
                "SELECT MAX(created_at) FROM transactions WHERE account_id = ?",
                LocalDateTime.class, accountId);
        return value == null ? LocalDateTime.now() : value;
    }

    /** Last 12 months, used by the dashboard chart. */
    public List<MonthlyTotals> monthlyTotals(long accountId) {
        return jdbc.query(
                "SELECT DATE_FORMAT(created_at, '%Y-%m') AS ym, "
                        + "SUM(CASE WHEN transaction_type IN ('DEPOSIT','TRANSFER_IN') "
                        + "THEN amount ELSE 0 END) AS deposits, "
                        + "SUM(CASE WHEN transaction_type IN ('WITHDRAWAL','TRANSFER_OUT') "
                        + "THEN amount ELSE 0 END) AS withdrawals "
                        + "FROM transactions WHERE account_id = ? "
                        + "AND created_at >= DATE_SUB(CURDATE(), INTERVAL 11 MONTH) "
                        + "GROUP BY ym ORDER BY ym",
                (rs, rowNum) -> new MonthlyTotals(
                        rs.getString("ym"),
                        rs.getBigDecimal("deposits"),
                        rs.getBigDecimal("withdrawals")), accountId);
    }

    public long insertTransfer(String reference, long senderAccountId, long receiverAccountId,
                               BigDecimal amount, String remarks) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO transfers (reference_number, sender_account_id, receiver_account_id, "
                            + "amount, remarks, transfer_status) VALUES (?, ?, ?, ?, ?, 'SUCCESS')",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, reference);
            ps.setLong(2, senderAccountId);
            ps.setLong(3, receiverAccountId);
            ps.setBigDecimal(4, amount);
            ps.setString(5, remarks);
            return ps;
        }, keyHolder);
        return GeneratedKeys.require(keyHolder, "transfer_id",
                "Could not read generated transfer id");
    }

    public List<TransferRow> findTransfers(long accountId) {
        return jdbc.query(
                "SELECT t.reference_number, t.amount, t.remarks, t.created_at, "
                        + "CASE WHEN t.sender_account_id = ? THEN 'DEBIT' ELSE 'CREDIT' END AS direction, "
                        + "CASE WHEN t.sender_account_id = ? THEN ra.account_number ELSE sa.account_number END "
                        + "AS counterparty "
                        + "FROM transfers t "
                        + "JOIN accounts sa ON sa.account_id = t.sender_account_id "
                        + "JOIN accounts ra ON ra.account_id = t.receiver_account_id "
                        + "WHERE t.sender_account_id = ? OR t.receiver_account_id = ? "
                        + "ORDER BY t.created_at DESC LIMIT 100",
                (rs, rowNum) -> new TransferRow(
                        rs.getString("reference_number"),
                        rs.getString("counterparty"),
                        rs.getBigDecimal("amount"),
                        rs.getString("remarks"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getString("direction")),
                accountId, accountId, accountId, accountId);
    }
}
