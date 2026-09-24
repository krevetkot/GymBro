package ru.itmo.gymbro.profile.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.gymbro.identity.api.CurrentUserAccess;
import ru.itmo.gymbro.profile.api.EditProfileUseCase;
import ru.itmo.gymbro.profile.api.SavedProfile;
import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.repository.UserProfileRepository;

import java.time.LocalDate;
import java.util.Optional;

@Service
class EditProfileService implements EditProfileUseCase {

    private final UserProfileRepository profiles;
    private final CurrentUserAccess access;

    EditProfileService(UserProfileRepository profiles, CurrentUserAccess access) {
        this.profiles = profiles;
        this.access = access;
    }

    @Override
    @Transactional
    public SavedProfile saveMine(String name, LocalDate birthDate, String about) {
        long userId = access.requireActiveUser();
        Optional<UserProfile> existing = profiles.findByUserIdForUpdate(userId);
        if (existing.isEmpty()) {
            return new SavedProfile(profiles.save(UserProfile.create(userId, name, birthDate, about)), true);
        }
        UserProfile profile = existing.get();
        profile.edit(name, birthDate, about);
        return new SavedProfile(profiles.save(profile), false);
    }
}
