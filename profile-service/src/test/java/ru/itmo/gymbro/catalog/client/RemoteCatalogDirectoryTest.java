package ru.itmo.gymbro.catalog.client;

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
import ru.itmo.gymbro.catalog.api.GymSnapshot;
import ru.itmo.gymbro.catalog.api.SportSnapshot;
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

class RemoteCatalogDirectoryTest extends AbstractIntegrationTest {

    private static final WireMockServer CATALOG = new WireMockServer(options().dynamicPort());

    static {
        CATALOG.start();
        warmUp();
    }

    @Autowired
    private RemoteCatalogDirectory directory;

    @DynamicPropertySource
    static void catalogUrl(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.openfeign.client.config.catalog-service.url", CATALOG::baseUrl);
    }

    private static void warmUp() {
        CATALOG.stubFor(get(urlEqualTo("/warm-up")).willReturn(okJson("{}")));
        try (InputStream ignored = URI.create(CATALOG.baseUrl() + "/warm-up").toURL().openStream()) {
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @AfterAll
    static void stopCatalog() {
        CATALOG.stop();
    }

    @Autowired
    private CircuitBreakerRegistry breakers;

    @BeforeEach
    void resetStubs() {
        breakers.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
        CATALOG.resetAll();
    }

    private static ResponseDefinitionBuilder problem(int status) {
        return aResponse().withStatus(status)
                .withHeader("Content-Type", "application/problem+json")
                .withBody("{\"status\":" + status + "}");
    }

    @Test
    @DisplayName("Вид спорта читается из ответа каталога")
    void readsSport() {
        CATALOG.stubFor(get(urlEqualTo("/internal/v1/catalog/sports/3"))
                .willReturn(okJson("{\"id\":3,\"name\":\"Бег\"}")));

        assertThat(directory.getSport(3)).isEqualTo(new SportSnapshot(3, "Бег"));
    }

    @Test
    @DisplayName("Зал читается из ответа каталога")
    void readsGym() {
        CATALOG.stubFor(get(urlEqualTo("/internal/v1/catalog/gyms/7"))
                .willReturn(okJson("{\"id\":7,\"name\":\"Зал\",\"city\":\"Москва\",\"address\":\"Ленина, 1\"}")));

        assertThat(directory.getGym(7)).isEqualTo(new GymSnapshot(7, "Зал", "Москва", "Ленина, 1"));
    }

    @Test
    @DisplayName("404 от каталога — вид спорта или зал не найден")
    void mapsNotFound() {
        CATALOG.stubFor(get(urlEqualTo("/internal/v1/catalog/sports/3")).willReturn(problem(404)));
        CATALOG.stubFor(get(urlEqualTo("/internal/v1/catalog/gyms/7")).willReturn(problem(404)));

        assertThatThrownBy(() -> directory.getSport(3))
                .isInstanceOf(NotFoundException.class).hasMessage("Вид спорта 3 не найден");
        assertThatThrownBy(() -> directory.getGym(7))
                .isInstanceOf(NotFoundException.class).hasMessage("Зал 7 не найден");
    }

    @Test
    @DisplayName("5xx от каталога — сервис каталога недоступен")
    void mapsServerError() {
        CATALOG.stubFor(get(urlEqualTo("/internal/v1/catalog/sports/3")).willReturn(problem(503)));

        assertThatThrownBy(() -> directory.getSport(3))
                .isInstanceOf(DependencyUnavailableException.class)
                .hasMessage("Сервис каталога временно недоступен");
    }

    @Test
    @DisplayName("После серии сбоев автомат размыкается: 503 сразу, без запроса к соседу")
    void opensAfterFailuresAndStopsCalling() {
        CATALOG.stubFor(get(urlEqualTo("/internal/v1/catalog/sports/3")).willReturn(problem(503)));
        for (int call = 0; call < 5; call++) {
            assertThatThrownBy(() -> directory.getSport(3)).isInstanceOf(DependencyUnavailableException.class);
        }

        CATALOG.resetRequests();
        assertThatThrownBy(() -> directory.getSport(3))
                .isInstanceOf(DependencyUnavailableException.class)
                .hasMessageContaining("запросы к нему приостановлены");
        CATALOG.verify(0, getRequestedFor(urlEqualTo("/internal/v1/catalog/sports/3")));
    }
}
