package ru.itmo.gymbro.matching.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Schema(example = "{\"toUserId\":2}")
@Getter
public class LikeRequest {

    @NotNull
    @Positive
    private Long toUserId;
}
