package ru.itmo.gymbro.catalog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.itmo.gymbro.catalog.model.GymRequest;

import java.util.Optional;

public interface GymRequestRepository {

    public GymRequest save(GymRequest request);

    public Optional<GymRequest> findById(long id);

    public Optional<GymRequest> findByIdForUpdate(long id);

    public Page<GymRequest> findAll(Pageable pageable);

    public void deleteById(long id);
}
