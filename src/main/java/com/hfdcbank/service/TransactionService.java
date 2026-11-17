package com.hfdcbank.service;

import com.hfdcbank.dto.DepositRequest;
import com.hfdcbank.dto.PageResponse;
import com.hfdcbank.dto.TransactionResponse;
import com.hfdcbank.dto.TransactionResult;
import com.hfdcbank.dto.WithdrawalRequest;
import com.hfdcbank.entity.Account;
import com.hfdcbank.entity.Transaction;
import com.hfdcbank.exception.BusinessException;
import com.hfdcbank.exception.InsufficientBalanceException;
import com.hfdcbank.exception.InvalidPinException;
import com.hfdcbank.exception.ResourceNotFoundException;
import com.hfdcbank.repository.AccountRepository;
import com.hfdcbank.repository.TransactionRepository;
import com.hfdcbank.util.MoneyUtil;
import com.hfdcbank.util.TransactionReferenceGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Deposit, withdrawal and read-only ledger queries. */
@Service
public class TransactionService {

    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionReferenceGenerator referenceGenerator;
    private final PasswordEncoder passwordEncoder;

    public TransactionService(AccountService accountService,
                              AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              TransactionReferenceGenerator referenceGenerator,
                              PasswordEncoder passwordEncoder) {
        this.accountService = accountService;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.referenceGenerator = referenceGenerator;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Simulated deposit. The balance UPDATE and the ledger INSERT share one
     * transaction, so a failed INSERT rolls the balance change back.
     */
    @Transactional
    public TransactionResult deposit(DepositRequest request) {
        BigDecimal amount = MoneyUtil.money(request.amount());
        if (!MoneyUtil.isPositive(amount)) {
            throw new BusinessException("Deposit amount must be greater than zero");
        }
        Account account = accountService.currentAccount();
        if (!account.isActive()) {
            throw new BusinessException("Account is not active");
        }

        String reference = referenceGenerator.next();
        int updated = accountRepository.credit(account.accountId(), amount);
        if (updated != 1) {
            throw new BusinessException("Deposit could not be completed");
        }
        long id = transactionRepository.insert(account.accountId(), reference, "DEPOSIT", amount,
                defaultDescription(request.description(), "Simulated deposit"), "SUCCESS");
        BigDecimal newBalance = account.balance().add(amount);
        return new TransactionResult(reference, id, newBalance,
                "Deposit successful. This is a simulated educational transaction.");
    }

    /** Withdrawal requires the BCrypt-hashed transaction PIN. */
    @Transactional
    public TransactionResult withdraw(WithdrawalRequest request) {
        BigDecimal amount = MoneyUtil.money(request.amount());
        if (!MoneyUtil.isPositive(amount)) {
            throw new BusinessException("Withdrawal amount must be greater than zero");
        }
        Account account = accountService.currentAccount();
        if (!account.isActive()) {
            throw new BusinessException("Account is not active");
        }
        if (!passwordEncoder.matches(request.transactionPin(), account.transactionPinHash())) {
            throw new InvalidPinException();
        }

        // Atomic guard in SQL: balance >= amount must hold at update time.
        int updated = accountRepository.debit(account.accountId(), amount);
        if (updated != 1) {
            throw new InsufficientBalanceException("Insufficient balance for this withdrawal");
        }

        String reference = referenceGenerator.next();
        long id = transactionRepository.insert(account.accountId(), reference, "WITHDRAWAL", amount,
                defaultDescription(request.description(), "Cash withdrawal"), "SUCCESS");
        return new TransactionResult(reference, id, account.balance().subtract(amount),
                "Withdrawal successful");
    }

    public PageResponse<TransactionResponse> list(String type, String from, String to,
                                                  String search, String sort, int page, int size) {
        Account account = accountService.currentAccount();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        LocalDateTime fromDate = parseDate(from, false);
        LocalDateTime toDate = parseDate(to, true);

        long total = transactionRepository.countFiltered(account.accountId(), type, fromDate, toDate, search);
        List<TransactionResponse> content = transactionRepository
                .findFiltered(account.accountId(), type, fromDate, toDate, search, sort,
                        safeSize, safePage * safeSize)
                .stream()
                .map(this::toResponse)
                .toList();

        int totalPages = (int) Math.ceil((double) total / safeSize);
        return new PageResponse<>(content, safePage, safeSize, total, totalPages,
                safePage == 0, safePage >= totalPages - 1);
    }

    /** Reference lookup is scoped to the caller's own account (no IDOR). */
    public TransactionResponse detail(String referenceNumber) {
        Account account = accountService.currentAccount();
        Transaction tx = transactionRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        if (tx.accountId() != account.accountId()) {
            throw new ResourceNotFoundException("Transaction not found");
        }
        return toResponse(tx);
    }

    public List<TransactionResponse> transferHistory() {
        Account account = accountService.currentAccount();
        return transactionRepository.findTransfers(account.accountId()).stream()
                .map(row -> new TransactionResponse(0L, row.referenceNumber(),
                        "TRANSFER_" + row.direction(), row.amount(), row.remarks(),
                        "SUCCESS", row.createdAt(), row.direction(),
                        row.counterpartyAccountNumber()))
                .toList();
    }

    private TransactionResponse toResponse(Transaction tx) {
        return new TransactionResponse(tx.transactionId(), tx.referenceNumber(), tx.transactionType(),
                tx.amount(), tx.description(), tx.transactionStatus(), tx.createdAt(),
                tx.direction(), null);
    }

    private static String defaultDescription(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /** Accepts yyyy-MM-dd or a full ISO local date-time. */
    private static LocalDateTime parseDate(String value, boolean endOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim()).atTime(endOfDay ? 23 : 0, endOfDay ? 59 : 0,
                    endOfDay ? 59 : 0, endOfDay ? 999_999_999 : 0);
        } catch (RuntimeException ex) {
            return LocalDateTime.parse(value.trim());
        }
    }
}
