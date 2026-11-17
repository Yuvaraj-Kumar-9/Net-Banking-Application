package com.hfdcbank.controller;

import com.hfdcbank.dto.TransactionResponse;
import com.hfdcbank.dto.TransferRequest;
import com.hfdcbank.dto.TransferResponse;
import com.hfdcbank.service.TransactionService;
import com.hfdcbank.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Fund transfer endpoints. */
@RestController
@RequestMapping("/api/transfers")
@Tag(name = "Transfers", description = "Atomic peer-to-peer transfers")
@SecurityRequirement(name = "cookieAuth")
public class TransferController {

    private final TransferService transferService;
    private final TransactionService transactionService;

    public TransferController(TransferService transferService, TransactionService transactionService) {
        this.transferService = transferService;
        this.transactionService = transactionService;
    }

    @PostMapping
    @Operation(summary = "Transfer money atomically to another HFDC Bank account")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transferService.transfer(request));
    }

    @GetMapping("/history")
    @Operation(summary = "List transfers involving the logged-in account")
    public List<TransactionResponse> history() {
        return transactionService.transferHistory();
    }
}
