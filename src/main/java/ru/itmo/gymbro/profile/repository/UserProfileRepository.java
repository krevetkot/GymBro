package ru.itmo.gymbro.profile.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import ru.itmo.gymbro.profile.model.UserProfile;

import java.util.Collection;
import java.util.Optional;

public interface UserProfileRepository {

    public UserProfile save(UserProfile profile);

    public Optional<UserProfile> findById(long id);

    public Optional<UserProfile> findByUserId(long userId);

    public Optional<UserProfile> findByUserIdForUpdate(long userId);

    public boolean existsByUserId(long userId);

    public void deleteByUserId(long userId);

    public Slice<UserProfile> findFeed(long viewerProfileId, Collection<Long> excludedUserIds, Pageable pageable);
}
