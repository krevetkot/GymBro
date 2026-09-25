package ru.itmo.gymbro.catalog.repository;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import ru.itmo.gymbro.catalog.model.Sport;

interface SportDao extends ListCrudRepository<Sport, Long>, PagingAndSortingRepository<Sport, Long> {

    public boolean existsByName(String name);
}
