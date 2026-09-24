package ru.itmo.gymbro.profile.api;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.model.UserSport;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface ProfileUseCases {

    UserProfile getByUserId(long userId);

    SavedProfile saveMine(String name, LocalDate birthDate, String about);

    UserProfile replaceMySports(List<UserSport> sports);

    UserProfile replaceMyGyms(List<Long> gymIds);

    Slice<UserProfile> findFeedCandidates(long viewerUserId, Set<Long> excludedUserIds, Pageable pageable);
}
