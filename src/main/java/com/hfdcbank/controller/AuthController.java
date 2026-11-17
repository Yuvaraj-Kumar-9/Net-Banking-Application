package com.hfdcbank.controller;

import com.hfdcbank.dto.AuthResponse;
import com.hfdcbank.dto.ForgotPasswordRequest;
import com.hfdcbank.dto.ForgotPasswordResponse;
import com.hfdcbank.dto.LoginRequest;
import com.hfdcbank.dto.MessageResponse;
import com.hfdcbank.dto.RegisterRequest;
import com.hfdcbank.dto.ResetPasswordRequest;
import com.hfdcbank.entity.Account;
import com.hfdcbank.entity.Customer;
import com.hfdcbank.service.AccountService;
import com.hfdcbank.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Public authentication endpoints. Controllers contain no business logic. */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Register, login, logout and password reset")
public class AuthController {

    private final AuthService authService;
    private final AccountService accountService;

    public AuthController(AuthService authService, AccountService accountService) {
        this.authService = authService;
        this.accountService = accountService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a customer and create their default savings account")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with email and password; sets an HttpOnly JWT cookie")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return authService.login(request, response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Clear the authentication and CSRF cookies")
    public MessageResponse logout(HttpServletResponse response) {
        authService.logout(response);
        return new MessageResponse("You have been logged out");
    }

    @GetMapping("/csrf")
    @Operation(summary = "Issue a double-submit CSRF cookie for the browser client")
    public Map<String, String> csrf(HttpServletResponse response) {
        return Map.of("csrfToken", authService.issueCsrfToken(response));
    }

    @GetMapping("/me")
    @Operation(summary = "Return the currently authenticated customer summary")
    public AuthResponse me() {
        Customer customer = accountService.currentCustomer();
        Account account = accountService.currentAccount();
        return new AuthResponse(customer.email(), customer.fullName(), customer.customerId(),
                account.accountNumber(), "authenticated");
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Generate a 30-minute reset token (shown on screen in dev mode)")
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request.email());
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Set a new password using a valid reset token")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return new MessageResponse("Password reset successfully. Please log in.");
    }

    @GetMapping("/health")
    @Operation(summary = "Liveness probe for deployment monitoring")
    public Map<String, String> health() {
        return Map.of("status", "UP", "bank", "HFDC Bank (educational simulation)");
    }
}
