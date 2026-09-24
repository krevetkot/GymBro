package ru.itmo.gymbro.profile.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.gymbro.profile.api.RemoveProfilePhotoUseCase;
import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.repository.UserProfileRepository;
import ru.itmo.gymbro.shared.api.NotFoundException;

@Service
class RemoveProfilePhotoService implements RemoveProfilePhotoUseCase {

    private final MyProfile myProfile;
    private final UserProfileRepository profiles;

    RemoveProfilePhotoService(MyProfile myProfile, UserProfileRepository profiles) {
        this.myProfile = myProfile;
        this.profiles = profiles;
    }

    @Override
    @Transactional
    public void removeMyPhoto(int position) {
        UserProfile profile = myProfile.lockForChange();
        if (position < 0 || position >= profile.getPhotos().size()) {
            throw new NotFoundException("Фотографии с позицией " + position + " нет в анкете");
        }
        profile.removePhoto(position);
        profiles.save(profile);
    }
}
