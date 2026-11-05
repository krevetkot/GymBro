package ru.itmo.gymbro.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import ru.itmo.gymbro.identity.model.Role;

@Schema(example = "{\"role\":\"TRAINER\"}")
public record ChangeRoleRequest(@NotNull Role role) {
}
