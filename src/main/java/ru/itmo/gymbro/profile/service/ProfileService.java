package ru.itmo.gymbro.profile.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.gymbro.catalog.api.GymUseCases;
import ru.itmo.gymbro.catalog.api.SportUseCases;
import ru.itmo.gymbro.identity.api.CurrentUserAccess;
import ru.itmo.gymbro.profile.api.ProfileUseCases;
import ru.itmo.gymbro.profile.api.SavedProfile;
import ru.itmo.gymbro.profile.model.UserGym;
import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.model.UserSport;
import ru.itmo.gymbro.profile.repository.UserProfileRepository;
import ru.itmo.gymbro.shared.api.NotFoundException;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
class ProfileService implements ProfileUseCases {

    private static final String FILL_PROFILE_FIRST = "Сначала заполните анкету через PUT /api/v1/profiles/me";

    private final UserProfileRepository profiles;
    private final CurrentUserAccess access;
    private final SportUseCases catalogSports;
    private final GymUseCases catalogGyms;

    ProfileService(UserProfileRepository profiles, CurrentUserAccess access,
                   SportUseCases catalogSports, GymUseCases catalogGyms) {
        this.profiles = profiles;
        this.access = access;
        this.catalogSports = catalogSports;
        this.catalogGyms = catalogGyms;
    }

    @Override
    public UserProfile getByUserId(long userId) {
        return profiles.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Анкета пользователя " + userId + " не найдена"));
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

    @Override
    @Transactional
    public UserProfile replaceMySports(List<UserSport> sports) {
        UserProfile profile = lockMyProfile();
        if (new HashSet<>(sports).size() != sports.size()) {
            throw new IllegalArgumentException("Каждый вид спорта можно указать только один раз");
        }
        sports.forEach(sport -> catalogSports.getById(sport.getSportId()));
        profile.replaceSports(sports);
        return profiles.save(profile);
    }

    @Override
    @Transactional
    public UserProfile replaceMyGyms(List<Long> gymIds) {
        UserProfile profile = lockMyProfile();
        if (new HashSet<>(gymIds).size() != gymIds.size()) {
            throw new IllegalArgumentException("Каждый зал можно указать только один раз");
        }
        gymIds.forEach(catalogGyms::getById);
        profile.replaceGyms(gymIds.stream().map(UserGym::of).toList());
        return profiles.save(profile);
    }

    @Override
    public Slice<UserProfile> findFeedCandidates(long viewerUserId, Set<Long> excludedUserIds, Pageable pageable) {
        UserProfile viewer = profiles.findByUserId(viewerUserId)
                .orElseThrow(() -> new NotFoundException(FILL_PROFILE_FIRST));
        Set<Long> excluded = new HashSet<>(excludedUserIds);
        excluded.add(viewerUserId);
        return profiles.findFeed(viewer.getId(), excluded, pageable);
    }

    private UserProfile lockMyProfile() {
        long userId = access.requireActiveUser();
        return profiles.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new NotFoundException(FILL_PROFILE_FIRST));
    }
}
