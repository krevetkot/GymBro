package ru.itmo.gymbro.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(example = "{\"gymIds\":[1,2]}")
public class ProfileGymsWriteRequest {

    @NotNull
    @Size(max = 20)
    private List<@NotNull @Positive Long> gymIds;

    public List<Long> getGymIds() {
        return gymIds;
    }
}
