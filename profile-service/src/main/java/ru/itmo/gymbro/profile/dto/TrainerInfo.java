package ru.itmo.gymbro.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Есть только у тренеров: роль TRAINER выдаётся после того, как администратор подтвердил сертификат")
public record TrainerInfo(
        @Schema(description = "Сертификат или диплом проверен администратором") boolean certificateVerified,
        @Schema(description = "Цена часа занятия в рублях; отсутствует, если тренер её не указал") Integer hourlyRate) {
}
