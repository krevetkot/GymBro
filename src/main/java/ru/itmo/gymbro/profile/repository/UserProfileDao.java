package ru.itmo.gymbro.profile.repository;

import org.springframework.data.repository.ListCrudRepository;
import ru.itmo.gymbro.profile.model.UserProfile;

import java.util.Optional;

interface UserProfileDao extends ListCrudRepository<UserProfile, Long> {

    Optional<UserProfile> findByUserId(long userId);

    boolean existsByUserId(long userId);
}
