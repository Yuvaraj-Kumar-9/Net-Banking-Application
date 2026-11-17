package com.hfdcbank.service;

import com.hfdcbank.dto.BeneficiaryResponse;
import com.hfdcbank.entity.Account;
import com.hfdcbank.entity.Beneficiary;
import com.hfdcbank.exception.BusinessException;
import com.hfdcbank.exception.DuplicateResourceException;
import com.hfdcbank.exception.ResourceNotFoundException;
import com.hfdcbank.repository.AccountRepository;
import com.hfdcbank.repository.BeneficiaryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** Beneficiary CRUD, always scoped to the logged-in customer. */
@Service
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;

    public BeneficiaryService(BeneficiaryRepository beneficiaryRepository,
                              AccountRepository accountRepository,
                              AccountService accountService) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.accountRepository = accountRepository;
        this.accountService = accountService;
    }

    public BeneficiaryResponse add(String name, String accountNumber) {
        String number = accountNumber.trim();
        String customerId = accountService.currentCustomer().customerId();

        if (accountService.currentAccount().accountNumber().equals(number)) {
            throw new BusinessException("You cannot add your own account as a beneficiary");
        }
        Account target = accountRepository.findByAccountNumber(number)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary account not found"));
        if (!target.isActive()) {
            throw new BusinessException("The beneficiary account is not active");
        }
        if (beneficiaryRepository.existsForCustomer(customerId, number)) {
            throw new DuplicateResourceException("This beneficiary is already saved");
        }

        long id = beneficiaryRepository.insert(customerId, name.trim(), number);
        Beneficiary saved = beneficiaryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary could not be saved"));
        return toResponse(saved);
    }

    public List<BeneficiaryResponse> list(String search) {
        String customerId = accountService.currentCustomer().customerId();
        return beneficiaryRepository.findByCustomerId(customerId, search).stream()
                .map(BeneficiaryService::toResponse)
                .toList();
    }

    public void delete(long beneficiaryId) {
        String customerId = accountService.currentCustomer().customerId();
        int deleted = beneficiaryRepository.deleteOwned(beneficiaryId, customerId);
        if (deleted == 0) {
            throw new ResourceNotFoundException("Beneficiary not found");
        }
    }

    private static BeneficiaryResponse toResponse(Beneficiary beneficiary) {
        return new BeneficiaryResponse(beneficiary.beneficiaryId(), beneficiary.beneficiaryName(),
                beneficiary.beneficiaryAccountNumber(), beneficiary.createdAt());
    }
}
