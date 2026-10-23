package ru.itmo.gymbro.shared.api;

import reactor.core.publisher.Mono;

public interface CurrentUserProvider {

    public Mono<Long> currentUserId();
}
