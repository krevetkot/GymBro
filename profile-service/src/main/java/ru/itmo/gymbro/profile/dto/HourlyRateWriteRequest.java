package ru.itmo.gymbro.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import ru.itmo.gymbro.profile.model.UserProfile;

@Schema(example = "{\"hourlyRate\":1500}")
public record HourlyRateWriteRequest(
        @Schema(description = "Цена часа в рублях; null убирает цену из анкеты")
        @Positive @Max(UserProfile.MAX_HOURLY_RATE) Integer hourlyRate) {
}
