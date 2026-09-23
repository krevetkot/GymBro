package ru.itmo.gymbro.catalog.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.itmo.gymbro.catalog.model.GymRequest;

public interface GymRequestUseCases {

    GymRequest submit(String name, String city, String address);

    Page<GymRequest> getPage(Pageable pageable);
}

