package ru.itmo.gymbro.catalog.dto;

import ru.itmo.gymbro.catalog.model.Sport;

public record SportResponse(long id, String name) {

    public static SportResponse from(Sport entity) {
        return new SportResponse(entity.getId(), entity.getName());
    }
}

