package ru.itmo.gymbro.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(example = "{\"name\":\"Алексей\",\"birthDate\":\"2000-05-15\",\"about\":\"Ищу партнёра для тренировок\"}")
public class ProfileWriteRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    @Past
    private LocalDate birthDate;

    @Size(max = 1000)
    private String about;

    public String getName() {
        return name;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getAbout() {
        return about;
    }
}
