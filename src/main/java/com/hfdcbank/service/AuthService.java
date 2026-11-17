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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/** Registration, login, logout and the password-reset flow. */
@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final PasswordResetRepository resetRepository;
    private final CustomerIdGenerator customerIdGenerator;
    private final AccountNumberGenerator accountNumberGenerator;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CookieUtil cookieUtil;
    private final boolean devMode;

    public AuthService(CustomerRepository customerRepository,
                       AccountRepository accountRepository,
                       PasswordResetRepository resetRepository,
                       CustomerIdGenerator customerIdGenerator,
                       AccountNumberGenerator accountNumberGenerator,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       CookieUtil cookieUtil,
                       @Value("${hfdc.dev-mode:true}") boolean devMode) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.resetRepository = resetRepository;
        this.customerIdGenerator = customerIdGenerator;
        this.accountNumberGenerator = accountNumberGenerator;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.cookieUtil = cookieUtil;
        this.devMode = devMode;
    }

    /**
     * Customer + account are written in ONE transaction: a failure after the
     * customer insert rolls the customer back, so half-registered users cannot
     * exist. Passwords and PINs are BCrypt-hashed before they touch MySQL.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessException("Password and confirm password do not match");
        }

        String email = request.email().trim().toLowerCase();
        String mobile = request.mobileNumber().trim();
        if (customerRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        if (customerRepository.existsByMobileNumber(mobile)) {
            throw new DuplicateResourceException("An account with this mobile number already exists");
        }

        String customerId = customerIdGenerator.next();
        String accountNumber = accountNumberGenerator.next();
        String passwordHash = passwordEncoder.encode(request.password());
        String pinHash = passwordEncoder.encode(request.transactionPin());

        customerRepository.insert(new Customer(
                customerId, request.fullName().trim(), email, mobile,
                request.dateOfBirth(), request.address().trim(), passwordHash, null, null));

        accountRepository.insert(new Account(
                0L, customerId, accountNumber, "SAVINGS", BigDecimal.ZERO.setScale(2),
                pinHash, "ACTIVE", null));

        return new AuthResponse(email, request.fullName().trim(), customerId, accountNumber,
                "Registration successful. Welcome to HFDC Bank.");
    }

    /** Authenticates against the BCrypt hash and issues the HttpOnly JWT cookie. */
    public AuthResponse login(LoginRequest request,
                              jakarta.servlet.http.HttpServletResponse response) {
        String email = request.email().trim().toLowerCase();
        Customer customer = customerRepository.findByEmail(email).orElse(null);

        // Same message for unknown email and wrong password (no enumeration).
        if (customer == null || !passwordEncoder.matches(request.password(), customer.passwordHash())) {
            throw new BusinessException("Invalid email or password");
        }

        Account account = accountRepository.findByCustomerId(customer.customerId())
                .orElseThrow(() -> new BusinessException("No active account is linked to this profile"));
        if (!account.isActive()) {
            throw new BusinessException("This account is not active. Please contact support.");
        }

        cookieUtil.writeAuthCookie(response, jwtService.generateToken(email));
        cookieUtil.writeCsrfCookie(response, newCsrfToken());

        return new AuthResponse(email, customer.fullName(), customer.customerId(),
                account.accountNumber(), "Login successful");
    }

    /** Called by the frontend on first load so it can send the CSRF header. */
    public String issueCsrfToken(jakarta.servlet.http.HttpServletResponse response) {
        String token = newCsrfToken();
        cookieUtil.writeCsrfCookie(response, token);
        return token;
    }

    public void logout(jakarta.servlet.http.HttpServletResponse response) {
        cookieUtil.clearAll(response);
    }

    public ForgotPasswordResponse forgotPassword(String email) {
        Customer customer = customerRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
        if (customer != null) {
            String rawToken = UUID.randomUUID().toString().replace("-", "")
                    + UUID.randomUUID().toString().replace("-", "");
            resetRepository.insert(customer.customerId(), sha256(rawToken),
                    LocalDateTime.now().plusMinutes(30));
            // In a real bank this token would be emailed. Dev mode shows it on screen.
            if (devMode) {
                return new ForgotPasswordResponse(
                        "A password reset token was generated (demo mode).", rawToken);
            }
        }
        return new ForgotPasswordResponse(
                "If this email is registered, a password reset link has been sent.", null);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException("New password and confirm password do not match");
        }
        String hash = sha256(request.token().trim());
        String customerId = resetRepository.findActiveCustomer(hash)
                .orElseThrow(() -> new BusinessException("Reset token is invalid or has expired"));
        customerRepository.updatePasswordHash(customerId, passwordEncoder.encode(request.newPassword()));
        resetRepository.markUsed(hash);
    }

    private String newCsrfToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
