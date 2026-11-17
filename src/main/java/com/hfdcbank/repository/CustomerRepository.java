package com.hfdcbank.repository;

import com.hfdcbank.entity.Customer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * All SQL for the customers table lives here. JdbcTemplate uses bind
 * parameters (?, ?, ?) so user input can never become executable SQL.
 */
@Repository
public class CustomerRepository {

    private static final String COLUMNS =
            "customer_id, full_name, email, mobile_number, date_of_birth, address, "
                    + "password_hash, created_at, updated_at";

    private static final RowMapper<Customer> MAPPER = (rs, rowNum) -> new Customer(
            rs.getString("customer_id"),
            rs.getString("full_name"),
            rs.getString("email"),
            rs.getString("mobile_number"),
            rs.getObject("date_of_birth", java.time.LocalDate.class),
            rs.getString("address"),
            rs.getString("password_hash"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
    );

    private final JdbcTemplate jdbc;

    public CustomerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Customer> findByEmail(String email) {
        List<Customer> rows = jdbc.query(
                "SELECT " + COLUMNS + " FROM customers WHERE email = ?", MAPPER, email);
        return rows.stream().findFirst();
    }

    public Optional<Customer> findByCustomerId(String customerId) {
        List<Customer> rows = jdbc.query(
                "SELECT " + COLUMNS + " FROM customers WHERE customer_id = ?", MAPPER, customerId);
        return rows.stream().findFirst();
    }

    public Optional<Customer> findByMobileNumber(String mobileNumber) {
        List<Customer> rows = jdbc.query(
                "SELECT " + COLUMNS + " FROM customers WHERE mobile_number = ?", MAPPER, mobileNumber);
        return rows.stream().findFirst();
    }

    public boolean existsByEmail(String email) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM customers WHERE email = ?", Integer.class, email);
        return count != null && count > 0;
    }

    public boolean existsByMobileNumber(String mobileNumber) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM customers WHERE mobile_number = ?", Integer.class, mobileNumber);
        return count != null && count > 0;
    }

    public boolean existsByCustomerId(String customerId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM customers WHERE customer_id = ?", Integer.class, customerId);
        return count != null && count > 0;
    }

    public int insert(Customer customer) {
        return jdbc.update(
                "INSERT INTO customers (customer_id, full_name, email, mobile_number, "
                        + "date_of_birth, address, password_hash) VALUES (?, ?, ?, ?, ?, ?, ?)",
                customer.customerId(), customer.fullName(), customer.email(), customer.mobileNumber(),
                customer.dateOfBirth(), customer.address(), customer.passwordHash());
    }

    public int updateProfile(String customerId, String fullName, String mobileNumber, String address) {
        return jdbc.update(
                "UPDATE customers SET full_name = ?, mobile_number = ?, address = ? WHERE customer_id = ?",
                fullName, mobileNumber, address, customerId);
    }

    public int updatePasswordHash(String customerId, String passwordHash) {
        return jdbc.update(
                "UPDATE customers SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE customer_id = ?",
                passwordHash, customerId);
    }
}
