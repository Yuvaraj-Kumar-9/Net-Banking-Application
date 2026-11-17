package com.hfdcbank.service;

import com.hfdcbank.dto.TransferRequest;
import com.hfdcbank.exception.InsufficientBalanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The most important test in the project: 20 customers transfer money out of
 * the SAME account at the same instant, but only 1000.00 is available.
 *
 * <p>This runs the real JdbcTemplate repositories, the real row locks and the
 * real TransactionTemplate against H2 in MySQL mode. Exactly 10 transfers must
 * succeed, the balance must never go negative, and the ledger must contain one
 * debit and one credit for each successful transfer.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class TransferConcurrencyTest {

    private static final String SENDER_EMAIL = "concurrent.sender@hfdcbank.test";
    private static final String SENDER_ACCOUNT = "HFDC9000000001";
    private static final String RECEIVER_ACCOUNT = "HFDC9000000002";
    private static final int THREADS = 20;
    private static final BigDecimal STARTING_BALANCE = new BigDecimal("1000.00");
    private static final BigDecimal AMOUNT = new BigDecimal("100.00");

    @Autowired
    private TransferService transferService;

    @Autowired
    private JdbcTemplate jdbc;

    private long senderId;
    private long receiverId;

    @BeforeEach
    void resetData() {
        jdbc.update("DELETE FROM transfers");
        jdbc.update("DELETE FROM transactions");
        jdbc.update("DELETE FROM beneficiaries");
        jdbc.update("DELETE FROM password_resets");
        jdbc.update("DELETE FROM accounts");
        jdbc.update("DELETE FROM customers");

        jdbc.update("INSERT INTO customers (customer_id, full_name, email, mobile_number, "
                        + "date_of_birth, address, password_hash) VALUES (?, ?, ?, ?, ?, ?, ?)",
                "CID9000000001", "Concurrent Sender", SENDER_EMAIL, "9000000001",
                LocalDate.of(1990, 1, 1), "Test address", "unused-password-hash");
        jdbc.update("INSERT INTO customers (customer_id, full_name, email, mobile_number, "
                        + "date_of_birth, address, password_hash) VALUES (?, ?, ?, ?, ?, ?, ?)",
                "CID9000000002", "Concurrent Receiver", "concurrent.receiver@hfdcbank.test",
                "9000000002", LocalDate.of(1991, 2, 2), "Test address", "unused-password-hash");

        String pinHash = new BCryptPasswordEncoder(4).encode("1234");
        jdbc.update("INSERT INTO accounts (customer_id, account_number, account_type, balance, "
                        + "transaction_pin_hash, account_status) VALUES (?, ?, ?, ?, ?, ?)",
                "CID9000000001", SENDER_ACCOUNT, "SAVINGS", STARTING_BALANCE, pinHash, "ACTIVE");
        jdbc.update("INSERT INTO accounts (customer_id, account_number, account_type, balance, "
                        + "transaction_pin_hash, account_status) VALUES (?, ?, ?, ?, ?, ?)",
                "CID9000000002", RECEIVER_ACCOUNT, "SAVINGS", new BigDecimal("0.00"),
                pinHash, "ACTIVE");

        senderId = jdbc.queryForObject(
                "SELECT account_id FROM accounts WHERE account_number = ?", Long.class, SENDER_ACCOUNT);
        receiverId = jdbc.queryForObject(
                "SELECT account_id FROM accounts WHERE account_number = ?", Long.class, RECEIVER_ACCOUNT);
    }

    @Test
    void concurrentTransfersNeverOverspendAndNeverLoseMoney() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger insufficient = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < THREADS; i++) {
                futures.add(pool.submit(() -> {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(SENDER_EMAIL, null,
                                    List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
                    try {
                        ready.countDown();
                        try {
                            if (!start.await(10, TimeUnit.SECONDS)) {
                                throw new IllegalStateException("Start gate was not released");
                            }
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException("Interrupted before transfer", ex);
                        }
                        transferService.transfer(new TransferRequest(RECEIVER_ACCOUNT,
                                "Concurrent Receiver", AMOUNT, "concurrency test", "1234"));
                        successes.incrementAndGet();
                    } catch (InsufficientBalanceException ex) {
                        insufficient.incrementAndGet();
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                }));
            }

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            // Future.get() rethrows any unexpected exception and fails the test.
            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(successes.get()).isEqualTo(10);
        assertThat(insufficient.get()).isEqualTo(10);

        assertThat(balanceOf(senderId)).isEqualByComparingTo("0.00");
        assertThat(balanceOf(receiverId)).isEqualByComparingTo("1000.00");

        assertThat(count("SELECT COUNT(*) FROM transactions WHERE account_id = ? "
                + "AND transaction_type = 'TRANSFER_OUT'", senderId)).isEqualTo(10);
        assertThat(count("SELECT COUNT(*) FROM transactions WHERE account_id = ? "
                + "AND transaction_type = 'TRANSFER_IN'", receiverId)).isEqualTo(10);
        assertThat(count("SELECT COUNT(*) FROM transfers")).isEqualTo(10);

        // Every transaction row is positive and every balance is still valid.
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE amount <= 0", Long.class)).isZero();
    }

    private BigDecimal balanceOf(long accountId) {
        return jdbc.queryForObject(
                "SELECT balance FROM accounts WHERE account_id = ?", BigDecimal.class, accountId);
    }

    private long count(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }
}
