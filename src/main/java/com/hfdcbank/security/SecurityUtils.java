package com.hfdcbank.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Helper used by services to obtain the logged-in customer's email safely. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static String currentEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("You must be signed in");
        }
        return authentication.getName();
    }
}
