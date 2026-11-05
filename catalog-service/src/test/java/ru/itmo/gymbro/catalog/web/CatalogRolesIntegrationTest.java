package ru.itmo.gymbro.catalog.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.TestTokens;
import ru.itmo.gymbro.catalog.model.Gym;
import ru.itmo.gymbro.catalog.model.GymRequest;
import ru.itmo.gymbro.catalog.model.RequestStatus;
import ru.itmo.gymbro.catalog.repository.GymRepository;
import ru.itmo.gymbro.catalog.repository.GymRequestRepository;
import ru.itmo.gymbro.catalog.repository.SportRepository;
import ru.itmo.gymbro.identity.api.Role;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ролевая модель каталога: справочник меняет и заявки на залы рассматривает только ADMIN,
 * подать заявку может любой пользователь. Тренер в каталоге прав администратора не получает.
 */
class CatalogRolesIntegrationTest extends AbstractIntegrationTest {

    private static final String REQUESTS = "/api/v1/gym-requests";
    private static final String GYM_BODY = "{\"name\":\"Gym\",\"city\":\"%s\",\"address\":\"Address\"}";

    @Autowired
    private GymRequestRepository requests;
    @Autowired
    private GymRepository gyms;
    @Autowired
    private SportRepository sports;

    private long authorId;
    private long requestId;

    @BeforeEach
    void setUp() {
        authorId = testUsers.active();
        requestId = requests.save(GymRequest.submit(authorId, "New gym", "Roles-" + UUID.randomUUID(), "Address"))
                .block().getId();
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "TRAINER"})
    @DisplayName("Не администратор заявки не видит и не рассматривает — 403")
    void onlyAdminReviewsRequests(String role) {
        long userId = userWithRole(Role.valueOf(role));

        for (String operation : new String[] {"approve", "reject"}) {
            as(userId, client.post().uri(REQUESTS + "/" + requestId + "/" + operation)).exchange()
                    .expectStatus().isForbidden();
        }
        as(userId, client.get().uri(REQUESTS)).exchange().expectStatus().isForbidden();
        assertThat(requests.findById(requestId).block().getStatus()).isEqualTo(RequestStatus.PENDING);
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "TRAINER"})
    @DisplayName("Не администратор не меняет справочник видов спорта и залов — 403")
    void onlyAdminChangesCatalog(String role) {
        long userId = userWithRole(Role.valueOf(role));
        long gymId = gyms.save(Gym.of("Original", "Catalog-" + UUID.randomUUID(), "Address")).block().getId();
        long sportsBefore = sports.count().block();

        as(userId, client.post().uri("/api/v1/sports").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"Forbidden sport\"}")).exchange().expectStatus().isForbidden();
        as(userId, client.delete().uri("/api/v1/sports/" + sports.findAll().blockFirst().getId()))
                .exchange().expectStatus().isForbidden();
        as(userId, client.post().uri("/api/v1/gyms").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(GYM_BODY.formatted("Forbidden city"))).exchange().expectStatus().isForbidden();
        as(userId, client.put().uri("/api/v1/gyms/" + gymId).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(GYM_BODY.formatted("Other"))).exchange().expectStatus().isForbidden();
        as(userId, client.delete().uri("/api/v1/gyms/" + gymId)).exchange().expectStatus().isForbidden();

        assertThat(sports.count().block()).isEqualTo(sportsBefore);
        assertThat(gyms.findById(gymId).block().getName()).isEqualTo("Original");
    }

    @Test
    @DisplayName("Администратор создаёт вид спорта и рассматривает заявку")
    void adminChangesCatalogAndReviews() {
        long adminId = testUsers.admin();

        as(adminId, client.post().uri("/api/v1/sports").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"Admin sport\"}")).exchange().expectStatus().isCreated();
        as(adminId, client.post().uri(REQUESTS + "/" + requestId + "/reject")).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.reviewedBy").isEqualTo(adminId);
    }

    @Test
    @DisplayName("Разжалованный администратор теряет права сразу, даже со старым токеном")
    void demotedAdminLosesRightsImmediately() {
        long adminId = testUsers.admin();
        String oldAdminToken = TestTokens.bearer(adminId);
        testUsers.changeRole(adminId, Role.USER);

        client.post().uri(REQUESTS + "/" + requestId + "/approve")
                .header(HttpHeaders.AUTHORIZATION, oldAdminToken)
                .exchange().expectStatus().isForbidden();
        assertThat(requests.findById(requestId).block().getStatus()).isEqualTo(RequestStatus.PENDING);
    }

    @Test
    @DisplayName("Назначенный администратор получает права с новым токеном")
    void promotedUserNeedsNewToken() {
        String oldUserToken = TestTokens.bearer(authorId);
        testUsers.changeRole(authorId, Role.ADMIN);

        client.get().uri(REQUESTS).header(HttpHeaders.AUTHORIZATION, oldUserToken)
                .exchange().expectStatus().isForbidden();
        as(authorId, client.get().uri(REQUESTS)).exchange().expectStatus().isOk();
    }

    private long userWithRole(Role role) {
        long id = testUsers.active();
        testUsers.changeRole(id, role);
        return id;
    }

    private static WebTestClient.RequestHeadersSpec<?> as(long userId, WebTestClient.RequestHeadersSpec<?> request) {
        return request.header(HttpHeaders.AUTHORIZATION, TestTokens.bearer(userId));
    }
}
