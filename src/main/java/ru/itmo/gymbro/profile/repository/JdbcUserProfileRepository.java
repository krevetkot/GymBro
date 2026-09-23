package ru.itmo.gymbro.profile.repository;

import org.springframework.stereotype.Repository;
import ru.itmo.gymbro.profile.model.UserProfile;

import java.util.Optional;

@Repository
class JdbcUserProfileRepository implements UserProfileRepository {

    private final UserProfileDao dao;

    JdbcUserProfileRepository(UserProfileDao dao) {
        this.dao = dao;
    }

    @Override
    public UserProfile save(UserProfile profile) {
        return dao.save(profile);
    }

    @Override
    public Optional<UserProfile> findById(long id) {
        return dao.findById(id);
    }

    @Override
    public Optional<UserProfile> findByUserId(long userId) {
        return dao.findByUserId(userId);
    }

    @Override
    public boolean existsByUserId(long userId) {
        return dao.existsByUserId(userId);
    }

    @Override
    public void deleteByUserId(long userId) {
        dao.findByUserId(userId).ifPresent(dao::delete);
    }
}
