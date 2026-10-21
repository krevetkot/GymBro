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
}
