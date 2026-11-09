package com.example.itsm.security;

import com.example.itsm.user.Role;
import com.example.itsm.user.User;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtServiceTest {

    private static final String SECRET =
            "test-only-secret-that-is-long-enough-for-hs384-signing-1234567890";

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-09T10:00:00Z"), ZoneOffset.UTC);
    private final JwtService jwtService = new JwtService(SECRET, 60, clock);

    private User user() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(user.getEmail()).thenReturn("technician@example.test");
        when(user.getName()).thenReturn("Test Technician");
        when(user.getRole()).thenReturn(Role.TECHNICIAN);
        return user;
    }

    @Test
    void generatedTokenCarriesIdentityAndValidatesAgainstPrincipal() {
        String token = jwtService.generateToken(user());

        assertThat(jwtService.extractUsername(token)).isEqualTo("technician@example.test");

        UserPrincipal principal = new UserPrincipal(
                7L,
                "Test Technician",
                "technician@example.test",
                "hash",
                Role.TECHNICIAN,
                true
        );
        assertThat(jwtService.isTokenValid(token, principal)).isTrue();
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken(user());
        String tampered = token.substring(0, token.length() - 3) + "abc";

        UserPrincipal principal = new UserPrincipal(
                7L,
                "Test Technician",
                "technician@example.test",
                "hash",
                Role.TECHNICIAN,
                true
        );
        assertThat(jwtService.isTokenValid(tampered, principal)).isFalse();
    }
}
