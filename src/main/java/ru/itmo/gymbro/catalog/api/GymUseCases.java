package ru.itmo.gymbro.catalog.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.itmo.gymbro.catalog.model.Gym;

public interface GymUseCases {

    public Gym create(String name, String city, String address);

    public Gym getById(long id);

    public Page<Gym> getPage(Pageable pageable);

    public Gym update(long id, String name, String city, String address);

    public void delete(long id);
}

