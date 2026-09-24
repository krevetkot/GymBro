package ru.itmo.gymbro.profile.api;

import java.time.LocalDate;

public interface EditProfileUseCase {

    SavedProfile saveMine(String name, LocalDate birthDate, String about);
}
