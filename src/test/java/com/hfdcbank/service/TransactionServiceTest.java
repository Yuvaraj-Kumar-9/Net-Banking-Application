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
import com.hfdcbank.util.TransactionReferenceGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Deposit, withdrawal, ledger queries and pagination rules. */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountService accountService;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionReferenceGenerator referenceGenerator;
    @Mock
    private PasswordEncoder passwordEncoder;

    private TransactionService service;

    @BeforeEach
    void setUp() {
        service = new TransactionService(accountService, accountRepository, transactionRepository,
                referenceGenerator, passwordEncoder);
    }

    // ------------------------------------------------------------- deposits --

    @Test
    void depositCreditsTheBalanceAndWritesOneLedgerRow() {
        Account account = account(1L, "1000.00", "ACTIVE");
        when(accountService.currentAccount()).thenReturn(account);
        when(referenceGenerator.next()).thenReturn("TXN-DEPOSIT");
        when(accountRepository.credit(eq(1L), any(BigDecimal.class))).thenReturn(1);
        when(transactionRepository.insert(eq(1L), eq("TXN-DEPOSIT"), eq("DEPOSIT"),
                any(BigDecimal.class), anyString(), eq("SUCCESS"))).thenReturn(42L);

        TransactionResult result = service.deposit(new DepositRequest(new BigDecimal("250.50"), null));

        assertThat(result.referenceNumber()).isEqualTo("TXN-DEPOSIT");
        assertThat(result.transactionId()).isEqualTo(42L);
        assertThat(result.balance()).isEqualByComparingTo("1250.50");
        assertThat(result.message()).contains("simulated");

        verify(transactionRepository).insert(1L, "TXN-DEPOSIT", "DEPOSIT",
                new BigDecimal("250.50"), "Simulated deposit", "SUCCESS");
    }

    @Test
    void depositUsesTheCustomerSuppliedDescription() {
        Account account = account(1L, "1000.00", "ACTIVE");
        when(accountService.currentAccount()).thenReturn(account);
        when(referenceGenerator.next()).thenReturn("TXN-DEPOSIT");
        when(accountRepository.credit(eq(1L), any(BigDecimal.class))).thenReturn(1);
        when(transactionRepository.insert(anyLong(), anyString(), anyString(), any(), anyString(), anyString()))
                .thenReturn(7L);

        service.deposit(new DepositRequest(new BigDecimal("10.00"), "  Pocket money  "));

        verify(transactionRepository).insert(1L, "TXN-DEPOSIT", "DEPOSIT",
                new BigDecimal("10.00"), "Pocket money", "SUCCESS");
    }

    @Test
    void depositRejectsAnInactiveAccount() {
        when(accountService.currentAccount()).thenReturn(account(1L, "1000.00", "FROZEN"));

        assertThatThrownBy(() -> service.deposit(new DepositRequest(new BigDecimal("10.00"), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not active");

        verifyNoInteractions(accountRepository, transactionRepository);
    }

    @Test
    void depositRejectsZeroAndNegativeAmountsBeforeLoadingTheAccount() {
        assertThatThrownBy(() -> service.deposit(new DepositRequest(BigDecimal.ZERO, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("greater than zero");
        assertThatThrownBy(() -> service.deposit(new DepositRequest(new BigDecimal("-5"), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("greater than zero");

        verifyNoInteractions(accountService, accountRepository, transactionRepository);
    }

    @Test
    void depositFailsCleanlyWhenTheCreditUpdateAffectsNoRow() {
        when(accountService.currentAccount()).thenReturn(account(1L, "1000.00", "ACTIVE"));
        when(referenceGenerator.next()).thenReturn("TXN-DEPOSIT");
        when(accountRepository.credit(eq(1L), any(BigDecimal.class))).thenReturn(0);

        assertThatThrownBy(() -> service.deposit(new DepositRequest(new BigDecimal("10.00"), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("could not be completed");

        verify(transactionRepository, never()).insert(anyLong(), anyString(), anyString(),
                any(), anyString(), anyString());
    }

    // ---------------------------------------------------------- withdrawals --

    @Test
    void withdrawalDebitsTheBalanceAfterCheckingThePin() {
        Account account = account(1L, "1000.00", "ACTIVE");
        when(accountService.currentAccount()).thenReturn(account);
        when(passwordEncoder.matches("1234", "pin-hash")).thenReturn(true);
        when(accountRepository.debit(eq(1L), any(BigDecimal.class))).thenReturn(1);
        when(referenceGenerator.next()).thenReturn("TXN-WITHDRAWAL");
        when(transactionRepository.insert(anyLong(), anyString(), anyString(), any(), anyString(), anyString()))
                .thenReturn(43L);

        TransactionResult result = service.withdraw(
                new WithdrawalRequest(new BigDecimal("250.00"), "1234", "ATM"));

        assertThat(result.referenceNumber()).isEqualTo("TXN-WITHDRAWAL");
        assertThat(result.transactionId()).isEqualTo(43L);
        assertThat(result.balance()).isEqualByComparingTo("750.00");

        verify(transactionRepository).insert(1L, "TXN-WITHDRAWAL", "WITHDRAWAL",
                new BigDecimal("250.00"), "ATM", "SUCCESS");
    }

    @Test
    void withdrawalRejectsAWrongPinBeforeDebiting() {
        when(accountService.currentAccount()).thenReturn(account(1L, "1000.00", "ACTIVE"));
        when(passwordEncoder.matches("9999", "pin-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.withdraw(
                new WithdrawalRequest(new BigDecimal("10.00"), "9999", null)))
                .isInstanceOf(InvalidPinException.class)
                .hasMessage("Invalid transaction PIN");

        verify(accountRepository, never()).debit(anyLong(), any(BigDecimal.class));
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void withdrawalRejectsMoreThanTheAvailableBalance() {
        when(accountService.currentAccount()).thenReturn(account(1L, "100.00", "ACTIVE"));
        when(passwordEncoder.matches("1234", "pin-hash")).thenReturn(true);
        when(accountRepository.debit(eq(1L), any(BigDecimal.class))).thenReturn(0);

        assertThatThrownBy(() -> service.withdraw(
                new WithdrawalRequest(new BigDecimal("500.00"), "1234", null)))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient balance");

        verifyNoInteractions(transactionRepository);
    }

    @Test
    void withdrawalRejectsAnInactiveAccount() {
        when(accountService.currentAccount()).thenReturn(account(1L, "1000.00", "CLOSED"));

        assertThatThrownBy(() -> service.withdraw(
                new WithdrawalRequest(new BigDecimal("10.00"), "1234", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not active");

        verifyNoInteractions(accountRepository, transactionRepository);
    }

    @Test
    void withdrawalRejectsZeroAndNegativeAmounts() {
        assertThatThrownBy(() -> service.withdraw(
                new WithdrawalRequest(BigDecimal.ZERO, "1234", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("greater than zero");
        assertThatThrownBy(() -> service.withdraw(
                new WithdrawalRequest(new BigDecimal("-1"), "1234", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("greater than zero");

        verifyNoInteractions(accountService, accountRepository, transactionRepository);
    }

    // --------------------------------------------------------------- detail --

    @Test
    void detailReturnsATransactionThatBelongsToTheCaller() {
        when(accountService.currentAccount()).thenReturn(account(1L, "1000.00", "ACTIVE"));
        when(transactionRepository.findByReferenceNumber("TXN-1")).thenReturn(Optional.of(
                transaction(9L, "TXN-1", 1L, "DEPOSIT", "100.00", "Simulated deposit")));

        TransactionResponse response = service.detail("TXN-1");

        assertThat(response.referenceNumber()).isEqualTo("TXN-1");
        assertThat(response.transactionType()).isEqualTo("DEPOSIT");
        assertThat(response.direction()).isEqualTo("CREDIT");
        assertThat(response.amount()).isEqualByComparingTo("100.00");
        assertThat(response.relatedParty()).isNull();
    }

    @Test
    void detailHidesAnotherCustomersTransaction() {
        when(accountService.currentAccount()).thenReturn(account(1L, "1000.00", "ACTIVE"));
        when(transactionRepository.findByReferenceNumber("TXN-OTHER")).thenReturn(Optional.of(
                transaction(10L, "TXN-OTHER", 2L, "DEPOSIT", "100.00", "Not yours")));

        assertThatThrownBy(() -> service.detail("TXN-OTHER"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Transaction not found");
    }

    @Test
    void detailReturns404ForAnUnknownReference() {
        when(accountService.currentAccount()).thenReturn(account(1L, "1000.00", "ACTIVE"));
        when(transactionRepository.findByReferenceNumber("TXN-MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detail("TXN-MISSING"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Transaction not found");
    }

    // ----------------------------------------------------------------- list --

    @Test
    void listClampsPageAndPageSizeAndBuildsThePaginationEnvelope() {
        when(accountService.currentAccount()).thenReturn(account(1L, "1000.00", "ACTIVE"));
        when(transactionRepository.countFiltered(1L, "DEPOSIT", null, null, "rent")).thenReturn(7L);
        when(transactionRepository.findFiltered(1L, "DEPOSIT", null, null, "rent", "desc", 1, 0))
                .thenReturn(List.of(transaction(1L, "TXN-1", 1L, "DEPOSIT", "100.00", "Rent")));

        // page -5 becomes 0 and size 0 becomes 1.
        PageResponse<TransactionResponse> page = service.list(
                "DEPOSIT", null, null, "rent", "desc", -5, 0);

        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(1);
        assertThat(page.totalElements()).isEqualTo(7);
        assertThat(page.totalPages()).isEqualTo(7);
        assertThat(page.first()).isTrue();
        assertThat(page.last()).isFalse();
        assertThat(page.content()).hasSize(1);
    }

    @Test
    void listCapsThePageSizeAtOneHundred() {
        when(accountService.currentAccount()).thenReturn(account(1L, "1000.00", "ACTIVE"));
        when(transactionRepository.countFiltered(1L, null, null, null, null)).thenReturn(250L);
        when(transactionRepository.findFiltered(1L, null, null, null, null, "asc", 100, 200))
                .thenReturn(List.of());

        PageResponse<TransactionResponse> page = service.list(null, null, null, null, "asc", 2, 500);

        assertThat(page.size()).isEqualTo(100);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.first()).isFalse();
        assertThat(page.last()).isTrue();
    }

    @Test
    void listParsesInclusiveDateRanges() {
        when(accountService.currentAccount()).thenReturn(account(1L, "1000.00", "ACTIVE"));
        when(transactionRepository.countFiltered(eq(1L), isNull(), any(), any(), isNull())).thenReturn(0L);
        when(transactionRepository.findFiltered(eq(1L), isNull(), any(), any(), isNull(),
                eq("desc"), eq(10), eq(0))).thenReturn(List.of());

        PageResponse<TransactionResponse> page = service.list(
                null, "2026-01-01", "2026-01-31", null, "desc", 0, 10);

        assertThat(page.totalPages()).isZero();
        assertThat(page.first()).isTrue();
        assertThat(page.last()).isTrue();

        verify(transactionRepository).countFiltered(1L, null,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 31, 23, 59, 59, 999_999_999), null);
    }

    @Test
    void transferHistoryMapsTheAuditView() {
        when(accountService.currentAccount()).thenReturn(account(1L, "1000.00", "ACTIVE"));
        when(transactionRepository.findTransfers(1L)).thenReturn(List.of(
                new TransactionRepository.TransferRow("TXN-TRANSFER", "HFDC1000000002",
                        new BigDecimal("150.25"), "Rent", LocalDateTime.now(), "DEBIT")));

        List<TransactionResponse> history = service.transferHistory();

        assertThat(history).hasSize(1);
        assertThat(history.get(0).transactionType()).isEqualTo("TRANSFER_DEBIT");
        assertThat(history.get(0).direction()).isEqualTo("DEBIT");
        assertThat(history.get(0).relatedParty()).isEqualTo("HFDC1000000002");
        assertThat(history.get(0).amount()).isEqualByComparingTo("150.25");
    }

    private static Account account(long id, String balance, String status) {
        return new Account(id, "CID1000000001", "HFDC1000000001", "SAVINGS",
                new BigDecimal(balance), "pin-hash", status, LocalDateTime.now());
    }

    private static Transaction transaction(long id, String reference, long accountId,
                                           String type, String amount, String description) {
        return new Transaction(id, reference, accountId, type, new BigDecimal(amount),
                description, "SUCCESS", LocalDateTime.now());
    }
}
