package ru.itmo.gymbro.catalog.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.catalog.model.Gym;
import ru.itmo.gymbro.catalog.repository.GymRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GymAdminAccessIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private GymRepository gyms;
    private long gymId;

    @BeforeEach
    void setUp() {
        gymId = gyms.save(Gym.of("Original", "City", "Address")).block().getId();
    }

    @ParameterizedTest
    @ValueSource(strings = {"update", "delete"})
    void requiresAnExistingUser(String operation) {
        change(operation, null).expectStatus().isUnauthorized();
        change(operation, Long.MAX_VALUE).expectStatus().isUnauthorized();
        assertUnchanged();
    }

    @ParameterizedTest
    @ValueSource(strings = {"update", "delete"})
    void rejectsNonAdminsAndBannedAdminsWithoutChangingGym(String operation) {
        long bannedAdmin = testUsers.admin();
        testUsers.ban(bannedAdmin);
        for (long userId : List.of(testUsers.active(), bannedAdmin)) {
            change(operation, userId).expectStatus().isForbidden();
            assertUnchanged();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"update", "delete"})
    void allowsActiveAdmin(String operation) {
        change(operation, testUsers.admin())
                .expectStatus().isEqualTo(operation.equals("update") ? 200 : 204);
        if (operation.equals("update")) {
            assertThat(gyms.findById(gymId).block().getName()).isEqualTo("Updated");
        } else {
            assertThat(gyms.findById(gymId).blockOptional()).isEmpty();
        }
    }

    private WebTestClient.ResponseSpec change(String operation, Long userId) {
        String path = "/api/v1/gyms/" + gymId;
        WebTestClient.RequestHeadersSpec<?> request = operation.equals("delete")
                ? client.delete().uri(path)
                : client.put().uri(path).contentType(MediaType.APPLICATION_JSON).bodyValue("""
                        {"name":"Updated","city":"Other city","address":"Other address"}
                        """);
        if (userId != null) {
            request.header("X-User-Id", String.valueOf(userId));
        }
        return request.exchange();
    }

    private void assertUnchanged() {
        Gym stored = gyms.findById(gymId).block();
        assertThat(stored.getName()).isEqualTo("Original");
        assertThat(stored.getCity()).isEqualTo("City");
        assertThat(stored.getAddress()).isEqualTo("Address");
    }
}
