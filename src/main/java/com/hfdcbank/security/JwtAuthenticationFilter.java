package com.hfdcbank.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hfdcbank.dto.ApiError;
import com.hfdcbank.util.CookieUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Set;

/**
 * Two jobs:
 *  1. Authenticate each API request from the HttpOnly JWT cookie (or a Bearer
 *     header for tools such as Postman/Swagger).
 *  2. Enforce double-submit-cookie CSRF protection on every state-changing
 *     /api request: the XSRF-TOKEN cookie must match the X-XSRF-TOKEN header.
 *
 * A Bearer header is immune to CSRF (browsers never attach it automatically),
 * so the CSRF check is skipped in that case.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Exactly the endpoints that must work before a session exists. */
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/logout",
            "/api/auth/csrf",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/health"
    );
    private static final Set<String> MUTATING = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final CookieUtil cookieUtil;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   CustomUserDetailsService userDetailsService,
                                   CookieUtil cookieUtil,
                                   ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.cookieUtil = cookieUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod().toUpperCase();

        // CORS pre-flight must never require authentication.
        if ("OPTIONS".equals(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean bearerUsed = request.getHeader("Authorization") != null
                && request.getHeader("Authorization").startsWith("Bearer ");

        if (path.startsWith("/api/") && MUTATING.contains(method)
                && !path.startsWith("/swagger-ui") && !bearerUsed) {
            String cookieToken = cookieUtil.read(request, CookieUtil.CSRF_COOKIE);
            String headerToken = request.getHeader(CookieUtil.CSRF_HEADER);
            if (cookieToken == null || headerToken == null
                    || !MessageDigest.isEqual(cookieToken.getBytes(StandardCharsets.UTF_8),
                    headerToken.getBytes(StandardCharsets.UTF_8))) {
                writeError(response, HttpStatus.FORBIDDEN, "CSRF token missing or invalid");
                return;
            }
        }

        if (!isPublic(path) && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = extractToken(request);
            if (token != null && jwtService.isValid(token)) {
                try {
                    String email = jwtService.extractSubject(token);
                    UserDetails user = userDetailsService.loadUserByUsername(email);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (RuntimeException ex) {
                    SecurityContextHolder.clearContext();
                    writeError(response, HttpStatus.UNAUTHORIZED, "Your session is no longer valid");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublic(String path) {
        // Only /api/** requires a JWT. Static assets and SPA routes are public:
        // an expired cookie must never stop the landing page from loading.
        if (!path.startsWith("/api/")) {
            return true;
        }
        return PUBLIC_PATHS.contains(path);
    }

    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        return cookieUtil.read(request, CookieUtil.TOKEN_COOKIE);
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        ApiError error = new ApiError(Instant.now(), status.value(),
                status.getReasonPhrase(), message, "security");
        objectMapper.writeValue(response.getWriter(), error);
    }
}
