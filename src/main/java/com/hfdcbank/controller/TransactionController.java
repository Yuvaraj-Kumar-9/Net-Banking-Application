package com.hfdcbank.controller;

import com.hfdcbank.dto.DepositRequest;
import com.hfdcbank.dto.PageResponse;
import com.hfdcbank.dto.TransactionResponse;
import com.hfdcbank.dto.TransactionResult;
import com.hfdcbank.dto.WithdrawalRequest;
import com.hfdcbank.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Deposit, withdraw and transaction-history endpoints. */
@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "Deposits, withdrawals and the customer's ledger")
@SecurityRequirement(name = "cookieAuth")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/deposit")
    @Operation(summary = "Simulate a deposit into the logged-in account")
    public ResponseEntity<TransactionResult> deposit(@Valid @RequestBody DepositRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.deposit(request));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw funds after validating the transaction PIN")
    public ResponseEntity<TransactionResult> withdraw(@Valid @RequestBody WithdrawalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.withdraw(request));
    }

    @GetMapping
    @Operation(summary = "Search, filter, sort and paginate your own transactions")
    public PageResponse<TransactionResponse> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "desc") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return transactionService.list(type, from, to, search, sort, page, size);
    }

    @GetMapping("/{referenceNumber}")
    @Operation(summary = "Get one transaction by its reference number")
    public TransactionResponse detail(@PathVariable String referenceNumber) {
        return transactionService.detail(referenceNumber);
    }
}
