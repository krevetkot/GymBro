package ru.itmo.gymbro.catalog.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;
import ru.itmo.gymbro.catalog.model.GymRequest;

public interface GymRequestUseCases {

    public Mono<GymRequest> submit(String name, String city, String address);

    public Mono<Page<GymRequest>> getPage(Pageable pageable);

    public Mono<GymRequest> approve(long requestId);

    public Mono<GymRequest> reject(long requestId);
}
