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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Profile editing and the change-password security rules. */
@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private AccountService accountService;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private ProfileService service;

    @BeforeEach
    void setUp() {
        service = new ProfileService(accountService, customerRepository, accountRepository,
                passwordEncoder);
    }

    @Test
    void getProfileCombinesCustomerAndAccountDataWithoutHashes() {
        when(accountService.currentCustomer()).thenReturn(customer("CID1000000001", "9876543210"));
        when(accountRepository.findByCustomerId("CID1000000001")).thenReturn(Optional.of(account()));

        ProfileResponse response = service.getProfile();

        assertThat(response.customerId()).isEqualTo("CID1000000001");
        assertThat(response.fullName()).isEqualTo("Aarav Sharma");
        assertThat(response.email()).isEqualTo("aarav@hfdcbank.test");
        assertThat(response.accountNumber()).isEqualTo("HFDC1000000001");
    }

    @Test
    void updateRejectsAMobileNumberBelongingToAnotherCustomer() {
        when(accountService.currentCustomer()).thenReturn(customer("CID1000000001", "9876543210"));
        when(customerRepository.findByMobileNumber("9876543211"))
                .thenReturn(Optional.of(customer("CID1000000002", "9876543211")));

        assertThatThrownBy(() -> service.update(
                new ProfileUpdateRequest("Aarav Sharma", "9876543211", "12 Marine Drive")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already registered");

        verify(customerRepository, never()).updateProfile(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void updateAllowsTheCustomerToKeepTheirOwnMobileNumber() {
        when(accountService.currentCustomer()).thenReturn(
                customer("CID1000000001", "9876543210"),
                customer("CID1000000001", "Aarav S Sharma", "9876543210"));
        when(customerRepository.findByMobileNumber("9876543210"))
                .thenReturn(Optional.of(customer("CID1000000001", "9876543210")));
        when(customerRepository.updateProfile("CID1000000001", "Aarav S Sharma",
                "9876543210", "14 Marine Drive")).thenReturn(1);
        when(accountRepository.findByCustomerId("CID1000000001")).thenReturn(Optional.of(account()));

        ProfileResponse response = service.update(
                new ProfileUpdateRequest(" Aarav S Sharma ", " 9876543210 ", " 14 Marine Drive "));

        assertThat(response.fullName()).isEqualTo("Aarav S Sharma");
        verify(customerRepository).updateProfile("CID1000000001", "Aarav S Sharma",
                "9876543210", "14 Marine Drive");
    }

    @Test
    void updatePersistsANewMobileNumber() {
        when(accountService.currentCustomer()).thenReturn(customer("CID1000000001", "9876543210"));
        when(customerRepository.findByMobileNumber("9876500000")).thenReturn(Optional.empty());
        when(customerRepository.updateProfile("CID1000000001", "Aarav Sharma",
                "9876500000", "12 Marine Drive")).thenReturn(1);
        when(accountRepository.findByCustomerId("CID1000000001")).thenReturn(Optional.of(account()));

        service.update(new ProfileUpdateRequest("Aarav Sharma", "9876500000", "12 Marine Drive"));

        verify(customerRepository).updateProfile("CID1000000001", "Aarav Sharma",
                "9876500000", "12 Marine Drive");
    }

    @Test
    void changePasswordRejectsMismatchedConfirmationBeforeTouchingTheDatabase() {
        assertThatThrownBy(() -> service.changePassword(new ChangePasswordRequest(
                "Demo@1234", "New@12345", "Other@12345")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("do not match");

        verifyNoInteractions(accountService, customerRepository, passwordEncoder);
    }

    @Test
    void changePasswordRejectsAWrongCurrentPassword() {
        when(accountService.currentCustomer()).thenReturn(customer("CID1000000001", "9876543210"));
        when(passwordEncoder.matches("Wrong@1234", "password-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(new ChangePasswordRequest(
                "Wrong@1234", "New@12345", "New@12345")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Current password is incorrect");

        verify(customerRepository, never()).updatePasswordHash(anyString(), anyString());
    }

    @Test
    void changePasswordRejectsReusingTheCurrentPassword() {
        when(accountService.currentCustomer()).thenReturn(customer("CID1000000001", "9876543210"));
        // Both the "current" and "new" checks use the same value, so one stub covers both.
        when(passwordEncoder.matches("Demo@1234", "password-hash")).thenReturn(true);

        assertThatThrownBy(() -> service.changePassword(new ChangePasswordRequest(
                "Demo@1234", "Demo@1234", "Demo@1234")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("must be different");

        verify(customerRepository, never()).updatePasswordHash(anyString(), anyString());
    }

    @Test
    void changePasswordStoresANewBcryptHash() {
        when(accountService.currentCustomer()).thenReturn(customer("CID1000000001", "9876543210"));
        when(passwordEncoder.matches("Demo@1234", "password-hash")).thenReturn(true);
        when(passwordEncoder.matches("New@12345", "password-hash")).thenReturn(false);
        when(passwordEncoder.encode("New@12345")).thenReturn("new-bcrypt-hash");

        service.changePassword(new ChangePasswordRequest(
                "Demo@1234", "New@12345", "New@12345"));

        verify(customerRepository).updatePasswordHash("CID1000000001", "new-bcrypt-hash");
    }

    private static Customer customer(String customerId, String mobile) {
        return customer(customerId, "Aarav Sharma", mobile);
    }

    private static Customer customer(String customerId, String fullName, String mobile) {
        return new Customer(customerId, fullName, "aarav@hfdcbank.test", mobile,
                LocalDate.of(1992, 4, 18), "12 Marine Drive, Mumbai", "password-hash",
                LocalDateTime.now(), LocalDateTime.now());
    }

    private static Account account() {
        return new Account(1L, "CID1000000001", "HFDC1000000001", "SAVINGS",
                new BigDecimal("1000.00"), "pin-hash", "ACTIVE", LocalDateTime.now());
    }
}
