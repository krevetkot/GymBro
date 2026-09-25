package ru.itmo.gymbro.profile.api;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.model.UserSport;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface ProfileUseCases {

    public UserProfile getByUserId(long userId);

    public SavedProfile saveMine(String name, LocalDate birthDate, String about);

    public UserProfile replaceMySports(List<UserSport> sports);

    public UserProfile replaceMyGyms(List<Long> gymIds);

    public Slice<UserProfile> findFeedCandidates(long viewerUserId, Set<Long> excludedUserIds, Pageable pageable);
}
