package ru.itmo.gymbro.catalog.repository;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import ru.itmo.gymbro.catalog.model.GymRequest;

interface GymRequestDao extends ListCrudRepository<GymRequest, Long>,
        PagingAndSortingRepository<GymRequest, Long> {
}
