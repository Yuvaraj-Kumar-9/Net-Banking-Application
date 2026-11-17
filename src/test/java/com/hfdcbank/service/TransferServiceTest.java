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
import com.hfdcbank.util.TransactionReferenceGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the money-moving transfer logic. The transaction manager is a
 * no-op fake so the service's TransactionTemplate works without a database.
 * The real BEGIN/COMMIT/ROLLBACK behaviour is covered by TransferConcurrencyTest.
 */
@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

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

    private TransferService service;

    private final PlatformTransactionManager transactionManager = new PlatformTransactionManager() {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
            // no-op: the unit test is not talking to a database
        }

        @Override
        public void rollback(TransactionStatus status) {
            // no-op: the unit test is not talking to a database
        }
    };

    @BeforeEach
    void setUp() {
        service = new TransferService(accountService, accountRepository, transactionRepository,
                referenceGenerator, passwordEncoder, transactionManager);
    }

    @Test
    void transfersMoneyAtomicallyAndWritesBothLedgerRows() {
        Account sender = account(1L, "HFDC1000000001", "1000.00", "ACTIVE");
        Account receiver = account(2L, "HFDC1000000002", "0.00", "ACTIVE");
        Customer customer = customer("Aarav Sharma");
        TransferRequest request = new TransferRequest(
                "HFDC1000000002", "Priya Nair", new BigDecimal("100.00"), "Rent", "1234");

        when(accountService.currentAccount()).thenReturn(sender);
        when(accountService.currentCustomer()).thenReturn(customer);
        when(passwordEncoder.matches("1234", sender.transactionPinHash())).thenReturn(true);
        when(accountRepository.findByAccountNumber("HFDC1000000002")).thenReturn(Optional.of(receiver));
        when(accountRepository.lockById(1L)).thenReturn(Optional.of(sender));
        when(accountRepository.lockById(2L)).thenReturn(Optional.of(receiver));
        when(accountRepository.debit(eq(1L), any(BigDecimal.class))).thenReturn(1);
        when(accountRepository.credit(eq(2L), any(BigDecimal.class))).thenReturn(1);
        when(referenceGenerator.next()).thenReturn("REF-TRANSFER", "REF-OUT", "REF-IN");
        when(transactionRepository.insert(anyLong(), anyString(), anyString(), any(), anyString(), anyString()))
                .thenReturn(11L);
        when(transactionRepository.insertTransfer(anyString(), anyLong(), anyLong(), any(), any()))
                .thenReturn(22L);

        TransferResponse response = service.transfer(request);

        assertThat(response.referenceNumber()).isEqualTo("REF-TRANSFER");
        assertThat(response.amount()).isEqualByComparingTo("100.00");
        assertThat(response.balance()).isEqualByComparingTo("900.00");
        assertThat(response.beneficiaryAccountNumber()).isEqualTo("HFDC1000000002");
        assertThat(response.beneficiaryName()).isEqualTo("Priya Nair");
        assertThat(response.message()).contains("successfully");

        verify(transactionRepository).insert(eq(1L), eq("REF-OUT"), eq("TRANSFER_OUT"),
                eq(new BigDecimal("100.00")),
                contains("Transfer to HFDC1000000002 (Priya Nair) - Rent"), eq("SUCCESS"));
        verify(transactionRepository).insert(eq(2L), eq("REF-IN"), eq("TRANSFER_IN"),
                eq(new BigDecimal("100.00")),
                contains("Transfer from HFDC1000000001 (Aarav Sharma)"), eq("SUCCESS"));
        verify(transactionRepository).insertTransfer(eq("REF-TRANSFER"), eq(1L), eq(2L),
                eq(new BigDecimal("100.00")), eq("Rent"));
    }

    @Test
    void locksAccountsInAscendingIdOrderToAvoidDeadlocks() {
        // Sender has the HIGHER id: the service must still lock the lower id first.
        Account sender = account(5L, "HFDC1000000005", "1000.00", "ACTIVE");
        Account receiver = account(2L, "HFDC1000000002", "0.00", "ACTIVE");
        Customer customer = customer("Aarav Sharma");

        when(accountService.currentAccount()).thenReturn(sender);
        when(accountService.currentCustomer()).thenReturn(customer);
        when(passwordEncoder.matches("1234", sender.transactionPinHash())).thenReturn(true);
        when(accountRepository.findByAccountNumber("HFDC1000000002")).thenReturn(Optional.of(receiver));
        when(accountRepository.lockById(2L)).thenReturn(Optional.of(receiver));
        when(accountRepository.lockById(5L)).thenReturn(Optional.of(sender));
        when(accountRepository.debit(eq(5L), any(BigDecimal.class))).thenReturn(1);
        when(accountRepository.credit(eq(2L), any(BigDecimal.class))).thenReturn(1);
        when(referenceGenerator.next()).thenReturn("REF1", "REF2", "REF3");
        when(transactionRepository.insert(anyLong(), anyString(), anyString(), any(), anyString(), anyString()))
                .thenReturn(1L);
        when(transactionRepository.insertTransfer(anyString(), anyLong(), anyLong(), any(), any()))
                .thenReturn(1L);

        service.transfer(new TransferRequest("HFDC1000000002", "Priya Nair",
                new BigDecimal("10.00"), null, "1234"));

        InOrder order = inOrder(accountRepository);
        order.verify(accountRepository).lockById(2L);
        order.verify(accountRepository).lockById(5L);
        order.verify(accountRepository).debit(eq(5L), any(BigDecimal.class));
        order.verify(accountRepository).credit(eq(2L), any(BigDecimal.class));
    }

    @Test
    void rejectsAnIncorrectTransactionPinBeforeAnyMoneyMoves() {
        Account sender = account(1L, "HFDC1000000001", "1000.00", "ACTIVE");
        when(accountService.currentAccount()).thenReturn(sender);
        when(accountService.currentCustomer()).thenReturn(customer("Aarav Sharma"));
        when(passwordEncoder.matches("0000", sender.transactionPinHash())).thenReturn(false);

        assertThatThrownBy(() -> service.transfer(new TransferRequest("HFDC1000000002",
                "Priya Nair", new BigDecimal("10.00"), null, "0000")))
                .isInstanceOf(InvalidPinException.class)
                .hasMessage("Invalid transaction PIN");

        verify(accountRepository, never()).findByAccountNumber(anyString());
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void rejectsATransferToTheCallersOwnAccount() {
        Account sender = account(1L, "HFDC1000000001", "1000.00", "ACTIVE");
        when(accountService.currentAccount()).thenReturn(sender);
        when(accountService.currentCustomer()).thenReturn(customer("Aarav Sharma"));

        assertThatThrownBy(() -> service.transfer(new TransferRequest("HFDC1000000001",
                "Myself", new BigDecimal("10.00"), null, "1234")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("own account");

        verifyNoInteractions(passwordEncoder, accountRepository, transactionRepository);
    }

    @Test
    void rejectsZeroOrNegativeAmounts() {
        assertThatThrownBy(() -> service.transfer(new TransferRequest("HFDC1000000002",
                "Priya Nair", BigDecimal.ZERO, null, "1234")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("greater than zero");

        assertThatThrownBy(() -> service.transfer(new TransferRequest("HFDC1000000002",
                "Priya Nair", new BigDecimal("-1.00"), null, "1234")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("greater than zero");

        verifyNoInteractions(accountService, accountRepository, transactionRepository, passwordEncoder);
    }

    @Test
    void rejectsAnInactiveSenderAccount() {
        Account sender = account(1L, "HFDC1000000001", "1000.00", "FROZEN");
        when(accountService.currentAccount()).thenReturn(sender);
        when(accountService.currentCustomer()).thenReturn(customer("Aarav Sharma"));

        assertThatThrownBy(() -> service.transfer(new TransferRequest("HFDC1000000002",
                "Priya Nair", new BigDecimal("10.00"), null, "1234")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not active");

        verifyNoInteractions(passwordEncoder, accountRepository);
    }

    @Test
    void rejectsAnUnknownBeneficiaryAccount() {
        Account sender = account(1L, "HFDC1000000001", "1000.00", "ACTIVE");
        when(accountService.currentAccount()).thenReturn(sender);
        when(accountService.currentCustomer()).thenReturn(customer("Aarav Sharma"));
        when(passwordEncoder.matches("1234", sender.transactionPinHash())).thenReturn(true);
        when(accountRepository.findByAccountNumber("HFDC9999999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transfer(new TransferRequest("HFDC9999999999",
                "Ghost", new BigDecimal("10.00"), null, "1234")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Beneficiary account not found");

        verify(accountRepository, never()).lockById(anyLong());
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void rejectsAnInactiveBeneficiaryAccount() {
        Account sender = account(1L, "HFDC1000000001", "1000.00", "ACTIVE");
        Account receiver = account(2L, "HFDC1000000002", "0.00", "CLOSED");
        when(accountService.currentAccount()).thenReturn(sender);
        when(accountService.currentCustomer()).thenReturn(customer("Aarav Sharma"));
        when(passwordEncoder.matches("1234", sender.transactionPinHash())).thenReturn(true);
        when(accountRepository.findByAccountNumber("HFDC1000000002")).thenReturn(Optional.of(receiver));

        assertThatThrownBy(() -> service.transfer(new TransferRequest("HFDC1000000002",
                "Priya Nair", new BigDecimal("10.00"), null, "1234")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("beneficiary account is not active");

        verifyNoInteractions(transactionRepository);
    }

    @Test
    void refusesToTransferMoreThanTheLockedBalance() {
        Account sender = account(1L, "HFDC1000000001", "50.00", "ACTIVE");
        Account receiver = account(2L, "HFDC1000000002", "0.00", "ACTIVE");
        when(accountService.currentAccount()).thenReturn(sender);
        when(accountService.currentCustomer()).thenReturn(customer("Aarav Sharma"));
        when(passwordEncoder.matches("1234", sender.transactionPinHash())).thenReturn(true);
        when(accountRepository.findByAccountNumber("HFDC1000000002")).thenReturn(Optional.of(receiver));
        when(accountRepository.lockById(1L)).thenReturn(Optional.of(sender));
        when(accountRepository.lockById(2L)).thenReturn(Optional.of(receiver));

        assertThatThrownBy(() -> service.transfer(new TransferRequest("HFDC1000000002",
                "Priya Nair", new BigDecimal("100.00"), null, "1234")))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient balance");

        verify(accountRepository, never()).debit(anyLong(), any(BigDecimal.class));
        verify(accountRepository, never()).credit(anyLong(), any(BigDecimal.class));
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void rollsBackWhenTheAtomicDebitLosesARace() {
        Account sender = account(1L, "HFDC1000000001", "1000.00", "ACTIVE");
        Account receiver = account(2L, "HFDC1000000002", "0.00", "ACTIVE");
        when(accountService.currentAccount()).thenReturn(sender);
        when(accountService.currentCustomer()).thenReturn(customer("Aarav Sharma"));
        when(passwordEncoder.matches("1234", sender.transactionPinHash())).thenReturn(true);
        when(accountRepository.findByAccountNumber("HFDC1000000002")).thenReturn(Optional.of(receiver));
        when(accountRepository.lockById(1L)).thenReturn(Optional.of(sender));
        when(accountRepository.lockById(2L)).thenReturn(Optional.of(receiver));
        // The balance changed between the lock read and the UPDATE.
        when(accountRepository.debit(eq(1L), any(BigDecimal.class))).thenReturn(0);

        assertThatThrownBy(() -> service.transfer(new TransferRequest("HFDC1000000002",
                "Priya Nair", new BigDecimal("100.00"), null, "1234")))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient balance");

        verify(accountRepository, never()).credit(anyLong(), any(BigDecimal.class));
        verifyNoInteractions(transactionRepository);
    }

    private static Account account(long id, String number, String balance, String status) {
        return new Account(id, "CID1000000001", number, "SAVINGS", new BigDecimal(balance),
                "pin-hash", status, LocalDateTime.now());
    }

    private static Customer customer(String fullName) {
        return new Customer("CID1000000001", fullName, "aarav@hfdcbank.test", "9876543210",
                LocalDate.of(1992, 4, 18), "12 Marine Drive, Mumbai", "password-hash",
                LocalDateTime.now(), LocalDateTime.now());
    }
}
