package ru.itmo.gymbro.profile.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.gymbro.profile.api.GetProfileUseCase;
import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.repository.UserProfileRepository;
import ru.itmo.gymbro.shared.api.NotFoundException;

@Service
@Transactional(readOnly = true)
class GetProfileService implements GetProfileUseCase {

    private final UserProfileRepository profiles;

    GetProfileService(UserProfileRepository profiles) {
        this.profiles = profiles;
    }

    @Override
    public UserProfile getByUserId(long userId) {
        return profiles.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Анкета пользователя " + userId + " не найдена"));
    }
}
