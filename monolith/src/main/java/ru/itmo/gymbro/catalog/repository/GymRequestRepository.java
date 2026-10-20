package ru.itmo.gymbro.catalog.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import ru.itmo.gymbro.catalog.model.GymRequest;

import java.util.Optional;

public interface GymRequestRepository extends JpaRepository<GymRequest, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT request FROM GymRequest request WHERE request.id = :id")
    public Optional<GymRequest> findByIdForUpdate(long id);
}
