package com.hfdcbank.controller;

import com.hfdcbank.dto.ChangePasswordRequest;
import com.hfdcbank.dto.MessageResponse;
import com.hfdcbank.dto.ProfileResponse;
import com.hfdcbank.dto.ProfileUpdateRequest;
import com.hfdcbank.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Profile read/update and password change. */
@RestController
@RequestMapping("/api/profile")
@Tag(name = "Profile", description = "Customer profile and password management")
@SecurityRequirement(name = "cookieAuth")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    @Operation(summary = "Get your profile information")
    public ProfileResponse get() {
        return profileService.getProfile();
    }

    @PutMapping
    @Operation(summary = "Update permitted profile fields")
    public ProfileResponse update(@Valid @RequestBody ProfileUpdateRequest request) {
        return profileService.update(request);
    }

    @PutMapping("/change-password")
    @Operation(summary = "Change password after verifying the current one")
    public MessageResponse changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(request);
        return new MessageResponse("Password changed successfully");
    }
}
