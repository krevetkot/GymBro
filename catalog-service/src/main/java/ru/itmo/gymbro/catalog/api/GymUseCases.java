package ru.itmo.gymbro.catalog.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;
import ru.itmo.gymbro.catalog.model.Gym;

public interface GymUseCases {

    public Mono<Gym> create(String name, String city, String address);

    public Mono<Gym> getById(long id);

    public Mono<Page<Gym>> getPage(Pageable pageable);

    public Mono<Gym> update(long id, String name, String city, String address);

    public Mono<Void> delete(long id);
}
