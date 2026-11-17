package com.hfdcbank.service;

import com.hfdcbank.dto.ChangePasswordRequest;
import com.hfdcbank.dto.ProfileResponse;
import com.hfdcbank.dto.ProfileUpdateRequest;
import com.hfdcbank.entity.Account;
import com.hfdcbank.entity.Customer;
import com.hfdcbank.exception.BusinessException;
import com.hfdcbank.exception.DuplicateResourceException;
import com.hfdcbank.repository.AccountRepository;
import com.hfdcbank.repository.CustomerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Self-service profile editing and secure password change. */
@Service
public class ProfileService {

    private final AccountService accountService;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(AccountService accountService,
                          CustomerRepository customerRepository,
                          AccountRepository accountRepository,
                          PasswordEncoder passwordEncoder) {
        this.accountService = accountService;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public ProfileResponse getProfile() {
        Customer customer = accountService.currentCustomer();
        Account account = accountRepository.findByCustomerId(customer.customerId())
                .orElseThrow(() -> new BusinessException("Account not found"));
        return new ProfileResponse(customer.customerId(), customer.fullName(), customer.email(),
                customer.mobileNumber(), customer.dateOfBirth(), customer.address(),
                customer.createdAt(), account.accountNumber());
    }

    @Transactional
    public ProfileResponse update(ProfileUpdateRequest request) {
        Customer customer = accountService.currentCustomer();
        String mobile = request.mobileNumber().trim();

        // A different customer must not be able to take the same mobile number.
        Customer owner = customerRepository.findByMobileNumber(mobile).orElse(null);
        if (owner != null && !owner.customerId().equals(customer.customerId())) {
            throw new DuplicateResourceException("This mobile number is already registered");
        }

        customerRepository.updateProfile(customer.customerId(), request.fullName().trim(),
                mobile, request.address().trim());
        return getProfile();
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException("New password and confirm password do not match");
        }
        Customer customer = accountService.currentCustomer();
        if (!passwordEncoder.matches(request.currentPassword(), customer.passwordHash())) {
            throw new BusinessException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), customer.passwordHash())) {
            throw new BusinessException("New password must be different from the current password");
        }
        customerRepository.updatePasswordHash(customer.customerId(),
                passwordEncoder.encode(request.newPassword()));
    }
}
