package ru.itmo.gymbro.identity.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.identity.dto.TokenResponse;
import ru.itmo.gymbro.identity.model.Role;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

class UserAuthorizationIntegrationTest extends AbstractIntegrationTest {

    private static final String USERS = "/api/v1/users";
    private static final String PASSWORD = "secret-password";

    @Autowired
    private UserRepository users;

    @Autowired
    private ReactiveJwtDecoder decoder;

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
        trainerUser.changeRole(Role.TRAINER);
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

    @Test
    void adminCreatesUserWithRequestedRoleAndTokenCarriesIt() {
        String admin = token("admin@gymbro.local", "admin12345");

        as(admin).post().uri(USERS).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body("boss@roles.test", "ADMIN"))
                .exchange().expectStatus().isCreated()
                .expectBody().jsonPath("$.role").isEqualTo("ADMIN").jsonPath("$.password").doesNotExist();
        create(admin, "plain@roles.test");

        assertThat(users.findByEmail("boss@roles.test").orElseThrow().getRole()).isEqualTo(Role.ADMIN);
        assertThat(users.findByEmail("plain@roles.test").orElseThrow().getRole()).isEqualTo(Role.USER);
        assertThat(roleClaim(token("boss@roles.test", PASSWORD))).isEqualTo("ADMIN");
        assertThat(roleClaim(token("plain@roles.test", PASSWORD))).isEqualTo("USER");
    }

    @Test
    void trainerRoleIsNotAssignedDirectly() {
        String admin = token("admin@gymbro.local", "admin12345");
        String location = create(admin, "not-a-coach@roles.test");

        as(admin).post().uri(USERS).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body("coach@roles.test", "TRAINER"))
                .exchange().expectStatus().isBadRequest()
                .expectBody().jsonPath("$.detail").isEqualTo("Роль TRAINER выдаётся только через одобренный сертификат");
        changeRole(admin, location, "TRAINER").expectStatus().isBadRequest();

        assertThat(users.existsByEmail("coach@roles.test")).isFalse();
        assertThat(users.findByEmail("not-a-coach@roles.test").orElseThrow().getRole()).isEqualTo(Role.USER);
    }

    @Test
    void adminRevokesTrainerRole() {
        String admin = token("admin@gymbro.local", "admin12345");
        String location = create(admin, "former-coach@roles.test");
        User coach = users.findByEmail("former-coach@roles.test").orElseThrow();
        coach.changeRole(Role.TRAINER);
        users.save(coach);

        changeRole(admin, location, "USER").expectStatus().isOk()
                .expectBody().jsonPath("$.role").isEqualTo("USER");
        assertThat(users.findByEmail("former-coach@roles.test").orElseThrow().getRole()).isEqualTo(Role.USER);
    }

    @Test
    void adminAssignsRoleAndNewRightsComeWithNewToken() {
        String admin = token("admin@gymbro.local", "admin12345");
        String location = create(admin, "promoted@roles.test");
        String oldToken = token("promoted@roles.test", PASSWORD);

        changeRole(admin, location, "ADMIN")
                .expectStatus().isOk()
                .expectBody().jsonPath("$.role").isEqualTo("ADMIN");
        assertThat(users.findByEmail("promoted@roles.test").orElseThrow().getRole()).isEqualTo(Role.ADMIN);

        as(oldToken).get().uri(USERS).exchange().expectStatus().isForbidden();
        as(token("promoted@roles.test", PASSWORD)).get().uri(USERS).exchange().expectStatus().isOk();
    }

    @Test
    void demotedAdminLosesRightsImmediatelyEvenWithOldToken() {
        String admin = token("admin@gymbro.local", "admin12345");
        as(admin).post().uri(USERS).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body("second-admin@roles.test", "ADMIN"))
                .exchange().expectStatus().isCreated();
        String secondAdmin = token("second-admin@roles.test", PASSWORD);
        String location = USERS + "/" + users.findByEmail("second-admin@roles.test").orElseThrow().getId();
        as(secondAdmin).get().uri(USERS).exchange().expectStatus().isOk();

        changeRole(admin, location, "USER").expectStatus().isOk();

        assertThat(roleClaim(secondAdmin)).isEqualTo("ADMIN");
        as(secondAdmin).get().uri(USERS).exchange().expectStatus().isForbidden();
        as(secondAdmin).post().uri(USERS).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body("never@roles.test")).exchange().expectStatus().isForbidden();
        assertThat(users.existsByEmail("never@roles.test")).isFalse();
    }

    @Test
    void onlyAdminChangesRolesAndNotOwnRole() {
        String admin = token("admin@gymbro.local", "admin12345");
        String location = create(admin, "climber@roles.test");
        String member = token("climber@roles.test", PASSWORD);
        String adminLocation = USERS + "/" + users.findByEmail("admin@gymbro.local").orElseThrow().getId();

        changeRole(member, location, "ADMIN").expectStatus().isForbidden();
        changeRole(admin, adminLocation, "USER").expectStatus().isEqualTo(409);
        changeRole(admin, USERS + "/" + Long.MAX_VALUE, "ADMIN").expectStatus().isNotFound();

        assertThat(users.findByEmail("climber@roles.test").orElseThrow().getRole()).isEqualTo(Role.USER);
        assertThat(users.findByEmail("admin@gymbro.local").orElseThrow().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void validatesRole() {
        String admin = token("admin@gymbro.local", "admin12345");
        String location = create(admin, "validated@roles.test");

        changeRole(admin, location, "KING").expectStatus().isBadRequest();
        as(admin).patch().uri(location + "/role").contentType(MediaType.APPLICATION_JSON).bodyValue("{}")
                .exchange().expectStatus().isBadRequest();
        as(admin).post().uri(USERS).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body("bad-role@roles.test", "KING"))
                .exchange().expectStatus().isBadRequest();
    }

    private WebTestClient.ResponseSpec changeRole(String bearer, String location, String role) {
        return as(bearer).patch().uri(location + "/role").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"role\":\"%s\"}".formatted(role)).exchange();
    }

    private String roleClaim(String token) {
        return decoder.decode(token).block().getClaimAsString("role");
    }

    private String body(String email, String role) {
        return "{\"email\":\"%s\",\"password\":\"%s\",\"role\":\"%s\"}".formatted(email, PASSWORD, role);
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
