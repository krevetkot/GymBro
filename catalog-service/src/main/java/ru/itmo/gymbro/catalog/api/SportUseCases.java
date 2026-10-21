package ru.itmo.gymbro.catalog.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.itmo.gymbro.catalog.model.Sport;

public interface SportUseCases {

    public Sport create(String name);

    public Sport getById(long id);

    public Page<Sport> getPage(Pageable pageable);

    public void delete(long id);
}

