package ru.itmo.gymbro.shared.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.filter.reactive.ServerWebExchangeContextFilter;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.itmo.gymbro.shared.api.UnauthorizedException;

import static org.assertj.core.api.Assertions.assertThat;

class HeaderCurrentUserProviderTest {

    private final HeaderCurrentUserProvider provider = new HeaderCurrentUserProvider();

    @Test
    @DisplayName("Идентификатор берётся из заголовка X-User-Id запроса в контексте Reactor")
    void readsUserIdFromHeader() {
        StepVerifier.create(withRequestHeader(" 42 ")).expectNext(42L).verifyComplete();
    }

    @Test
    @DisplayName("Без заголовка пользователь не аутентифицирован")
    void rejectsMissingHeader() {
        StepVerifier.create(withRequestHeader(null))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(UnauthorizedException.class)
                        .hasMessageContaining("X-User-Id"))
                .verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "0", "-5", "1.5", "99999999999999999999"})
    @DisplayName("Заголовок не с положительным числом отклоняется")
    void rejectsMalformedHeader(String header) {
        StepVerifier.create(withRequestHeader(header)).verifyError(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Вне HTTP-запроса текущего пользователя нет")
    void failsOutsideOfRequest() {
        StepVerifier.create(provider.currentUserId()).verifyError(IllegalStateException.class);
    }

    private Mono<Long> withRequestHeader(String value) {
        MockServerHttpRequest.BaseBuilder<?> request = MockServerHttpRequest.get("/");
        if (value != null) {
            request.header("X-User-Id", value);
        }
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        return provider.currentUserId()
                .contextWrite(context -> context.put(ServerWebExchangeContextFilter.EXCHANGE_CONTEXT_ATTRIBUTE, exchange));
    }
}
