package ru.itmo.gymbro.catalog.dto;

import ru.itmo.gymbro.catalog.model.Gym;

public record GymResponse(long id, String name, String city, String address) {

    public static GymResponse from(Gym entity) {
        return new GymResponse(entity.getId(), entity.getName(), entity.getCity(), entity.getAddress());
    }
}

