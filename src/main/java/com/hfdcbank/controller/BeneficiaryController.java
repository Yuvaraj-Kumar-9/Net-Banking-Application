package com.hfdcbank.controller;

import com.hfdcbank.dto.BeneficiaryResponse;
import com.hfdcbank.dto.MessageResponse;
import com.hfdcbank.service.BeneficiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Beneficiary management. The customer id never comes from the client. */
@RestController
@RequestMapping("/api/beneficiaries")
@Tag(name = "Beneficiaries", description = "Saved transfer recipients")
@SecurityRequirement(name = "cookieAuth")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    public BeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    @PostMapping
    @Operation(summary = "Save a verified beneficiary account")
    public ResponseEntity<BeneficiaryResponse> add(@Valid @RequestBody AddBeneficiaryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(beneficiaryService.add(request.name(), request.accountNumber()));
    }

    @GetMapping
    @Operation(summary = "List (and optionally search) your beneficiaries")
    public List<BeneficiaryResponse> list(@RequestParam(required = false) String search) {
        return beneficiaryService.list(search);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete one of your beneficiaries")
    public MessageResponse delete(@PathVariable long id) {
        beneficiaryService.delete(id);
        return new MessageResponse("Beneficiary removed");
    }

    /** Small request record local to this controller. */
    public record AddBeneficiaryRequest(
            @NotBlank(message = "Beneficiary name is required") String name,
            @NotBlank(message = "Account number is required") String accountNumber
    ) {
    }
}
