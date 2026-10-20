package ru.itmo.gymbro.catalog.dto;

import ru.itmo.gymbro.catalog.model.GymRequest;
import ru.itmo.gymbro.catalog.model.RequestStatus;

import java.time.Instant;

public record GymRequestResponse(
        long id, long authorId, String name, String city, String address,
        RequestStatus status, Long reviewedBy, Long gymId, Instant createdAt) {

    public static GymRequestResponse from(GymRequest request) {
        return new GymRequestResponse(request.getId(), request.getAuthorId(), request.getName(),
                request.getCity(), request.getAddress(), request.getStatus(), request.getReviewedBy(),
                request.getGymId(), request.getCreatedAt());
    }
}

