package ru.itmo.gymbro.catalog.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.itmo.gymbro.catalog.model.Sport;

public interface SportUseCases {

    Sport create(String name);

    Sport getById(long id);

    Page<Sport> getPage(Pageable pageable);

    void delete(long id);
}

