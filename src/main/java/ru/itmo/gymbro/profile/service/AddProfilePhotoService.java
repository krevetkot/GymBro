package ru.itmo.gymbro.profile.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.gymbro.profile.api.AddProfilePhotoUseCase;
import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.repository.UserProfileRepository;

@Service
class AddProfilePhotoService implements AddProfilePhotoUseCase {

    private final MyProfile myProfile;
    private final UserProfileRepository profiles;

    AddProfilePhotoService(MyProfile myProfile, UserProfileRepository profiles) {
        this.myProfile = myProfile;
        this.profiles = profiles;
    }

    @Override
    @Transactional
    public UserProfile addMyPhoto(String url) {
        UserProfile profile = myProfile.lockForChange();
        profile.addPhoto(url);
        return profiles.save(profile);
    }
}
