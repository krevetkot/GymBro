package ru.itmo.gymbro.profile.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.profile.api.FeedCandidates;
import ru.itmo.gymbro.profile.api.ProfileSnapshot;
import ru.itmo.gymbro.shared.api.DependencyUnavailableException;
import ru.itmo.gymbro.shared.api.NotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.List;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.havingExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Настоящий Feign-клиент против WireMock вместо profile-service:
 * проверяется, как лента запрашивается у сервиса анкет и как переводятся его ошибки.
 */
class RemoteProfileDirectoryTest extends AbstractIntegrationTest {

    private static final String FEED = "/internal/v1/profiles/feed";
    private static final WireMockServer PROFILES = new WireMockServer(options().dynamicPort());

    static {
        PROFILES.start();
        warmUp();
    }

    @Autowired
    private RemoteProfileDirectory directory;

    @DynamicPropertySource
    static void profileUrl(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.openfeign.client.config.profile-service.url", PROFILES::baseUrl);
    }

    /** Первый запрос к только что запущенному WireMock обрабатывается дольше таймаута клиента. */
    private static void warmUp() {
        PROFILES.stubFor(get(urlEqualTo("/warm-up")).willReturn(okJson("{}")));
        try (InputStream ignored = URI.create(PROFILES.baseUrl() + "/warm-up").toURL().openStream()) {
            // ответ не нужен, важно прогреть обработку запросов
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @AfterAll
    static void stopProfiles() {
        PROFILES.stop();
    }

    @BeforeEach
    void resetStubs() {
        PROFILES.resetAll();
    }

    private static ResponseDefinitionBuilder problem(int status) {
        return aResponse().withStatus(status)
                .withHeader("Content-Type", "application/problem+json")
                .withBody("{\"status\":" + status + "}");
    }

    @Test
    @DisplayName("Лента запрашивается с viewer, исключёнными и страницей; ответ разбирается в срез")
    void requestsFeedPage() {
        PROFILES.stubFor(get(urlPathEqualTo(FEED)).willReturn(okJson("""
                {"items":[{"userId":5,"name":"Анна","age":25,"about":null,"sportIds":[1,2],"gymIds":[3]}],
                 "page":1,"size":2,"hasNext":true}
                """)));

        FeedCandidates candidates = directory.findFeedCandidates(7, Set.of(8L, 9L), 1, 2);

        assertThat(candidates).isEqualTo(new FeedCandidates(
                List.of(new ProfileSnapshot(5, "Анна", 25, null, List.of(1L, 2L), List.of(3L))), 1, 2, true));
        PROFILES.verify(getRequestedFor(urlPathEqualTo(FEED))
                .withQueryParam("viewerUserId", equalTo("7"))
                .withQueryParam("excludedUserIds", havingExactly("8", "9"))
                .withQueryParam("page", equalTo("1"))
                .withQueryParam("size", equalTo("2")));
    }

    @Test
    @DisplayName("404 от сервиса анкет — у смотрящего нет анкеты")
    void mapsNotFound() {
        PROFILES.stubFor(get(urlPathEqualTo(FEED)).willReturn(problem(404)));

        assertThatThrownBy(() -> directory.findFeedCandidates(7, Set.of(), 0, 20))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("5xx от сервиса анкет — сервис анкет недоступен")
    void mapsServerError() {
        PROFILES.stubFor(get(urlPathEqualTo(FEED)).willReturn(problem(500)));

        assertThatThrownBy(() -> directory.findFeedCandidates(7, Set.of(), 0, 20))
                .isInstanceOf(DependencyUnavailableException.class)
                .hasMessage("Сервис анкет временно недоступен");
    }
}
