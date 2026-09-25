package ru.itmo.gymbro.catalog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.itmo.gymbro.catalog.model.Sport;

import java.util.Optional;

public interface SportRepository {

    public Sport save(Sport sport);

    public Optional<Sport> findById(long id);

    public Page<Sport> findAll(Pageable pageable);

    public boolean existsById(long id);

    public boolean existsByName(String name);

    public void deleteById(long id);
}
