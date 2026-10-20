package ru.itmo.gymbro.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.gymbro.catalog.model.Gym;

public interface GymRepository extends JpaRepository<Gym, Long> {

    public boolean existsByCityAndAddress(String city, String address);
}
