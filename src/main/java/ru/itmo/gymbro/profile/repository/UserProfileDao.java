package ru.itmo.gymbro.profile.repository;

import org.springframework.data.relational.core.sql.LockMode;
import org.springframework.data.relational.repository.Lock;
import org.springframework.data.repository.ListCrudRepository;
import ru.itmo.gymbro.profile.model.UserProfile;

import java.util.Optional;

interface UserProfileDao extends ListCrudRepository<UserProfile, Long> {

    Optional<UserProfile> findByUserId(long userId);

    @Lock(LockMode.PESSIMISTIC_WRITE)
    Optional<UserProfile> findLockedByUserId(long userId);

    boolean existsByUserId(long userId);
}
