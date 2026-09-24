package ru.itmo.gymbro.catalog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.itmo.gymbro.catalog.model.Gym;

import java.util.Optional;

public interface GymRepository {

    Gym save(Gym gym);

    Optional<Gym> findById(long id);

    Page<Gym> findAll(Pageable pageable);

    boolean existsById(long id);

    boolean existsByCityAndAddress(String city, String address);

    void deleteById(long id);
}
