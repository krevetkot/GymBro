package ru.itmo.gymbro.matching.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class LikeRequest {

    @NotNull
    @Positive
    private Long toUserId;

    public Long getToUserId() {
        return toUserId;
    }
}
