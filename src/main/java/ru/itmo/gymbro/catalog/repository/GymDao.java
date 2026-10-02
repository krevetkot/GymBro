package ru.itmo.gymbro.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.gymbro.catalog.model.Gym;

interface GymDao extends JpaRepository<Gym, Long> {

    public boolean existsByCityAndAddress(String city, String address);
}
