package ru.itmo.gymbro.catalog.repository;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.relational.core.sql.LockMode;
import org.springframework.data.relational.repository.Lock;
import ru.itmo.gymbro.catalog.model.GymRequest;

import java.util.Optional;

interface GymRequestDao extends ListCrudRepository<GymRequest, Long>,
        PagingAndSortingRepository<GymRequest, Long> {

    @Lock(LockMode.PESSIMISTIC_WRITE)
    Optional<GymRequest> findLockedById(long id);
}
