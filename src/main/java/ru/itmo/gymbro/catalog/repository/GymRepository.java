package ru.itmo.gymbro.catalog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.itmo.gymbro.catalog.model.Gym;

import java.util.Optional;

public interface GymRepository {

    public Gym save(Gym gym);

    public Optional<Gym> findById(long id);

    public Page<Gym> findAll(Pageable pageable);

    public boolean existsById(long id);

    public boolean existsByCityAndAddress(String city, String address);

    public void deleteById(long id);
}
