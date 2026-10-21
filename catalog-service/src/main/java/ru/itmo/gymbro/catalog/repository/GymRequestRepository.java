package ru.itmo.gymbro.catalog.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.itmo.gymbro.catalog.model.GymRequest;

public interface GymRequestRepository extends R2dbcRepository<GymRequest, Long> {

    /** Блокирует строку заявки до конца транзакции: одобрить или отклонить её может только один запрос. */
    @Query("SELECT * FROM gym_requests WHERE id = :id FOR UPDATE")
    public Mono<GymRequest> findByIdForUpdate(long id);

    public Flux<GymRequest> findAllBy(Pageable pageable);
}
