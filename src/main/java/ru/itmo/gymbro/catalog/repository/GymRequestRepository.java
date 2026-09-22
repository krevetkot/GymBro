package ru.itmo.gymbro.catalog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.itmo.gymbro.catalog.model.GymRequest;

import java.util.Optional;

public interface GymRequestRepository {

    GymRequest save(GymRequest request);

    Optional<GymRequest> findById(long id);

    Page<GymRequest> findAll(Pageable pageable);

    void deleteById(long id);
}
