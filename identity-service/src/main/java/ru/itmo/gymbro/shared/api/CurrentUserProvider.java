package ru.itmo.gymbro.shared.api;

import reactor.core.publisher.Mono;

public interface CurrentUserProvider {

    /** Идентификатор текущего пользователя из запроса; ошибка {@link UnauthorizedException}, если его нет. */
    public Mono<Long> currentUserId();
}
