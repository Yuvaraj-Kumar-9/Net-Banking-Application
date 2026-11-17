package com.hfdcbank.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** SQL for short-lived password-reset tokens. Only token HASHES are stored. */
@Repository
public class PasswordResetRepository {

    private final JdbcTemplate jdbc;

    public PasswordResetRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(String customerId, String tokenHash, LocalDateTime expiresAt) {
        jdbc.update(
                "INSERT INTO password_resets (customer_id, token_hash, expires_at) VALUES (?, ?, ?)",
                customerId, tokenHash, java.sql.Timestamp.valueOf(expiresAt));
    }

    public Optional<String> findActiveCustomer(String tokenHash) {
        List<String> rows = jdbc.queryForList(
                "SELECT customer_id FROM password_resets WHERE token_hash = ? "
                        + "AND used = FALSE AND expires_at > NOW() LIMIT 1",
                String.class, tokenHash);
        return rows.stream().findFirst();
    }

    public int markUsed(String tokenHash) {
        return jdbc.update(
                "UPDATE password_resets SET used = TRUE WHERE token_hash = ?", tokenHash);
    }
}
