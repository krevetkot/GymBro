package ru.itmo.gymbro.identity.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.identity.dto.TokenResponse;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

class UserAuthorizationIntegrationTest extends AbstractIntegrationTest {

    private static final String USERS = "/api/v1/users";
    private static final String PASSWORD = "secret-password";

    @Autowired
    private UserRepository users;

    @Test
    void onlyAdminCanCreateUsersAndReadTheList() {
        String admin = token("admin@gymbro.local", "admin12345");
        String userLocation = create(admin, "member@roles.test");
        String member = token("member@roles.test", PASSWORD);

        as(member).post().uri(USERS).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body("denied@roles.test"))
                .exchange().expectStatus().isForbidden();
        as(member).get().uri(USERS).exchange().expectStatus().isForbidden();
        assertThat(users.existsByEmail("denied@roles.test")).isFalse();

        User trainerUser = users.findByEmail("member@roles.test").orElseThrow();
        trainerUser.promoteToTrainer();
        users.save(trainerUser);
        String trainer = token("member@roles.test", PASSWORD);
        as(trainer).post().uri(USERS).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body("trainer-denied@roles.test"))
                .exchange().expectStatus().isForbidden();

        as(admin).get().uri(USERS).exchange().expectStatus().isOk();
        as(admin).get().uri(userLocation).exchange().expectStatus().isOk();
    }

    @Test
    void ownerCanReadAndChangeOwnRecordButNotAnotherUsersRecord() {
        String admin = token("admin@gymbro.local", "admin12345");
        String ownLocation = create(admin, "owner@roles.test");
        String otherLocation = create(admin, "other@roles.test");
        String owner = token("owner@roles.test", PASSWORD);
        String otherPasswordHash = users.findByEmail("other@roles.test").orElseThrow().getPassword();

        as(owner).get().uri(ownLocation).exchange().expectStatus().isOk();
        as(owner).get().uri(otherLocation).exchange().expectStatus().isForbidden();
        as(owner).patch().uri(ownLocation).contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"renamed@roles.test\"}")
                .exchange().expectStatus().isOk();
        as(owner).patch().uri(otherLocation).contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"password\":\"changed-password\"}")
                .exchange().expectStatus().isForbidden();
        assertThat(users.findByEmail("other@roles.test").orElseThrow().getPassword())
                .isEqualTo(otherPasswordHash);

        as(admin).patch().uri(otherLocation).contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"admin-changed@roles.test\"}")
                .exchange().expectStatus().isOk();
        assertThat(users.existsByEmail("admin-changed@roles.test")).isTrue();
    }

    @Test
    void ownerCannotDeleteAnotherUserButCanDeleteSelf() {
        String admin = token("admin@gymbro.local", "admin12345");
        String ownLocation = create(admin, "delete-owner@roles.test");
        String otherLocation = create(admin, "delete-other@roles.test");
        String owner = token("delete-owner@roles.test", PASSWORD);

        as(owner).delete().uri(otherLocation).exchange().expectStatus().isForbidden();
        as(owner).delete().uri(ownLocation).exchange().expectStatus().isNoContent();
        as(owner).get().uri(ownLocation).exchange().expectStatus().isUnauthorized();
        as(admin).delete().uri(otherLocation).exchange().expectStatus().isNoContent();
    }

    @Test
    void bannedUserCannotUsePreviouslyIssuedToken() {
        String admin = token("admin@gymbro.local", "admin12345");
        String location = create(admin, "banned@roles.test");
        String member = token("banned@roles.test", PASSWORD);
        User banned = users.findByEmail("banned@roles.test").orElseThrow();
        banned.ban();
        users.save(banned);

        as(member).get().uri(location).exchange().expectStatus().isForbidden();
    }

    private String create(String bearer, String email) {
        return as(bearer).post().uri(USERS).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body(email)).exchange()
                .expectStatus().isCreated()
                .returnResult(String.class).getResponseHeaders().getFirst(HttpHeaders.LOCATION);
    }

    private String token(String email, String password) {
        TokenResponse response = client.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password))
                .exchange().expectStatus().isOk()
                .expectBody(TokenResponse.class).returnResult().getResponseBody();
        return response.accessToken();
    }

    private WebTestClient as(String token) {
        return client.mutate().defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token).build();
    }

    private String body(String email) {
        return "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD);
    }
}
