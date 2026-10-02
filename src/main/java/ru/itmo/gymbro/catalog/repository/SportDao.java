package ru.itmo.gymbro.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.gymbro.catalog.model.Sport;

interface SportDao extends JpaRepository<Sport, Long> {

    public boolean existsByName(String name);
}
