package ru.itmo.gymbro.catalog.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;
import ru.itmo.gymbro.catalog.model.Sport;

public interface SportUseCases {

    public Mono<Sport> create(String name);

    public Mono<Sport> getById(long id);

    public Mono<Page<Sport>> getPage(Pageable pageable);

    public Mono<Void> delete(long id);
}
