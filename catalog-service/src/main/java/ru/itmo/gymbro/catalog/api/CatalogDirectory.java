package ru.itmo.gymbro.catalog.api;

import reactor.core.publisher.Mono;

/** Read-only contract for other domains; never exposes persistence entities. */
public interface CatalogDirectory {

    Mono<SportSnapshot> getSport(long id);

    Mono<GymSnapshot> getGym(long id);
}
