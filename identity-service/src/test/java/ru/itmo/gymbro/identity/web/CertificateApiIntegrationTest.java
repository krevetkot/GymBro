package ru.itmo.gymbro.identity.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.identity.dto.TokenResponse;
import ru.itmo.gymbro.identity.model.CertificateStatus;
import ru.itmo.gymbro.identity.model.Role;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.CertificateRepository;
import ru.itmo.gymbro.identity.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Сценарии «Приложить сертификат» (USER/TRAINER) и «Верифицировать сертификат тренера» (ADMIN).
 */
class CertificateApiIntegrationTest extends AbstractIntegrationTest {

    private static final String CERTIFICATES = "/api/v1/certificates";
    private static final String PASSWORD = "secret-password";

    @Autowired
    private UserRepository users;
    @Autowired
    private CertificateRepository certificates;
    @Autowired
    private ReactiveJwtDecoder decoder;

    private String admin;
    private String applicant;

    @BeforeEach
    void setUp() {
        admin = token("admin@gymbro.local", "admin12345");
        applicant = createAndLogin("applicant@cert.test");
    }

    @Test
    @DisplayName("Пользователь прикладывает сертификат, администратор одобряет — пользователь становится тренером")
    void approvalMakesUserTrainer() {
        submit(applicant, 3)
                .expectStatus().isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION)
                .expectBody()
                .jsonPath("$.status").isEqualTo("PENDING")
                .jsonPath("$.sportId").isEqualTo(3)
                .jsonPath("$.trainerId").isEqualTo(userId("applicant@cert.test"))
                .jsonPath("$.reviewedBy").doesNotExist();
        long id = onlyCertificateId();

        as(applicant).get().uri(CERTIFICATES + "/me").exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Total-Count", "1")
                .expectBody().jsonPath("$[0].id").isEqualTo(id);
        as(admin).get().uri(CERTIFICATES + "?status=PENDING").exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Total-Count", "1");

        as(admin).post().uri(CERTIFICATES + "/" + id + "/approve").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("APPROVED")
                .jsonPath("$.reviewedBy").isEqualTo(userId("admin@gymbro.local"));

        assertThat(users.findByEmail("applicant@cert.test").orElseThrow().getRole()).isEqualTo(Role.TRAINER);
        assertThat(roleClaim(applicant)).isEqualTo("USER");
        assertThat(roleClaim(token("applicant@cert.test", PASSWORD))).isEqualTo("TRAINER");
        as(admin).get().uri(CERTIFICATES + "?status=PENDING").exchange()
                .expectHeader().valueEquals("X-Total-Count", "0");
    }

    @Test
    @DisplayName("Тренер прикладывает сертификат ещё по одному виду спорта; роль не меняется")
    void trainerAttachesAnotherCertificate() {
        submit(applicant, 3).expectStatus().isCreated();
        as(admin).post().uri(CERTIFICATES + "/" + onlyCertificateId() + "/approve").exchange().expectStatus().isOk();
        String trainer = token("applicant@cert.test", PASSWORD);

        submit(trainer, 4).expectStatus().isCreated();
        long second = certificates.findAllByStatus(CertificateStatus.PENDING, PageRequest.of(0, 1))
                .getContent().getFirst().getId();
        as(admin).post().uri(CERTIFICATES + "/" + second + "/approve").exchange().expectStatus().isOk();

        assertThat(users.findByEmail("applicant@cert.test").orElseThrow().getRole()).isEqualTo(Role.TRAINER);
        as(trainer).get().uri(CERTIFICATES + "/me").exchange().expectHeader().valueEquals("X-Total-Count", "2");
    }

    @Test
    @DisplayName("Отклонённый сертификат не меняет роль, повторное рассмотрение — 409")
    void rejectionKeepsRole() {
        submit(applicant, 3).expectStatus().isCreated();
        long id = onlyCertificateId();

        as(admin).post().uri(CERTIFICATES + "/" + id + "/reject").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("REJECTED");
        as(admin).post().uri(CERTIFICATES + "/" + id + "/approve").exchange().expectStatus().isEqualTo(409);
        as(admin).post().uri(CERTIFICATES + "/" + id + "/reject").exchange().expectStatus().isEqualTo(409);

        assertThat(users.findByEmail("applicant@cert.test").orElseThrow().getRole()).isEqualTo(Role.USER);
        submit(applicant, 3).expectStatus().isCreated();
    }

    @Test
    @DisplayName("Второй сертификат по тому же виду спорта, пока первый на проверке, — 409")
    void onePendingCertificatePerSport() {
        submit(applicant, 3).expectStatus().isCreated();
        submit(applicant, 3).expectStatus().isEqualTo(409);
        submit(applicant, 4).expectStatus().isCreated();
    }

    @Test
    @DisplayName("Пользователь не видит чужие сертификаты и не рассматривает их — 403")
    void onlyAdminVerifies() {
        submit(applicant, 3).expectStatus().isCreated();
        long id = onlyCertificateId();
        String other = createAndLogin("other@cert.test");

        as(other).get().uri(CERTIFICATES).exchange().expectStatus().isForbidden();
        as(other).post().uri(CERTIFICATES + "/" + id + "/approve").exchange().expectStatus().isForbidden();
        as(applicant).post().uri(CERTIFICATES + "/" + id + "/approve").exchange().expectStatus().isForbidden();
        as(other).get().uri(CERTIFICATES + "/me").exchange().expectHeader().valueEquals("X-Total-Count", "0");

        assertThat(certificates.findById(id).orElseThrow().getStatus()).isEqualTo(CertificateStatus.PENDING);
        assertThat(users.findByEmail("applicant@cert.test").orElseThrow().getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("Администратору сертификат не нужен — 403")
    void adminCannotSubmit() {
        submit(admin, 3).expectStatus().isForbidden();
        assertThat(certificates.count()).isZero();
    }

    @Test
    @DisplayName("Сертификат заблокированного автора не одобряется — 409, роль не меняется")
    void bannedAuthorIsNotPromoted() {
        submit(applicant, 3).expectStatus().isCreated();
        long id = onlyCertificateId();
        User author = users.findByEmail("applicant@cert.test").orElseThrow();
        author.ban();
        users.save(author);

        as(admin).post().uri(CERTIFICATES + "/" + id + "/approve").exchange().expectStatus().isEqualTo(409);

        assertThat(certificates.findById(id).orElseThrow().getStatus()).isEqualTo(CertificateStatus.PENDING);
        assertThat(users.findByEmail("applicant@cert.test").orElseThrow().getRole()).isEqualTo(Role.USER);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"sportId\":3,\"fileUrl\":\"not-a-link\"}",
            "{\"sportId\":0,\"fileUrl\":\"https://files.test/a.pdf\"}",
            "{\"fileUrl\":\"https://files.test/a.pdf\"}",
            "{\"sportId\":3}"})
    @DisplayName("Некорректный сертификат — 400")
    void validatesSubmission(String body) {
        as(applicant).post().uri(CERTIFICATES).contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                .exchange().expectStatus().isBadRequest();
        assertThat(certificates.count()).isZero();
    }

    @Test
    @DisplayName("Несуществующий сертификат — 404, без токена — 401")
    void missingCertificateAndMissingToken() {
        as(admin).post().uri(CERTIFICATES + "/" + Long.MAX_VALUE + "/approve").exchange().expectStatus().isNotFound();
        as(admin).post().uri(CERTIFICATES + "/" + Long.MAX_VALUE + "/reject").exchange().expectStatus().isNotFound();
        client.post().uri(CERTIFICATES).contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"sportId\":3,\"fileUrl\":\"https://files.test/a.pdf\"}")
                .exchange().expectStatus().isUnauthorized();
        client.get().uri(CERTIFICATES + "/me").exchange().expectStatus().isUnauthorized();
    }

    private WebTestClient.ResponseSpec submit(String bearer, long sportId) {
        return as(bearer).post().uri(CERTIFICATES).contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"sportId\":%d,\"fileUrl\":\"https://files.test/diploma-%d.pdf\"}".formatted(sportId, sportId))
                .exchange();
    }

    private long onlyCertificateId() {
        return certificates.findAll().getFirst().getId();
    }

    private long userId(String email) {
        return users.findByEmail(email).orElseThrow().getId();
    }

    private String createAndLogin(String email) {
        as(admin).post().uri("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD))
                .exchange().expectStatus().isCreated();
        return token(email, PASSWORD);
    }

    private String roleClaim(String token) {
        return decoder.decode(token).block().getClaimAsString("role");
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
}
