package ru.itmo.gymbro.profile.api;

import ru.itmo.gymbro.profile.model.UserProfile;

import java.util.List;

public interface ChangeProfileGymsUseCase {

    UserProfile replaceMyGyms(List<Long> gymIds);
}
