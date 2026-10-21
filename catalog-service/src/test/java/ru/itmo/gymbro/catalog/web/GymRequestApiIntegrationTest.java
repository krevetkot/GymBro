package ru.itmo.gymbro.catalog.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.catalog.model.Gym;
import ru.itmo.gymbro.catalog.model.GymRequest;
import ru.itmo.gymbro.catalog.model.RequestStatus;
import ru.itmo.gymbro.catalog.repository.GymRepository;
import ru.itmo.gymbro.catalog.repository.GymRequestRepository;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class GymRequestApiIntegrationTest extends AbstractIntegrationTest {

    private static final String PATH = "/api/v1/gym-requests";
    private static final String BODY = "{\"name\":\" New gym \",\"city\":\" City \",\"address\":\" Address \"}";

    @Autowired
    private GymRequestRepository requests;
    @Autowired
    private GymRepository gyms;

    private long authorId;
    private long adminId;

    @BeforeEach
    void setUp() {
        authorId = testUsers.active();
        adminId = testUsers.admin();
    }

    @Test
    void submitsPendingRequestAndPersistsStringStatusWithoutCreatingGym() {
        submit(String.valueOf(authorId), BODY)
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNumber()
                .jsonPath("$.authorId").isEqualTo(authorId)
                .jsonPath("$.name").isEqualTo("New gym")
                .jsonPath("$.city").isEqualTo("City")
                .jsonPath("$.address").isEqualTo("Address")
                .jsonPath("$.status").isEqualTo("PENDING")
                .jsonPath("$.createdAt").isNotEmpty()
                .jsonPath("$.gymId").doesNotExist()
                .jsonPath("$.reviewedBy").doesNotExist();

        assertThat(requests.findAll().collectList().block())
                .singleElement()
                .satisfies(saved -> assertThat(saved.getAuthorId()).isEqualTo(authorId));
        assertThat(database.sql("SELECT status FROM gym_requests")
                .map(row -> row.get("status", String.class)).one().block()).isEqualTo("PENDING");
        assertThat(gyms.count().block()).isZero();
    }

    @Test
    void requiresCurrentUserForBothEndpoints() {
        client.post().uri(PATH).contentType(MediaType.APPLICATION_JSON).bodyValue(BODY).exchange()
                .expectStatus().isUnauthorized();
        client.get().uri(PATH).exchange().expectStatus().isUnauthorized();
        assertThat(requests.count().block()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "-1", "0", "999999999999999999"})
    void rejectsInvalidOrUnknownCurrentUser(String userId) {
        submit(userId, BODY).expectStatus().isUnauthorized();
        list(userId, "").expectStatus().isUnauthorized();
    }

    @Test
    void deniesListingToOrdinaryUser() {
        requests.save(GymRequest.submit(authorId, "Gym", "City", "Address")).block();
        list(String.valueOf(authorId), "").expectStatus().isForbidden();
    }

    @Test
    void deniesBannedUserAndBannedAdmin() {
        testUsers.ban(authorId);
        testUsers.ban(adminId);

        submit(String.valueOf(authorId), BODY).expectStatus().isForbidden();
        list(String.valueOf(adminId), "").expectStatus().isForbidden();
        assertThat(requests.count().block()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{", "{\"name\":\"Gym\",\"city\":\" \",\"address\":\"Address\"}"})
    void rejectsInvalidSubmission(String body) {
        submit(String.valueOf(authorId), body).expectStatus().isBadRequest();
        assertThat(requests.count().block()).isZero();
    }

    @Test
    void rejectsOversizedFields() {
        submit(String.valueOf(authorId), BODY.replace("New gym", "x".repeat(201))).expectStatus().isBadRequest();
    }

    @Test
    void rejectsRequestForExistingGym() {
        gyms.save(Gym.of("Existing gym", "City", "Address")).block();

        submit(String.valueOf(authorId), BODY)
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.detail").isNotEmpty();
        assertThat(requests.count().block()).isZero();
    }

    @Test
    void paginatesForAdminWithTotalCountAndDeterministicNewestFirstOrder() {
        Instant sameTime = Instant.parse("2026-01-01T00:00:00Z");
        long lastId = requests.saveAll(Flux.range(0, 55).map(i -> new GymRequest(null, authorId, "Gym", "City",
                        "Address " + i, RequestStatus.PENDING, null, null, sameTime)))
                .blockLast().getId();
        String admin = String.valueOf(adminId);

        list(admin, "")
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Total-Count", "55")
                .expectBody()
                .jsonPath("$.length()").isEqualTo(20)
                .jsonPath("$[0].id").isEqualTo(lastId);
        expectPage(admin, "?size=1000", 50);
        expectPage(admin, "?size=50&page=1", 5);
        expectPage(admin, "?page=100", 0);
    }

    @Test
    void returnsEmptyListAndRejectsUnknownSortForAdmin() {
        list(String.valueOf(adminId), "")
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Total-Count", "0")
                .expectBody().jsonPath("$.length()").isEqualTo(0);
        list(String.valueOf(adminId), "?sort=unknown,asc").expectStatus().isBadRequest();
    }

    private void expectPage(String userId, String query, int size) {
        list(userId, query)
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Total-Count", "55")
                .expectBody().jsonPath("$.length()").isEqualTo(size);
    }

    private WebTestClient.ResponseSpec submit(String userId, String body) {
        return client.post().uri(PATH).header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange();
    }

    private WebTestClient.ResponseSpec list(String userId, String query) {
        return client.get().uri(PATH + query).header("X-User-Id", userId).exchange();
    }
}
