package ru.itmo.gymbro.catalog.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.itmo.gymbro.catalog.model.Gym;

public interface GymRepository extends R2dbcRepository<Gym, Long> {

    public Mono<Boolean> existsByCityAndAddress(String city, String address);

    public Flux<Gym> findAllBy(Pageable pageable);
}
