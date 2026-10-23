package ru.itmo.gymbro.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayFallbackTest {

    @Value("${local.server.port}")
    private int port;

    private WebTestClient client;

    @BeforeEach
    void setUpClient() {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @ParameterizedTest
    @CsvSource({
            "/api/v1/users/1, пользователей",
            "/api/v1/gyms, каталога",
            "/api/v1/profiles/1, анкет",
            "/api/v1/feed, ленты и мэтчей"})
    @DisplayName("Недоступный сервис — 503 с понятным текстом из fallback своего маршрута")
    void answersFromFallbackWhenServiceIsUnavailable(String path, String service) {
        client.get().uri(path).exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Сервис недоступен")
                .jsonPath("$.detail").isEqualTo("Сервис " + service + " временно недоступен, повторите запрос позже");
    }

    @ParameterizedTest
    @CsvSource({"/internal/v1/users/banned-ids", "/v3/api-docs/unknown"})
    @DisplayName("Внутренние пути наружу не маршрутизируются — 404, а не fallback")
    void doesNotRouteInternalPaths(String path) {
        client.get().uri(path).exchange().expectStatus().isNotFound();
    }
}
