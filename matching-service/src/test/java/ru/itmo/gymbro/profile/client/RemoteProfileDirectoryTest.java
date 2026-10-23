package ru.itmo.gymbro.profile.client;

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
import ru.itmo.gymbro.profile.api.FeedCandidates;
import ru.itmo.gymbro.profile.api.ProfileSnapshot;
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

class RemoteProfileDirectoryTest extends AbstractIntegrationTest {

    private static final String FEED = "/internal/v1/profiles/feed";
    private static final String EMPTY_PAGE = "{\"items\":[],\"page\":0,\"size\":20,\"hasNext\":false}";
    private static final WireMockServer PROFILES = new WireMockServer(options().dynamicPort());

    static {
        PROFILES.start();
        warmUp();
    }

    @Autowired
    private RemoteProfileDirectory directory;

    @Autowired
    private CircuitBreakerRegistry breakers;

    @DynamicPropertySource
    static void profileUrl(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.openfeign.client.config.profile-service.url", PROFILES::baseUrl);
        registry.add("resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state", () -> "1s");
    }

    private static void warmUp() {
        PROFILES.stubFor(get(urlEqualTo("/warm-up")).willReturn(okJson("{}")));
        try (InputStream ignored = URI.create(PROFILES.baseUrl() + "/warm-up").toURL().openStream()) {
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @AfterAll
    static void stopProfiles() {
        PROFILES.stop();
    }

    @BeforeEach
    void resetStubsAndBreakers() {
        breakers.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
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
    @DisplayName("5xx от сервиса анкет — fallback отдаёт пустую ленту вместо ошибки")
    void fallsBackToEmptyFeedOnServerError() {
        PROFILES.stubFor(get(urlPathEqualTo(FEED)).willReturn(problem(500)));

        assertThat(directory.findFeedCandidates(7, Set.of(), 2, 20))
                .isEqualTo(new FeedCandidates(List.of(), 2, 20, false));
    }

    @Test
    @DisplayName("Сервис анкет не ответил вовремя — fallback отдаёт пустую ленту")
    void fallsBackToEmptyFeedOnTimeout() {
        PROFILES.stubFor(get(urlPathEqualTo(FEED)).willReturn(okJson(EMPTY_PAGE).withFixedDelay(5_000)));

        assertThat(directory.findFeedCandidates(7, Set.of(), 0, 20).items()).isEmpty();
    }

    @Test
    @DisplayName("После серии сбоев автомат размыкается: запросы к сервису анкет больше не уходят")
    void opensAfterFailuresAndStopsCallingProfiles() {
        PROFILES.stubFor(get(urlPathEqualTo(FEED)).willReturn(problem(503)));

        for (int call = 0; call < 5; call++) {
            directory.findFeedCandidates(7, Set.of(), 0, 20);
        }
        assertThat(feedBreaker().getState()).isEqualTo(CircuitBreaker.State.OPEN);

        PROFILES.resetRequests();
        assertThat(directory.findFeedCandidates(7, Set.of(), 0, 20).items()).isEmpty();
        PROFILES.verify(0, getRequestedFor(urlPathEqualTo(FEED)));
    }

    @Test
    @DisplayName("Ответы 404 — не сбой сервиса анкет: автомат остаётся замкнутым")
    void clientErrorsDoNotOpenBreaker() {
        PROFILES.stubFor(get(urlPathEqualTo(FEED)).willReturn(problem(404)));

        for (int call = 0; call < 10; call++) {
            assertThatThrownBy(() -> directory.findFeedCandidates(7, Set.of(), 0, 20))
                    .isInstanceOf(NotFoundException.class);
        }
        assertThat(feedBreaker().getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("Сервис анкет поднялся: после паузы автомат пропускает пробные запросы и замыкается")
    void closesAgainWhenProfilesRecover() throws InterruptedException {
        PROFILES.stubFor(get(urlPathEqualTo(FEED)).willReturn(problem(503)));
        for (int call = 0; call < 5; call++) {
            directory.findFeedCandidates(7, Set.of(), 0, 20);
        }
        assertThat(feedBreaker().getState()).isEqualTo(CircuitBreaker.State.OPEN);

        PROFILES.stubFor(get(urlPathEqualTo(FEED)).willReturn(okJson(EMPTY_PAGE)));
        Thread.sleep(1_500);
        assertThat(feedBreaker().getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        directory.findFeedCandidates(7, Set.of(), 0, 20);
        directory.findFeedCandidates(7, Set.of(), 0, 20);
        assertThat(feedBreaker().getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    private CircuitBreaker feedBreaker() {
        return breakers.getAllCircuitBreakers().stream()
                .filter(breaker -> breaker.getName().startsWith("ProfileClient"))
                .findFirst()
                .orElseThrow();
    }
}
