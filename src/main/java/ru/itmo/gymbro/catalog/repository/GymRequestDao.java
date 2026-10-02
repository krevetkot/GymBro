package ru.itmo.gymbro.catalog.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import ru.itmo.gymbro.catalog.model.GymRequest;

import java.util.Optional;

interface GymRequestDao extends JpaRepository<GymRequest, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public Optional<GymRequest> findLockedById(long id);
}
