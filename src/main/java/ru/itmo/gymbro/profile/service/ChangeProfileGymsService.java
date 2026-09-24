package ru.itmo.gymbro.profile.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.gymbro.catalog.api.GymUseCases;
import ru.itmo.gymbro.profile.api.ChangeProfileGymsUseCase;
import ru.itmo.gymbro.profile.model.UserGym;
import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.repository.UserProfileRepository;

import java.util.HashSet;
import java.util.List;

@Service
class ChangeProfileGymsService implements ChangeProfileGymsUseCase {

    private final MyProfile myProfile;
    private final UserProfileRepository profiles;
    private final GymUseCases catalogGyms;

    ChangeProfileGymsService(MyProfile myProfile, UserProfileRepository profiles, GymUseCases catalogGyms) {
        this.myProfile = myProfile;
        this.profiles = profiles;
        this.catalogGyms = catalogGyms;
    }

    @Override
    @Transactional
    public UserProfile replaceMyGyms(List<Long> gymIds) {
        UserProfile profile = myProfile.require();
        if (new HashSet<>(gymIds).size() != gymIds.size()) {
            throw new IllegalArgumentException("Каждый зал можно указать только один раз");
        }
        gymIds.forEach(catalogGyms::getById);
        profile.replaceGyms(gymIds.stream().map(UserGym::of).toList());
        return profiles.save(profile);
    }
}
