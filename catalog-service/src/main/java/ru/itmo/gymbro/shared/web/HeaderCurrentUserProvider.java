package ru.itmo.gymbro.shared.web;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.reactive.ServerWebExchangeContextFilter;
import reactor.core.publisher.Mono;
import ru.itmo.gymbro.shared.api.CurrentUserProvider;
import ru.itmo.gymbro.shared.api.UnauthorizedException;

import java.util.regex.Pattern;

@Component
class HeaderCurrentUserProvider implements CurrentUserProvider {

    static final String USER_ID_HEADER = "X-User-Id";

    private static final Pattern POSITIVE_ID = Pattern.compile("[1-9]\\d{0,17}");

    @Override
    public Mono<Long> currentUserId() {
        return Mono.deferContextual(context -> ServerWebExchangeContextFilter.getExchange(context)
                .map(exchange -> parse(exchange.getRequest().getHeaders().getFirst(USER_ID_HEADER)))
                .orElseGet(() -> Mono.error(new IllegalStateException(
                        "Текущий пользователь определяется только внутри HTTP-запроса"))));
    }

    static Mono<Long> parse(String header) {
        if (header == null || header.isBlank()) {
            return Mono.error(new UnauthorizedException("Не передан заголовок " + USER_ID_HEADER));
        }
        String value = header.trim();
        if (!POSITIVE_ID.matcher(value).matches()) {
            return Mono.error(new UnauthorizedException(
                    "Заголовок " + USER_ID_HEADER + " должен содержать положительный идентификатор пользователя"));
        }
        return Mono.just(Long.parseLong(value));
    }
}
