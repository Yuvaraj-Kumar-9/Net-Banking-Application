package com.hfdcbank.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Centralises cookie handling. The JWT cookie is HttpOnly (JavaScript cannot
 * read it). A separate readable CSRF cookie is required as a header on every
 * state-changing request - this is the double-submit-cookie pattern.
 */
@Component
public class CookieUtil {

    public static final String TOKEN_COOKIE = "hfdc_token";
    public static final String CSRF_COOKIE = "XSRF-TOKEN";
    public static final String CSRF_HEADER = "X-XSRF-TOKEN";

    @Value("${hfdc.cookie.secure:false}")
    private boolean secure;

    @Value("${hfdc.cookie.same-site:lax}")
    private String sameSite;

    public String read(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public void writeAuthCookie(HttpServletResponse response, String token) {
        write(response, TOKEN_COOKIE, token, true, (int) Duration.ofDays(1).toSeconds());
    }

    public void writeCsrfCookie(HttpServletResponse response, String token) {
        write(response, CSRF_COOKIE, token, false, (int) Duration.ofDays(1).toSeconds());
    }

    public void clearAll(HttpServletResponse response) {
        write(response, TOKEN_COOKIE, "", true, 0);
        write(response, CSRF_COOKIE, "", false, 0);
    }

    private void write(HttpServletResponse response, String name, String value,
                       boolean httpOnly, int maxAge) {
        // ResponseCookie supports SameSite; Servlet Cookie does not in Servlet 6.
        String sameSiteValue = "none".equalsIgnoreCase(sameSite) ? "None" : "Lax";
        String header = name + "=" + value
                + "; Path=/"
                + "; Max-Age=" + maxAge
                + "; HttpOnly=" + httpOnly
                + "; SameSite=" + sameSiteValue
                + (secure ? "; Secure" : "");
        response.addHeader("Set-Cookie", header);
    }
}
