package ru.itmo.gymbro.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(example = "{\"name\":\"Атлет\",\"city\":\"Санкт-Петербург\",\"address\":\"Невский проспект, 10\"}")
public record GymRequestSubmission(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 300) String address) {
}
