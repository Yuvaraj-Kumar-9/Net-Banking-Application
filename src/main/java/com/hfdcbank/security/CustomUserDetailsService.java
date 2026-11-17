package com.hfdcbank.security;

import com.hfdcbank.entity.Customer;
import com.hfdcbank.repository.CustomerRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/** Loads the authentication principal from the customers table via JDBC. */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;

    public CustomUserDetailsService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No customer with email " + email));
        return User.builder()
                .username(customer.email())
                .password(customer.passwordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .accountLocked(false)
                .disabled(false)
                .build();
    }
}
