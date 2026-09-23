package ru.itmo.gymbro.catalog.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.itmo.gymbro.catalog.model.Gym;

public interface GymUseCases {

    Gym create(String name, String city, String address);

    Gym getById(long id);

    Page<Gym> getPage(Pageable pageable);

    Gym update(long id, String name, String city, String address);

    void delete(long id);
}

