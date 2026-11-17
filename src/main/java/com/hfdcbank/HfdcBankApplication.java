package com.hfdcbank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Entry point of the HFDC Bank backend.
 *
 * <p>EnableTransactionManagement makes @Transactional / TransactionTemplate
 * work. Money movement (deposit, withdrawal, transfer) is always wrapped in a
 * database transaction so partial updates are impossible.</p>
 */
@SpringBootApplication
@EnableTransactionManagement
public class HfdcBankApplication {

    public static void main(String[] args) {
        SpringApplication.run(HfdcBankApplication.class, args);
    }
}
