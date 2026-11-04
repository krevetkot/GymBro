package ru.itmo.gymbro.identity.web;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.identity.dto.TokenResponse;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

class AuthApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ReactiveJwtDecoder decoder;

    @Autowired
    private JwtEncoder encoder;

    @Autowired
    private UserRepository users;

    @Test
    void loginIssuesSignedJwtWithoutPassword() {
        TokenResponse response = login("admin@gymbro.local", "admin12345")
                .expectStatus().isOk()
                .expectBody(TokenResponse.class).returnResult().getResponseBody();

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600);
        assertThat(response.accessToken()).isNotBlank();

        var jwt = decoder.decode(response.accessToken()).block();
        assertThat(jwt.getSubject()).isEqualTo(users.findByEmail("admin@gymbro.local").orElseThrow().getId().toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
        assertThat(jwt.getClaims()).doesNotContainKeys("password", "passwordHash");

        client.get().uri("/api/v1/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + response.accessToken())
                .exchange().expectStatus().isOk();
    }

    @Test
    void rejectsWrongCredentialsAndBannedUser() {
        login("admin@gymbro.local", "wrong-password").expectStatus().isUnauthorized();
        login("unknown@gymbro.local", "admin12345").expectStatus().isUnauthorized();

        User admin = users.findByEmail("admin@gymbro.local").orElseThrow();
        admin.ban();
        users.save(admin);
        try {
            login("admin@gymbro.local", "admin12345").expectStatus().isUnauthorized();
        } finally {
            admin.activate();
            users.save(admin);
        }
    }

    @Test
    void requiresBearerTokenAndIgnoresUserIdHeader() {
        client.get().uri("/api/v1/users").exchange().expectStatus().isUnauthorized();
        client.get().uri("/api/v1/users").header("X-User-Id", "1")
                .exchange().expectStatus().isUnauthorized();
        client.get().uri("/api/v1/users").header(HttpHeaders.AUTHORIZATION, "Bearer forged")
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void rejectsExpiredSignedToken() {
        Instant past = Instant.now().minusSeconds(7200);
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder()
                        .issuer("gymbro-identity")
                        .subject("1")
                        .issuedAt(past)
                        .expiresAt(past.plusSeconds(3600))
                        .build())).getTokenValue();

        client.get().uri("/api/v1/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void validatesLoginBody() {
        client.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "bad", "password", ""))
                .exchange().expectStatus().isBadRequest();
    }

    private org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec login(String email, String password) {
        return client.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "password", password))
                .exchange();
    }
}
