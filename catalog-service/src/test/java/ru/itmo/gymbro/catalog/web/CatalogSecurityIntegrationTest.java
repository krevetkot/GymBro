package ru.itmo.gymbro.catalog.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.TestTokens;

class CatalogSecurityIntegrationTest extends AbstractIntegrationTest {

    private static final String REQUESTS = "/api/v1/gym-requests";
    private static final String BODY = "{\"name\":\"Gym\",\"city\":\"City\",\"address\":\"Address\"}";

    @Test
    @DisplayName("Справочник видов спорта и залов читается без токена")
    void catalogIsReadableWithoutToken() {
        client.get().uri("/api/v1/sports").exchange().expectStatus().isOk();
        client.get().uri("/api/v1/gyms").exchange().expectStatus().isOk();
    }

    @Test
    @DisplayName("Изменение справочника без токена — 401")
    void catalogWritesRequireToken() {
        client.post().uri("/api/v1/sports").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"Without token\"}")
                .exchange().expectStatus().isUnauthorized();
        client.post().uri("/api/v1/gyms").contentType(MediaType.APPLICATION_JSON).bodyValue(BODY)
                .exchange().expectStatus().isUnauthorized();
        client.delete().uri("/api/v1/sports/1").exchange().expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Без токена — 401 в формате ProblemDetail с WWW-Authenticate")
    void missingTokenIsProblemDetail() {
        client.get().uri(REQUESTS).exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.title").isEqualTo("Требуется аутентификация");
    }

    @Test
    @DisplayName("Заголовок X-User-Id без токена больше не даёт доступа")
    void userIdHeaderIsIgnored() {
        long admin = testUsers.admin();
        client.get().uri(REQUESTS).header("X-User-Id", String.valueOf(admin))
                .exchange().expectStatus().isUnauthorized();
    }

    @ParameterizedTest
    @ValueSource(strings = {"expired", "foreign-signature", "foreign-issuer", "garbage", "basic"})
    @DisplayName("Недействительный токен — 401")
    void rejectsInvalidTokens(String kind) {
        long admin = testUsers.admin();
        String authorization = switch (kind) {
            case "expired" -> TestTokens.expired(admin);
            case "foreign-signature" -> TestTokens.foreignSignature(admin);
            case "foreign-issuer" -> TestTokens.foreignIssuer(admin);
            case "garbage" -> "Bearer not-a-jwt";
            default -> "Basic YWRtaW46YWRtaW4=";
        };
        withToken(client.get().uri(REQUESTS), authorization).exchange().expectStatus().isUnauthorized();
        withToken(client.post().uri(REQUESTS).contentType(MediaType.APPLICATION_JSON).bodyValue(BODY), authorization)
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("С действительным токеном запрос проходит")
    void acceptsValidToken() {
        withToken(client.get().uri(REQUESTS), TestTokens.bearer(testUsers.admin()))
                .exchange().expectStatus().isOk();
    }

    @Test
    @DisplayName("Внутренние эндпоинты доступны без токена")
    void internalEndpointsAreOpen() {
        client.get().uri("/internal/v1/catalog/sports/" + Long.MAX_VALUE).exchange().expectStatus().isNotFound();
    }

    private static WebTestClient.RequestHeadersSpec<?> withToken(
            WebTestClient.RequestHeadersSpec<?> request, String authorization) {
        return request.header(HttpHeaders.AUTHORIZATION, authorization);
    }
}
