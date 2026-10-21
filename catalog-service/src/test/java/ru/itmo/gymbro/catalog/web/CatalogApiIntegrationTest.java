package ru.itmo.gymbro.catalog.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.catalog.model.Gym;
import ru.itmo.gymbro.catalog.repository.GymRepository;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

class CatalogApiIntegrationTest extends AbstractIntegrationTest {

    private static final String SPORTS = "/api/v1/sports";
    private static final String GYMS = "/api/v1/gyms";
    private static final String GYM = "{\"name\":\"Test gym\",\"city\":\"Moscow\",\"address\":\"Street 1\"}";
    private static final String UPDATED_GYM = "{\"name\":\"Updated gym\",\"city\":\"Kazan\",\"address\":\"Street 2\"}";

    @Autowired
    private GymRepository gyms;

    private long adminId;

    @BeforeEach
    void setUp() {
        adminId = testUsers.admin();
    }

    static Stream<Arguments> catalogs() {
        return Stream.of(
                Arguments.of(SPORTS, "{\"name\":\"Test sport\"}"),
                Arguments.of(GYMS, GYM));
    }

    @Test
    void createsListsAndDeletesSport() {
        long id = create(SPORTS, "{\"name\":\"Test sport\"}");

        String page = client.get().uri(SPORTS + "?size=50").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
        List<Number> ids = JsonPath.read(page, "$[*].id");
        assertThat(ids).extracting(Number::longValue).contains(id);

        delete(SPORTS + "/" + id).expectStatus().isNoContent();
        delete(SPORTS + "/" + id).expectStatus().isNotFound();
    }

    @Test
    void createsReadsUpdatesAndDeletesGym() {
        String location = write(client.post(), GYMS, GYM)
                .expectStatus().isCreated()
                .expectHeader().value("Location", value -> assertThat(value).contains(GYMS + "/"))
                .returnResult(String.class).getResponseHeaders().getFirst("Location");
        client.get().uri(location).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.id").isNumber();

        write(client.put(), location, UPDATED_GYM)
                .expectStatus().isOk()
                .expectBody().jsonPath("$.name").isEqualTo("Updated gym");
        client.get().uri(location).exchange()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Updated gym")
                .jsonPath("$.city").isEqualTo("Kazan")
                .jsonPath("$.address").isEqualTo("Street 2");

        write(client.put(), location, UPDATED_GYM).expectStatus().isOk();

        delete(location).expectStatus().isNoContent();
        client.get().uri(location).exchange().expectStatus().isNotFound();
        delete(location).expectStatus().isNotFound();
    }

    @ParameterizedTest
    @MethodSource("catalogs")
    void rejectsDuplicateCreation(String path, String body) {
        create(path, body);
        write(client.post(), path, body)
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.detail").isNotEmpty();
    }

    @Test
    void rejectsGymUpdateToAnotherRecordsUniqueFields() {
        long id = create(GYMS, GYM);
        create(GYMS, UPDATED_GYM);
        write(client.put(), GYMS + "/" + id, UPDATED_GYM).expectStatus().isEqualTo(409);
        client.get().uri(GYMS + "/" + id).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.name").isEqualTo("Test gym");
    }

    @ParameterizedTest
    @MethodSource("catalogs")
    void rejectsInvalidAndMalformedBodies(String path, String body) {
        write(client.post(), path, "{\"name\":\" \"}")
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.errors").isArray();
        write(client.post(), path, "{").expectStatus().isBadRequest();
        write(client.post(), path, body.replace("Test", "x".repeat(301))).expectStatus().isBadRequest();
    }

    @Test
    void rejectsInvalidGymUpdate() {
        long id = create(GYMS, GYM);
        write(client.put(), GYMS + "/" + id, "{}").expectStatus().isBadRequest();
    }

    @ParameterizedTest
    @MethodSource("catalogs")
    void handlesMissingRecordsAndInvalidParameters(String path, String body) {
        delete(path + "/9223372036854775807").expectStatus().isNotFound();
        delete(path + "/-1").expectStatus().isBadRequest();
        delete(path + "/abc").expectStatus().isBadRequest();
        client.get().uri(path + "?sort=unknown,asc").exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.detail").value(containsString("unknown"));
    }

    @Test
    void handlesMissingGymOnReadAndUpdate() {
        String missing = GYMS + "/9223372036854775807";
        client.get().uri(missing).exchange().expectStatus().isNotFound();
        write(client.put(), missing, GYM).expectStatus().isNotFound();
        client.get().uri(GYMS + "/-1").exchange().expectStatus().isBadRequest();
        client.get().uri(GYMS + "/abc").exchange().expectStatus().isBadRequest();
    }

    @Test
    void paginatesGymsWithTotalCountAndCapsSize() {
        gyms.saveAll(Flux.range(0, 55).map(i -> Gym.of("Same name", "Test city", "Address " + i))).blockLast();

        expectPage(GYMS, 20);
        expectPage(GYMS + "?size=1000&sort=name,asc", 50);
        expectPage(GYMS + "?size=50&page=1", 5);
        expectPage(GYMS + "?page=100", 0);
    }

    @Test
    void listsSeededSportsWithTotalCount() {
        client.get().uri(SPORTS + "?size=3").exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Total-Count", "10")
                .expectBody().jsonPath("$.length()").isEqualTo(3);
    }

    @Test
    void normalizesWhitespaceBeforeCheckingDuplicates() {
        create(SPORTS, "{\"name\":\"  New sport  \"}");
        write(client.post(), SPORTS, "{\"name\":\"New sport\"}").expectStatus().isEqualTo(409);
        create(GYMS, "{\"name\":\" Gym \",\"city\":\" City \",\"address\":\" Address \"}");
        write(client.post(), GYMS, "{\"name\":\"Another gym\",\"city\":\"City\",\"address\":\"Address\"}")
                .expectStatus().isEqualTo(409);
    }

    private void expectPage(String uri, int size) {
        client.get().uri(uri).exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Total-Count", "55")
                .expectBody().jsonPath("$.length()").isEqualTo(size);
    }

    private WebTestClient.ResponseSpec write(WebTestClient.RequestBodyUriSpec method, String uri, String body) {
        return method.uri(uri).header("X-User-Id", String.valueOf(adminId))
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange();
    }

    private WebTestClient.ResponseSpec delete(String uri) {
        return client.delete().uri(uri).header("X-User-Id", String.valueOf(adminId)).exchange();
    }

    private long create(String path, String body) {
        String json = write(client.post(), path, body)
                .expectStatus().isCreated()
                .expectBody(String.class).returnResult().getResponseBody();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
