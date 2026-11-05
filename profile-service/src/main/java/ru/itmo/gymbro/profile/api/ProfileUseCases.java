package ru.itmo.gymbro.profile.api;

import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.model.UserSport;

import java.time.LocalDate;
import java.util.List;

public interface ProfileUseCases {

    public UserProfile getByUserId(long userId);

    public SavedProfile saveMine(String name, LocalDate birthDate, String about);

    public UserProfile replaceMySports(List<UserSport> sports);

    public UserProfile replaceMyGyms(List<Long> gymIds);

    /** Указать или убрать ({@code null}) цену часа занятия. Только для тренера. */
    public UserProfile changeMyHourlyRate(Integer hourlyRate);

    /**
     * Подтверждён ли сертификат владельца анкеты (роль TRAINER в identity). Если identity недоступен,
     * возвращается {@code null}: анкету всё равно можно показать, но без блока тренера.
     */
    public Boolean isVerifiedTrainer(long userId);
}
