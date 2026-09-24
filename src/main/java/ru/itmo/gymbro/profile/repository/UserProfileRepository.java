package ru.itmo.gymbro.profile.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import ru.itmo.gymbro.profile.model.UserProfile;

import java.util.Collection;
import java.util.Optional;

public interface UserProfileRepository {

    UserProfile save(UserProfile profile);

    Optional<UserProfile> findById(long id);

    Optional<UserProfile> findByUserId(long userId);

    Optional<UserProfile> findByUserIdForUpdate(long userId);

    boolean existsByUserId(long userId);

    void deleteByUserId(long userId);

    Slice<UserProfile> findFeed(long viewerProfileId, Collection<Long> excludedUserIds, Pageable pageable);
}
