package ru.itmo.gymbro.catalog.api;

import ru.itmo.gymbro.catalog.model.GymRequest;

public interface ReviewGymRequestUseCase {

    GymRequest approve(long requestId);

    GymRequest reject(long requestId);
}

