package ru.itmo.gymbro.identity.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.identity.api.UserSnapshot;
import ru.itmo.gymbro.shared.api.DependencyUnavailableException;
import ru.itmo.gymbro.shared.api.NotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RemoteUserDirectoryTest extends AbstractIntegrationTest {

    private static final WireMockServer IDENTITY = new WireMockServer(options().dynamicPort());

    static {
        IDENTITY.start();
        warmUp();
    }

    @Autowired
    private RemoteUserDirectory directory;

    @DynamicPropertySource
    static void identityUrl(DynamicPropertyRegistry registry) {
        registry.add("gymbro.clients.identity-url", IDENTITY::baseUrl);
        registry.add("reactive.feign.client.config.identity-service.options.readTimeoutMillis", () -> 1_000);
    }

    private static void warmUp() {
        IDENTITY.stubFor(get(urlEqualTo("/warm-up")).willReturn(okJson("{}")));
        try (InputStream ignored = URI.create(IDENTITY.baseUrl() + "/warm-up").toURL().openStream()) {
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @AfterAll
    static void stopIdentity() {
        IDENTITY.stop();
    }

    @Autowired
    private CircuitBreakerRegistry breakers;

    @BeforeEach
    void resetStubs() {
        breakers.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
        IDENTITY.resetAll();
    }

    private static ResponseDefinitionBuilder problem(int status) {
        return aResponse().withStatus(status)
                .withHeader("Content-Type", "application/problem+json")
                .withBody("{\"status\":" + status + "}");
    }

    @Test
    @DisplayName("Снапшот пользователя читается из ответа identity")
    void readsSnapshot() {
        IDENTITY.stubFor(get(urlEqualTo("/internal/v1/users/7"))
                .willReturn(okJson("{\"id\":7,\"active\":true,\"admin\":true}")));

        assertThat(directory.getSnapshot(7).block()).isEqualTo(new UserSnapshot(7, true, true));
    }

    @Test
    @DisplayName("Список недоступных пользователей читается из ответа identity")
    void readsBannedIds() {
        IDENTITY.stubFor(get(urlEqualTo("/internal/v1/users/banned-ids")).willReturn(okJson("[3,5]")));

        assertThat(directory.bannedUserIds().block()).containsExactlyInAnyOrder(3L, 5L);
    }

    @Test
    @DisplayName("404 от identity — пользователь не найден")
    void mapsNotFound() {
        IDENTITY.stubFor(get(urlEqualTo("/internal/v1/users/7")).willReturn(problem(404)));

        assertThatThrownBy(() -> directory.getSnapshot(7).block()).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("5xx от identity — сервис недоступен")
    void mapsServerError() {
        IDENTITY.stubFor(get(urlEqualTo("/internal/v1/users/7")).willReturn(problem(500)));

        assertThatThrownBy(() -> directory.getSnapshot(7).block()).isInstanceOf(DependencyUnavailableException.class);
    }

    @Test
    @DisplayName("identity не ответил за таймаут — сервис недоступен")
    void mapsTimeout() {
        IDENTITY.stubFor(get(urlEqualTo("/internal/v1/users/7"))
                .willReturn(okJson("{\"id\":7,\"active\":true,\"admin\":false}").withFixedDelay(3_000)));

        assertThatThrownBy(() -> directory.getSnapshot(7).block()).isInstanceOf(DependencyUnavailableException.class);
    }

    @Test
    @DisplayName("Пустой ответ identity — сервис недоступен")
    void mapsEmptyBody() {
        IDENTITY.stubFor(get(urlEqualTo("/internal/v1/users/7")).willReturn(aResponse().withStatus(200)));

        assertThatThrownBy(() -> directory.getSnapshot(7).block()).isInstanceOf(DependencyUnavailableException.class);
    }

    @Test
    @DisplayName("После серии сбоев автомат размыкается: 503 сразу, без запроса к соседу")
    void opensAfterFailuresAndStopsCalling() {
        IDENTITY.stubFor(get(urlEqualTo("/internal/v1/users/7")).willReturn(problem(503)));
        for (int call = 0; call < 5; call++) {
            assertThatThrownBy(() -> directory.getSnapshot(7).block()).isInstanceOf(DependencyUnavailableException.class);
        }

        IDENTITY.resetRequests();
        assertThatThrownBy(() -> directory.getSnapshot(7).block())
                .isInstanceOf(DependencyUnavailableException.class)
                .hasMessageContaining("запросы к нему приостановлены");
        IDENTITY.verify(0, getRequestedFor(urlEqualTo("/internal/v1/users/7")));
    }
}
