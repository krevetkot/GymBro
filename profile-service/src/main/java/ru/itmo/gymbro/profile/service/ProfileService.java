package ru.itmo.gymbro.profile.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.gymbro.catalog.api.CatalogDirectory;
import ru.itmo.gymbro.identity.api.UserDirectory;
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
import ru.itmo.gymbro.shared.api.DependencyUnavailableException;
import ru.itmo.gymbro.shared.api.NotFoundException;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
class ProfileService implements ProfileUseCases, ProfileDirectory {

    private static final String FILL_PROFILE_FIRST = "Сначала заполните анкету через PUT /api/v1/profiles/me";

    private final UserProfileRepository profiles;
    private final CurrentUserAccess access;
    private final CatalogDirectory catalog;
    private final UserDirectory users;

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
    @Transactional
    public UserProfile changeMyHourlyRate(Integer hourlyRate) {
        long userId = access.requireTrainer();
        UserProfile profile = profiles.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new NotFoundException(FILL_PROFILE_FIRST));
        profile.changeHourlyRate(hourlyRate);
        return profiles.save(profile);
    }

    @Override
    public Boolean isVerifiedTrainer(long userId) {
        try {
            return users.getSnapshot(userId).isTrainer();
        } catch (NotFoundException exception) {
            return false;
        } catch (DependencyUnavailableException exception) {
            return null;
        }
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
        Slice<UserProfile> found = profiles.findFeed(viewer.getId(), excluded, PageRequest.of(page, size));
        Set<Long> trainers = trainersAmong(found.getContent());
        List<ProfileSnapshot> candidates = found.getContent().stream()
                .map(profile -> new ProfileSnapshot(profile.getUserId(), profile.getName(),
                        profile.getAge(), profile.getAbout(),
                        profile.getSports().stream().map(UserSport::getSportId).sorted().toList(),
                        profile.getGyms().stream().map(UserGym::getGymId).sorted().toList(),
                        trainers.contains(profile.getUserId())
                                ? new ProfileSnapshot.TrainerSnapshot(profile.getHourlyRate())
                                : null))
                .toList();
        return new FeedCandidates(candidates, page, size, found.hasNext());
    }

    /** Один запрос к identity на страницу ленты; при его недоступности лента показывается без отметок тренеров. */
    private Set<Long> trainersAmong(List<UserProfile> page) {
        if (page.isEmpty()) {
            return Set.of();
        }
        try {
            return users.trainerIds(page.stream().map(UserProfile::getUserId).collect(Collectors.toSet()));
        } catch (DependencyUnavailableException exception) {
            return Set.of();
        }
    }

    private UserProfile lockMyProfile() {
        long userId = access.requireActiveUser();
        return profiles.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new NotFoundException(FILL_PROFILE_FIRST));
    }
}
