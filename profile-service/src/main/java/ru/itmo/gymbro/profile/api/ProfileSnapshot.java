package ru.itmo.gymbro.profile.api;

import java.util.List;

/**
 * Анкета кандидата для ленты. {@code trainer} есть только у тренеров с подтверждённым сертификатом;
 * если identity недоступен, тренеры в ленте не отмечаются.
 */
public record ProfileSnapshot(long userId, String name, int age, String about,
                              List<Long> sportIds, List<Long> gymIds, TrainerSnapshot trainer) {

    public ProfileSnapshot {
        sportIds = List.copyOf(sportIds);
        gymIds = List.copyOf(gymIds);
    }

    public ProfileSnapshot(long userId, String name, int age, String about, List<Long> sportIds, List<Long> gymIds) {
        this(userId, name, age, about, sportIds, gymIds, null);
    }

    /** Тренер с подтверждённым сертификатом; {@code hourlyRate} — цена часа в рублях, если указана. */
    public record TrainerSnapshot(Integer hourlyRate) {
    }
}
