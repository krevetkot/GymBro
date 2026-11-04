package ru.itmo.gymbro.shared.web;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.itmo.gymbro.shared.api.UnauthorizedException;

class SecurityContextCurrentUserProviderTest {

    private final SecurityContextCurrentUserProvider provider = new SecurityContextCurrentUserProvider();

    @Test
    void readsIdFromAuthenticatedJwt() {
        StepVerifier.create(withSubject("42")).expectNext(42L).verifyComplete();
    }

    @Test
    void rejectsMissingAuthentication() {
        StepVerifier.create(provider.currentUserId()).verifyError(UnauthorizedException.class);
    }

    @Test
    void rejectsNonNumericSubject() {
        StepVerifier.create(withSubject("not-an-id")).verifyError(UnauthorizedException.class);
    }

    private Mono<Long> withSubject(String subject) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextImpl context = new SecurityContextImpl(new JwtAuthenticationToken(jwt));
        return provider.currentUserId()
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context)));
    }
}
