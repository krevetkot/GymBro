package ru.itmo.gymbro.shared.web;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import ru.itmo.gymbro.shared.api.UnauthorizedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityContextCurrentUserProviderTest {

    private final SecurityContextCurrentUserProvider provider = new SecurityContextCurrentUserProvider();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void readsIdFromAuthenticatedJwt() {
        authenticateWithSubject("42");
        assertThat(provider.currentUserId()).isEqualTo(42L);
    }

    @Test
    void rejectsMissingAuthentication() {
        assertThatThrownBy(provider::currentUserId).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejectsNonJwtAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("42", null, List.of()));
        assertThatThrownBy(provider::currentUserId).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejectsNonNumericOrNonPositiveSubject() {
        for (String subject : new String[] {"not-an-id", "0", "-5"}) {
            authenticateWithSubject(subject);
            assertThatThrownBy(provider::currentUserId)
                    .as(subject)
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    private static void authenticateWithSubject(String subject) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
