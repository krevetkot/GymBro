package ru.itmo.gymbro.catalog.api;

import reactor.core.publisher.Mono;

public interface CatalogDirectory {

    Mono<SportSnapshot> getSport(long id);

    Mono<GymSnapshot> getGym(long id);
}
