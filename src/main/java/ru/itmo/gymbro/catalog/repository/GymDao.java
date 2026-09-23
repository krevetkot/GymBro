package ru.itmo.gymbro.catalog.repository;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import ru.itmo.gymbro.catalog.model.Gym;

interface GymDao extends ListCrudRepository<Gym, Long>, PagingAndSortingRepository<Gym, Long> {

    boolean existsByCityAndAddress(String city, String address);
}
