package ru.itmo.gymbro.shared.api;

import reactor.core.publisher.Mono;

public interface CurrentUserAccess {

    public Mono<Long> requireActiveUser();

    public Mono<Long> requireAdmin();
}
