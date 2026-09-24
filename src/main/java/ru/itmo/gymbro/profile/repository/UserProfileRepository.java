package ru.itmo.gymbro.profile.repository;

import ru.itmo.gymbro.profile.model.UserProfile;

import java.util.Optional;

public interface UserProfileRepository {

    UserProfile save(UserProfile profile);

    Optional<UserProfile> findById(long id);

    Optional<UserProfile> findByUserId(long userId);

    Optional<UserProfile> findByUserIdForUpdate(long userId);

    boolean existsByUserId(long userId);

    void deleteByUserId(long userId);
}
