package com.hfdcbank.service;

import com.hfdcbank.dto.TransferRequest;
import com.hfdcbank.dto.TransferResponse;
import com.hfdcbank.entity.Account;
import com.hfdcbank.entity.Customer;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

/**
 * Fund transfer is the most safety-critical method in the application.
 *
 * <p>Everything runs inside ONE database transaction via TransactionTemplate:
 * row locks -> debit -> credit -> two ledger rows -> transfers audit row.
 * If any statement fails, the template rolls back all of them, so money can
 * never be deducted without being credited.</p>
 *
 * <p>Rows are locked in ascending account_id order to prevent deadlocks when
 * two customers transfer to each other at the same instant.</p>
 */
@Service
public class TransferService {

    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionReferenceGenerator referenceGenerator;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;

    public TransferService(AccountService accountService,
                           AccountRepository accountRepository,
                           TransactionRepository transactionRepository,
                           TransactionReferenceGenerator referenceGenerator,
                           PasswordEncoder passwordEncoder,
                           PlatformTransactionManager transactionManager) {
        this.accountService = accountService;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.referenceGenerator = referenceGenerator;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public TransferResponse transfer(TransferRequest request) {
        BigDecimal amount = MoneyUtil.money(request.amount());
        if (!MoneyUtil.isPositive(amount)) {
            throw new BusinessException("Transfer amount must be greater than zero");
        }

        Account sender = accountService.currentAccount();
        Customer senderCustomer = accountService.currentCustomer();

        if (sender.accountNumber().equals(request.beneficiaryAccountNumber().trim())) {
            throw new BusinessException("You cannot transfer money to your own account");
        }
        if (!sender.isActive()) {
            throw new BusinessException("Your account is not active");
        }
        if (!passwordEncoder.matches(request.transactionPin(), sender.transactionPinHash())) {
            throw new InvalidPinException();
        }

        Account receiver = accountRepository.findByAccountNumber(request.beneficiaryAccountNumber().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary account not found"));
        if (!receiver.isActive()) {
            throw new BusinessException("The beneficiary account is not active");
        }

        // The lambda body is executed inside a single BEGIN ... COMMIT/ROLLBACK.
        return transactionTemplate.execute(status -> {
            long firstId = Math.min(sender.accountId(), receiver.accountId());
            long secondId = Math.max(sender.accountId(), receiver.accountId());

            // Pessimistic locks: concurrent transfers wait here, then see fresh balances.
            Account first = accountRepository.lockById(firstId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
            Account second = accountRepository.lockById(secondId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
            Account lockedSender = first.accountId() == sender.accountId() ? first : second;

            if (lockedSender.balance().compareTo(amount) < 0) {
                throw new InsufficientBalanceException("Insufficient balance for this transfer");
            }

            if (accountRepository.debit(sender.accountId(), amount) != 1) {
                throw new InsufficientBalanceException("Insufficient balance for this transfer");
            }
            if (accountRepository.credit(receiver.accountId(), amount) != 1) {
                throw new BusinessException("The beneficiary account could not be credited");
            }

            String transferReference = referenceGenerator.next();
            String senderReference = referenceGenerator.next();
            String receiverReference = referenceGenerator.next();

            // Sender's ledger row (debit).
            transactionRepository.insert(sender.accountId(), senderReference, "TRANSFER_OUT", amount,
                    "Transfer to " + receiver.accountNumber() + " (" + request.beneficiaryName() + ")"
                            + remarksSuffix(request.remarks()), "SUCCESS");

            // Receiver's ledger row (credit).
            transactionRepository.insert(receiver.accountId(), receiverReference, "TRANSFER_IN", amount,
                    "Transfer from " + sender.accountNumber() + " (" + senderCustomer.fullName() + ")",
                    "SUCCESS");

            // Audit row tying the two accounts to one reference.
            transactionRepository.insertTransfer(transferReference, sender.accountId(),
                    receiver.accountId(), amount, request.remarks());

            BigDecimal newBalance = lockedSender.balance().subtract(amount);
            return new TransferResponse(transferReference, amount, receiver.accountNumber(),
                    request.beneficiaryName(), newBalance, "Transfer completed successfully");
        });
    }

    private static String remarksSuffix(String remarks) {
        return remarks == null || remarks.isBlank() ? "" : " - " + remarks.trim();
    }
}
