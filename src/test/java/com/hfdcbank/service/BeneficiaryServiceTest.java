package com.hfdcbank.service;

import com.hfdcbank.dto.BeneficiaryResponse;
import com.hfdcbank.entity.Account;
import com.hfdcbank.entity.Beneficiary;
import com.hfdcbank.entity.Customer;
import com.hfdcbank.exception.BusinessException;
import com.hfdcbank.exception.DuplicateResourceException;
import com.hfdcbank.exception.ResourceNotFoundException;
import com.hfdcbank.repository.AccountRepository;
import com.hfdcbank.repository.BeneficiaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Beneficiary add/list/delete rules, including ownership and duplicate guards. */
@ExtendWith(MockitoExtension.class)
class BeneficiaryServiceTest {

    @Mock
    private BeneficiaryRepository beneficiaryRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountService accountService;

    private BeneficiaryService service;

    @BeforeEach
    void setUp() {
        service = new BeneficiaryService(beneficiaryRepository, accountRepository, accountService);
    }

    @Test
    void addValidatesTheTargetAccountAndReturnsTheSavedBeneficiary() {
        when(accountService.currentCustomer()).thenReturn(customer());
        when(accountService.currentAccount()).thenReturn(account(1L, "HFDC1000000001", "ACTIVE"));
        when(accountRepository.findByAccountNumber("HFDC1000000002"))
                .thenReturn(Optional.of(account(2L, "HFDC1000000002", "ACTIVE")));
        when(beneficiaryRepository.existsForCustomer("CID1000000001", "HFDC1000000002")).thenReturn(false);
        when(beneficiaryRepository.insert("CID1000000001", "Priya Nair", "HFDC1000000002")).thenReturn(7L);
        when(beneficiaryRepository.findById(7L)).thenReturn(Optional.of(
                new Beneficiary(7L, "CID1000000001", "Priya Nair", "HFDC1000000002", LocalDateTime.now())));

        BeneficiaryResponse response = service.add("  Priya Nair  ", " HFDC1000000002 ");

        assertThat(response.beneficiaryId()).isEqualTo(7L);
        assertThat(response.beneficiaryName()).isEqualTo("Priya Nair");
        assertThat(response.beneficiaryAccountNumber()).isEqualTo("HFDC1000000002");
        verify(beneficiaryRepository).insert("CID1000000001", "Priya Nair", "HFDC1000000002");
    }

    @Test
    void addRejectsTheCallersOwnAccount() {
        when(accountService.currentCustomer()).thenReturn(customer());
        when(accountService.currentAccount()).thenReturn(account(1L, "HFDC1000000001", "ACTIVE"));

        assertThatThrownBy(() -> service.add("Myself", "HFDC1000000001"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("own account");

        verifyNoInteractions(accountRepository, beneficiaryRepository);
    }

    @Test
    void addRejectsAnAccountThatDoesNotExist() {
        when(accountService.currentCustomer()).thenReturn(customer());
        when(accountService.currentAccount()).thenReturn(account(1L, "HFDC1000000001", "ACTIVE"));
        when(accountRepository.findByAccountNumber("HFDC9999999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.add("Ghost", "HFDC9999999999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Beneficiary account not found");

        verifyNoInteractions(beneficiaryRepository);
    }

    @Test
    void addRejectsAnInactiveBeneficiaryAccount() {
        when(accountService.currentCustomer()).thenReturn(customer());
        when(accountService.currentAccount()).thenReturn(account(1L, "HFDC1000000001", "ACTIVE"));
        when(accountRepository.findByAccountNumber("HFDC1000000002"))
                .thenReturn(Optional.of(account(2L, "HFDC1000000002", "CLOSED")));

        assertThatThrownBy(() -> service.add("Priya Nair", "HFDC1000000002"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not active");

        verifyNoInteractions(beneficiaryRepository);
    }

    @Test
    void addRejectsADuplicateBeneficiary() {
        when(accountService.currentCustomer()).thenReturn(customer());
        when(accountService.currentAccount()).thenReturn(account(1L, "HFDC1000000001", "ACTIVE"));
        when(accountRepository.findByAccountNumber("HFDC1000000002"))
                .thenReturn(Optional.of(account(2L, "HFDC1000000002", "ACTIVE")));
        when(beneficiaryRepository.existsForCustomer("CID1000000001", "HFDC1000000002")).thenReturn(true);

        assertThatThrownBy(() -> service.add("Priya Nair", "HFDC1000000002"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("This beneficiary is already saved");

        verify(beneficiaryRepository, never()).insert(anyString(), anyString(), anyString());
    }

    @Test
    void listMapsRowsAndPassesTheSearchTermThrough() {
        when(accountService.currentCustomer()).thenReturn(customer());
        when(beneficiaryRepository.findByCustomerId("CID1000000001", "pri")).thenReturn(List.of(
                new Beneficiary(3L, "CID1000000001", "Priya Nair", "HFDC1000000002", LocalDateTime.now())));

        List<BeneficiaryResponse> result = service.list("pri");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).beneficiaryName()).isEqualTo("Priya Nair");
    }

    @Test
    void deleteRemovesOnlyABeneficiaryOwnedByTheCaller() {
        when(accountService.currentCustomer()).thenReturn(customer());
        when(beneficiaryRepository.deleteOwned(5L, "CID1000000001")).thenReturn(1);

        service.delete(5L);

        verify(beneficiaryRepository).deleteOwned(5L, "CID1000000001");
    }

    @Test
    void deleteReturns404WhenTheRowBelongsToSomeoneElseOrIsGone() {
        when(accountService.currentCustomer()).thenReturn(customer());
        when(beneficiaryRepository.deleteOwned(5L, "CID1000000001")).thenReturn(0);

        assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Beneficiary not found");
    }

    private static Customer customer() {
        return new Customer("CID1000000001", "Aarav Sharma", "aarav@hfdcbank.test",
                "9876543210", LocalDate.of(1992, 4, 18), "12 Marine Drive, Mumbai",
                "password-hash", LocalDateTime.now(), LocalDateTime.now());
    }

    private static Account account(long id, String number, String status) {
        return new Account(id, "CID1000000001", number, "SAVINGS", new BigDecimal("1000.00"),
                "pin-hash", status, LocalDateTime.now());
    }
}
