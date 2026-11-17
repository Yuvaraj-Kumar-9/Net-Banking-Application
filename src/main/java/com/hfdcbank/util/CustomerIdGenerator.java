package com.hfdcbank.util;

import com.hfdcbank.repository.CustomerRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** Generates a unique customer id: CID + 10 digits. */
@Component
public class CustomerIdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final CustomerRepository customerRepository;

    public CustomerIdGenerator(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public String next() {
        for (int attempt = 0; attempt < 20; attempt++) {
            StringBuilder sb = new StringBuilder("CID");
            for (int i = 0; i < 10; i++) {
                sb.append(RANDOM.nextInt(10));
            }
            String candidate = sb.toString();
            if (!customerRepository.existsByCustomerId(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate a unique customer id");
    }
}
