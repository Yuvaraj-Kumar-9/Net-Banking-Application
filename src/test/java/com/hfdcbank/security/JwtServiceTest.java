package com.hfdcbank.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** JWT creation, subject extraction and rejection of tampered/expired tokens. */
class JwtServiceTest {

    private static final String SECRET =
            "unit-test-only-hfdc-bank-jwt-secret-that-is-long-enough-1234567890";

    @Test
    void generatesATokenWithTheEmailAsSubject() {
        JwtService service = new JwtService(SECRET, 60_000);

        String token = service.generateToken("aarav@hfdcbank.test");

        assertThat(token).isNotBlank();
        assertThat(service.isValid(token)).isTrue();
        assertThat(service.extractSubject(token)).isEqualTo("aarav@hfdcbank.test");
    }

    @Test
    void rejectsMalformedAndTamperedTokens() {
        JwtService service = new JwtService(SECRET, 60_000);
        String token = service.generateToken("aarav@hfdcbank.test");

        assertThat(service.isValid("not-a-jwt")).isFalse();
        assertThat(service.isValid(token + "tampered")).isFalse();

        // Change the first signature character. The final Base64url character
        // can carry unused padding bits, so changing it may decode identically.
        int signatureStart = token.lastIndexOf('.') + 1;
        char original = token.charAt(signatureStart);
        char replacement = original == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, signatureStart)
                + replacement + token.substring(signatureStart + 1);
        assertThat(service.isValid(tampered)).isFalse();
    }

    @Test
    void rejectsTokensSignedWithADifferentSecret() {
        JwtService issuer = new JwtService(SECRET, 60_000);
        JwtService verifier = new JwtService(
                "another-unit-test-secret-that-is-also-long-enough-0987654321", 60_000);

        assertThat(verifier.isValid(issuer.generateToken("aarav@hfdcbank.test"))).isFalse();
    }

    @Test
    void rejectsExpiredTokens() {
        JwtService expiredIssuer = new JwtService(SECRET, -1_000);

        String token = expiredIssuer.generateToken("aarav@hfdcbank.test");

        assertThat(expiredIssuer.isValid(token)).isFalse();
    }

    @Test
    void refusesToSignWithASecretShorterThan256Bits() {
        assertThatThrownBy(() -> new JwtService("too-short", 60_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }
}
