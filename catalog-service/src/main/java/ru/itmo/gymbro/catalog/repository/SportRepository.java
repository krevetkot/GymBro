package ru.itmo.gymbro.catalog.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.itmo.gymbro.catalog.model.Sport;

public interface SportRepository extends R2dbcRepository<Sport, Long> {

    public Mono<Boolean> existsByName(String name);

    public Flux<Sport> findAllBy(Pageable pageable);
}
