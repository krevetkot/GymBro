package ru.itmo.gymbro.catalog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.itmo.gymbro.catalog.model.Sport;

import java.util.Optional;

public interface SportRepository {

    Sport save(Sport sport);

    Optional<Sport> findById(long id);

    Page<Sport> findAll(Pageable pageable);

    boolean existsById(long id);

    boolean existsByName(String name);

    void deleteById(long id);
}
