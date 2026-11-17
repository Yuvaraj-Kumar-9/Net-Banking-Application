package com.hfdcbank.repository;

import com.hfdcbank.entity.Beneficiary;
import com.hfdcbank.util.GeneratedKeys;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/** SQL for beneficiary management. Every query is scoped by customer_id. */
@Repository
public class BeneficiaryRepository {

    private static final String COLUMNS =
            "beneficiary_id, customer_id, beneficiary_name, beneficiary_account_number, created_at";

    private static final RowMapper<Beneficiary> MAPPER = (rs, rowNum) -> new Beneficiary(
            rs.getLong("beneficiary_id"),
            rs.getString("customer_id"),
            rs.getString("beneficiary_name"),
            rs.getString("beneficiary_account_number"),
            rs.getTimestamp("created_at").toLocalDateTime()
    );

    private final JdbcTemplate jdbc;

    public BeneficiaryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Beneficiary> findByCustomerId(String customerId, String search) {
        if (search == null || search.isBlank()) {
            return jdbc.query(
                    "SELECT " + COLUMNS + " FROM beneficiaries WHERE customer_id = ? "
                            + "ORDER BY created_at DESC", MAPPER, customerId);
        }
        String like = "%" + search + "%";
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM beneficiaries WHERE customer_id = ? "
                        + "AND (LOWER(beneficiary_name) LIKE LOWER(?) "
                        + "OR beneficiary_account_number LIKE ?) ORDER BY created_at DESC",
                MAPPER, customerId, like, like);
    }

    public Optional<Beneficiary> findById(long beneficiaryId) {
        List<Beneficiary> rows = jdbc.query(
                "SELECT " + COLUMNS + " FROM beneficiaries WHERE beneficiary_id = ?",
                MAPPER, beneficiaryId);
        return rows.stream().findFirst();
    }

    public boolean existsForCustomer(String customerId, String accountNumber) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM beneficiaries WHERE customer_id = ? "
                        + "AND beneficiary_account_number = ?", Integer.class, customerId, accountNumber);
        return count != null && count > 0;
    }

    public long insert(String customerId, String beneficiaryName, String accountNumber) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO beneficiaries (customer_id, beneficiary_name, beneficiary_account_number) "
                            + "VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, customerId);
            ps.setString(2, beneficiaryName);
            ps.setString(3, accountNumber);
            return ps;
        }, keyHolder);
        return GeneratedKeys.require(keyHolder, "beneficiary_id",
                "Could not read generated beneficiary id");
    }

    /** Deletes only when the row belongs to the given customer (no IDOR). */
    public int deleteOwned(long beneficiaryId, String customerId) {
        return jdbc.update(
                "DELETE FROM beneficiaries WHERE beneficiary_id = ? AND customer_id = ?",
                beneficiaryId, customerId);
    }
}
