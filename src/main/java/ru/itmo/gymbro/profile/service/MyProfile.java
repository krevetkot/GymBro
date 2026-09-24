package ru.itmo.gymbro.profile.service;

import org.springframework.stereotype.Component;
import ru.itmo.gymbro.identity.api.CurrentUserAccess;
import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.repository.UserProfileRepository;
import ru.itmo.gymbro.shared.api.NotFoundException;

@Component
class MyProfile {

    private final UserProfileRepository profiles;
    private final CurrentUserAccess access;

    MyProfile(UserProfileRepository profiles, CurrentUserAccess access) {
        this.profiles = profiles;
        this.access = access;
    }

    UserProfile require() {
        long userId = access.requireActiveUser();
        return profiles.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Сначала заполните анкету через PUT /api/v1/profiles/me"));
    }
}
