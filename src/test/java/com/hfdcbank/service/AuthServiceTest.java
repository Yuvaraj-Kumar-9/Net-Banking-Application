package com.hfdcbank.service;

import com.hfdcbank.dto.AuthResponse;
import com.hfdcbank.dto.ForgotPasswordResponse;
import com.hfdcbank.dto.LoginRequest;
import com.hfdcbank.dto.RegisterRequest;
import com.hfdcbank.dto.ResetPasswordRequest;
import com.hfdcbank.entity.Account;
import com.hfdcbank.entity.Customer;
import com.hfdcbank.exception.BusinessException;
import com.hfdcbank.exception.DuplicateResourceException;
import com.hfdcbank.repository.AccountRepository;
import com.hfdcbank.repository.CustomerRepository;
import com.hfdcbank.repository.PasswordResetRepository;
import com.hfdcbank.security.JwtService;
import com.hfdcbank.util.AccountNumberGenerator;
import com.hfdcbank.util.CookieUtil;
import com.hfdcbank.util.CustomerIdGenerator;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Registration, login, logout and the forgot/reset-password flow. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private PasswordResetRepository resetRepository;
    @Mock
    private CustomerIdGenerator customerIdGenerator;
    @Mock
    private AccountNumberGenerator accountNumberGenerator;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private CookieUtil cookieUtil;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(customerRepository, accountRepository, resetRepository,
                customerIdGenerator, accountNumberGenerator, passwordEncoder, jwtService,
                cookieUtil, true);
    }

    // ----------------------------------------------------------- register --

    @Test
    void registerHashesThePasswordAndPinAndCreatesAZeroBalanceAccount() {
        RegisterRequest request = registerRequest("Test@1234", "Test@1234");
        when(customerRepository.existsByEmail("aarav@test.com")).thenReturn(false);
        when(customerRepository.existsByMobileNumber("9876543210")).thenReturn(false);
        when(customerIdGenerator.next()).thenReturn("CID1000000009");
        when(accountNumberGenerator.next()).thenReturn("HFDC1000000009");
        when(passwordEncoder.encode("Test@1234")).thenReturn("bcrypt-password");
        when(passwordEncoder.encode("1234")).thenReturn("bcrypt-pin");
        when(customerRepository.insert(any(Customer.class))).thenReturn(1);
        when(accountRepository.insert(any(Account.class))).thenReturn(1L);

        AuthResponse response = service.register(request);

        assertThat(response.email()).isEqualTo("aarav@test.com");
        assertThat(response.fullName()).isEqualTo("Aarav Sharma");
        assertThat(response.customerId()).isEqualTo("CID1000000009");
        assertThat(response.accountNumber()).isEqualTo("HFDC1000000009");

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).insert(customerCaptor.capture());
        Customer saved = customerCaptor.getValue();
        assertThat(saved.fullName()).isEqualTo("Aarav Sharma");
        assertThat(saved.email()).isEqualTo("aarav@test.com");
        assertThat(saved.mobileNumber()).isEqualTo("9876543210");
        assertThat(saved.address()).isEqualTo("12 Marine Drive, Mumbai");
        assertThat(saved.passwordHash()).isEqualTo("bcrypt-password");

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).insert(accountCaptor.capture());
        Account account = accountCaptor.getValue();
        assertThat(account.customerId()).isEqualTo("CID1000000009");
        assertThat(account.accountNumber()).isEqualTo("HFDC1000000009");
        assertThat(account.accountType()).isEqualTo("SAVINGS");
        assertThat(account.accountStatus()).isEqualTo("ACTIVE");
        assertThat(account.balance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(account.balance().scale()).isEqualTo(2);
        assertThat(account.transactionPinHash()).isEqualTo("bcrypt-pin");
    }

    @Test
    void registerRejectsMismatchedPasswords() {
        assertThatThrownBy(() -> service.register(registerRequest("Test@1234", "Other@1234")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("do not match");

        verifyNoInteractions(customerRepository, accountRepository, passwordEncoder);
    }

    @Test
    void registerRejectsADuplicateEmail() {
        when(customerRepository.existsByEmail("aarav@test.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(registerRequest("Test@1234", "Test@1234")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("email already exists");

        verify(customerRepository, never()).insert(any(Customer.class));
        verifyNoInteractions(accountRepository);
    }

    @Test
    void registerRejectsADuplicateMobileNumber() {
        when(customerRepository.existsByEmail("aarav@test.com")).thenReturn(false);
        when(customerRepository.existsByMobileNumber("9876543210")).thenReturn(true);

        assertThatThrownBy(() -> service.register(registerRequest("Test@1234", "Test@1234")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("mobile number already exists");

        verify(customerRepository, never()).insert(any(Customer.class));
        verifyNoInteractions(accountRepository);
    }

    // -------------------------------------------------------------- login --

    @Test
    void loginIssuesTheHttpOnlyJwtCookieAndACsrfCookie() {
        HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);
        Customer customer = customer("password-hash");
        when(customerRepository.findByEmail("aarav@hfdcbank.test")).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("Demo@1234", "password-hash")).thenReturn(true);
        when(accountRepository.findByCustomerId("CID1000000001")).thenReturn(Optional.of(account("ACTIVE")));
        when(jwtService.generateToken("aarav@hfdcbank.test")).thenReturn("signed-jwt");

        AuthResponse result = service.login(
                new LoginRequest(" Aarav@HFDCBank.test ", "Demo@1234"), response);

        assertThat(result.fullName()).isEqualTo("Aarav Sharma");
        assertThat(result.accountNumber()).isEqualTo("HFDC1000000001");
        verify(cookieUtil).writeAuthCookie(response, "signed-jwt");
        verify(cookieUtil).writeCsrfCookie(eq(response), anyString());
    }

    @Test
    void loginGivesTheSameErrorForAnUnknownEmailAndAWrongPassword() {
        HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);
        when(customerRepository.findByEmail("nobody@hfdcbank.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(
                new LoginRequest("nobody@hfdcbank.test", "Whatever@1"), response))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Invalid email or password");

        verifyNoInteractions(jwtService, cookieUtil);
    }

    @Test
    void loginRejectsAWrongPasswordWithoutSettingCookies() {
        HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);
        when(customerRepository.findByEmail("aarav@hfdcbank.test"))
                .thenReturn(Optional.of(customer("password-hash")));
        when(passwordEncoder.matches("wrong", "password-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(
                new LoginRequest("aarav@hfdcbank.test", "wrong"), response))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Invalid email or password");

        verifyNoInteractions(jwtService, cookieUtil);
    }

    @Test
    void loginRejectsAnInactiveAccount() {
        HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);
        when(customerRepository.findByEmail("aarav@hfdcbank.test"))
                .thenReturn(Optional.of(customer("password-hash")));
        when(passwordEncoder.matches("Demo@1234", "password-hash")).thenReturn(true);
        when(accountRepository.findByCustomerId("CID1000000001"))
                .thenReturn(Optional.of(account("BLOCKED")));

        assertThatThrownBy(() -> service.login(
                new LoginRequest("aarav@hfdcbank.test", "Demo@1234"), response))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not active");

        verifyNoInteractions(jwtService, cookieUtil);
    }

    // ---------------------------------------------------- forgot / reset --

    @Test
    void forgotPasswordStoresOnlyAHashOfTheTokenInDevMode() {
        when(customerRepository.findByEmail("aarav@hfdcbank.test"))
                .thenReturn(Optional.of(customer("password-hash")));

        ForgotPasswordResponse response = service.forgotPassword(" Aarav@HFDCBank.test ");

        assertThat(response.resetToken()).isNotBlank().hasSize(64);
        assertThat(response.message()).contains("demo mode");

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(resetRepository).insert(eq("CID1000000001"), hashCaptor.capture(), any(LocalDateTime.class));
        assertThat(hashCaptor.getValue())
                .hasSize(64)
                .matches("[0-9a-f]{64}")
                .isNotEqualTo(response.resetToken());
    }

    @Test
    void forgotPasswordNeverRevealsWhetherAnEmailExists() {
        when(customerRepository.findByEmail("nobody@hfdcbank.test")).thenReturn(Optional.empty());

        ForgotPasswordResponse response = service.forgotPassword("nobody@hfdcbank.test");

        assertThat(response.resetToken()).isNull();
        assertThat(response.message()).contains("If this email is registered");
        verifyNoInteractions(resetRepository);
    }

    @Test
    void forgotPasswordHidesTheTokenWhenDevModeIsOff() {
        AuthService productionLike = new AuthService(customerRepository, accountRepository,
                resetRepository, customerIdGenerator, accountNumberGenerator, passwordEncoder,
                jwtService, cookieUtil, false);
        when(customerRepository.findByEmail("aarav@hfdcbank.test"))
                .thenReturn(Optional.of(customer("password-hash")));

        ForgotPasswordResponse response = productionLike.forgotPassword("aarav@hfdcbank.test");

        assertThat(response.resetToken()).isNull();
        verify(resetRepository).insert(eq("CID1000000001"), anyString(), any(LocalDateTime.class));
    }

    @Test
    void resetPasswordRejectsMismatchedConfirmation() {
        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest(
                "token", "New@12345", "Other@12345")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("do not match");

        verifyNoInteractions(resetRepository);
    }

    @Test
    void resetPasswordRejectsAnInvalidOrExpiredToken() {
        when(resetRepository.findActiveCustomer(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest(
                "bad-token", "New@12345", "New@12345")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("invalid or has expired");

        verify(customerRepository, never()).updatePasswordHash(anyString(), anyString());
    }

    @Test
    void resetPasswordUpdatesTheHashAndMarksTheTokenUsed() throws Exception {
        String expectedHash = sha256("raw-token");
        when(resetRepository.findActiveCustomer(expectedHash)).thenReturn(Optional.of("CID1000000001"));
        when(passwordEncoder.encode("New@12345")).thenReturn("new-bcrypt-hash");

        service.resetPassword(new ResetPasswordRequest(" raw-token ", "New@12345", "New@12345"));

        verify(customerRepository).updatePasswordHash("CID1000000001", "new-bcrypt-hash");
        verify(resetRepository).markUsed(expectedHash);
    }

    private static RegisterRequest registerRequest(String password, String confirmPassword) {
        return new RegisterRequest(" Aarav Sharma ", " Aarav@Test.com ", " 9876543210 ",
                LocalDate.of(1992, 4, 18), " 12 Marine Drive, Mumbai ", password,
                confirmPassword, "1234");
    }

    private static Customer customer(String passwordHash) {
        return new Customer("CID1000000001", "Aarav Sharma", "aarav@hfdcbank.test",
                "9876543210", LocalDate.of(1992, 4, 18), "12 Marine Drive, Mumbai",
                passwordHash, LocalDateTime.now(), LocalDateTime.now());
    }

    private static Account account(String status) {
        return new Account(1L, "CID1000000001", "HFDC1000000001", "SAVINGS",
                new BigDecimal("1000.00"), "pin-hash", status, LocalDateTime.now());
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
