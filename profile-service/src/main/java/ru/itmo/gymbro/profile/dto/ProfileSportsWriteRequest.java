package ru.itmo.gymbro.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(example = "{\"sports\":[{\"sportId\":1,\"level\":\"BEGINNER\"}]}")
public class ProfileSportsWriteRequest {

    @NotNull
    @Size(max = 20)
    private List<@NotNull @Valid ProfileSportItem> sports;

    public List<ProfileSportItem> getSports() {
        return sports;
    }
}
