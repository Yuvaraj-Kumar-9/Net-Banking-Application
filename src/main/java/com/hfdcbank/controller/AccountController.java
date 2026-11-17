package com.hfdcbank.controller;

import com.hfdcbank.dto.AccountResponse;
import com.hfdcbank.dto.BalanceResponse;
import com.hfdcbank.dto.DashboardResponse;
import com.hfdcbank.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Protected account read endpoints. */
@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts", description = "Account details, balance and dashboard aggregates")
@SecurityRequirement(name = "cookieAuth")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the logged-in customer's account details")
    public AccountResponse me() {
        return accountService.getMyAccount();
    }

    @GetMapping("/balance")
    @Operation(summary = "Get available balance and last ledger update time")
    public BalanceResponse balance() {
        return accountService.getBalance();
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard figures, recent activity and monthly chart data")
    public DashboardResponse dashboard() {
        return accountService.dashboard();
    }
}
