package ru.itmo.gymbro.catalog.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.itmo.gymbro.catalog.model.GymRequest;

public interface GymRequestUseCases {

    public GymRequest submit(String name, String city, String address);

    public Page<GymRequest> getPage(Pageable pageable);

    public GymRequest approve(long requestId);

    public GymRequest reject(long requestId);
}
