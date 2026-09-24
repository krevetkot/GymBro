package ru.itmo.gymbro.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SportWriteRequest(
        @NotBlank @Size(max = 100) String name) {
}
