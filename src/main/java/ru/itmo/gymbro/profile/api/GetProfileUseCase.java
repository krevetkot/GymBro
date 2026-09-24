package ru.itmo.gymbro.profile.api;

import ru.itmo.gymbro.profile.model.UserProfile;

public interface GetProfileUseCase {

    UserProfile getByUserId(long userId);
}
