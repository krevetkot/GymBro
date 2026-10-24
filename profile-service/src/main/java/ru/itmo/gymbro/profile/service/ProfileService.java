package ru.itmo.gymbro.profile.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.gymbro.catalog.api.CatalogDirectory;
import ru.itmo.gymbro.profile.api.FeedCandidates;
import ru.itmo.gymbro.profile.api.ProfileDirectory;
import ru.itmo.gymbro.profile.api.ProfileSnapshot;
import ru.itmo.gymbro.profile.api.ProfileUseCases;
import ru.itmo.gymbro.profile.api.SavedProfile;
import ru.itmo.gymbro.profile.model.UserGym;
import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.model.UserSport;
import ru.itmo.gymbro.profile.repository.UserProfileRepository;
import ru.itmo.gymbro.shared.api.CurrentUserAccess;
import ru.itmo.gymbro.shared.api.NotFoundException;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
class ProfileService implements ProfileUseCases, ProfileDirectory {

    private static final String FILL_PROFILE_FIRST = "Сначала заполните анкету через PUT /api/v1/profiles/me";

    private final UserProfileRepository profiles;
    private final CurrentUserAccess access;
    private final CatalogDirectory catalog;

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
        sports.forEach(sport -> catalog.getSport(sport.getSportId()));
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
        gymIds.forEach(catalog::getGym);
        profile.replaceGyms(gymIds.stream().map(UserGym::of).toList());
        return profiles.save(profile);
    }

    @Override
    public FeedCandidates findFeedCandidates(long viewerUserId, Set<Long> excludedUserIds, int page, int size) {
        if (viewerUserId <= 0 || page < 0 || size < 1 || size > 50) {
            throw new IllegalArgumentException("Некорректные параметры поиска анкет");
        }
        UserProfile viewer = profiles.findByUserId(viewerUserId)
                .orElseThrow(() -> new NotFoundException(FILL_PROFILE_FIRST));
        Set<Long> excluded = new HashSet<>(excludedUserIds);
        excluded.add(viewerUserId);
        Slice<ProfileSnapshot> candidates = profiles.findFeed(viewer.getId(), excluded, PageRequest.of(page, size))
                .map(profile -> new ProfileSnapshot(profile.getUserId(), profile.getName(),
                        profile.getAge(), profile.getAbout(),
                        profile.getSports().stream().map(UserSport::getSportId).sorted().toList(),
                        profile.getGyms().stream().map(UserGym::getGymId).sorted().toList()));
        return new FeedCandidates(candidates.getContent(), page, size, candidates.hasNext());
    }

    private UserProfile lockMyProfile() {
        long userId = access.requireActiveUser();
        return profiles.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new NotFoundException(FILL_PROFILE_FIRST));
    }
}
