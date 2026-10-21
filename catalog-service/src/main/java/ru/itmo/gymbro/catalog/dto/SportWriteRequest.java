package ru.itmo.gymbro.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(example = "{\"name\":\"Плавание\"}")
public record SportWriteRequest(
        @NotBlank @Size(max = 100) String name) {
}
