package ru.itmo.gymbro.matching.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(example = "{\"toUserId\":2}")
public class LikeRequest {

    @NotNull
    @Positive
    private Long toUserId;

    public Long getToUserId() {
        return toUserId;
    }
}
