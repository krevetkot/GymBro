package ru.itmo.gymbro.profile.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.gymbro.catalog.api.SportUseCases;
import ru.itmo.gymbro.profile.api.ChangeProfileSportsUseCase;
import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.model.UserSport;
import ru.itmo.gymbro.profile.repository.UserProfileRepository;

import java.util.HashSet;
import java.util.List;

@Service
class ChangeProfileSportsService implements ChangeProfileSportsUseCase {

    private final MyProfile myProfile;
    private final UserProfileRepository profiles;
    private final SportUseCases catalogSports;

    ChangeProfileSportsService(MyProfile myProfile, UserProfileRepository profiles, SportUseCases catalogSports) {
        this.myProfile = myProfile;
        this.profiles = profiles;
        this.catalogSports = catalogSports;
    }

    @Override
    @Transactional
    public UserProfile replaceMySports(List<UserSport> sports) {
        UserProfile profile = myProfile.lockForChange();
        if (new HashSet<>(sports).size() != sports.size()) {
            throw new IllegalArgumentException("Каждый вид спорта можно указать только один раз");
        }
        sports.forEach(sport -> catalogSports.getById(sport.getSportId()));
        profile.replaceSports(sports);
        return profiles.save(profile);
    }
}
