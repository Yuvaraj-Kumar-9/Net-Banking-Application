package com.hfdcbank.service;

import com.hfdcbank.dto.AccountResponse;
import com.hfdcbank.dto.BalanceResponse;
import com.hfdcbank.dto.DashboardResponse;
import com.hfdcbank.dto.TransactionResponse;
import com.hfdcbank.entity.Account;
import com.hfdcbank.entity.Customer;
import com.hfdcbank.exception.ResourceNotFoundException;
import com.hfdcbank.repository.AccountRepository;
import com.hfdcbank.repository.CustomerRepository;
import com.hfdcbank.repository.TransactionRepository;
import com.hfdcbank.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/** Read models for My Account, Balance and the dashboard - all from MySQL. */
@Service
public class AccountService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(CustomerRepository customerRepository,
                          AccountRepository accountRepository,
                          TransactionRepository transactionRepository) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /** The logged-in customer, resolved from the JWT subject (email). */
    public Customer currentCustomer() {
        return customerRepository.findByEmail(SecurityUtils.currentEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
    }

    /** The logged-in customer's account. No id is accepted from the browser. */
    public Account currentAccount() {
        return accountRepository.findByCustomerId(currentCustomer().customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found"));
    }

    public AccountResponse getMyAccount() {
        Account account = currentAccount();
        return new AccountResponse(account.accountId(), account.customerId(), account.accountNumber(),
                account.accountType(), account.balance(), account.accountStatus(), account.createdAt());
    }

    public BalanceResponse getBalance() {
        Account account = currentAccount();
        return new BalanceResponse(account.accountNumber(), account.accountType(),
                account.balance(), transactionRepository.lastUpdated(account.accountId()));
    }

    public DashboardResponse dashboard() {
        Customer customer = currentCustomer();
        Account account = currentAccount();

        // Both credit-style types are counted as "deposits" in the summary.
        BigDecimal deposits = transactionRepository.sumByType(account.accountId(), "DEPOSIT")
                .add(transactionRepository.sumByType(account.accountId(), "TRANSFER_IN"));
        BigDecimal withdrawals = transactionRepository.sumByType(account.accountId(), "WITHDRAWAL")
                .add(transactionRepository.sumByType(account.accountId(), "TRANSFER_OUT"));

        List<TransactionResponse> recent = transactionRepository
                .findRecent(account.accountId(), 5).stream()
                .map(tx -> new TransactionResponse(tx.transactionId(), tx.referenceNumber(),
                        tx.transactionType(), tx.amount(), tx.description(), tx.transactionStatus(),
                        tx.createdAt(), tx.direction(), null))
                .toList();

        List<DashboardResponse.MonthlySummary> monthly = transactionRepository
                .monthlyTotals(account.accountId()).stream()
                .map(row -> new DashboardResponse.MonthlySummary(
                        row.month(), row.deposits(), row.withdrawals()))
                .toList();

        return new DashboardResponse(
                customer.fullName(), account.accountNumber(), account.accountType(), account.balance(),
                deposits, withdrawals, transactionRepository.countByAccountId(account.accountId()),
                recent, monthly);
    }
}
