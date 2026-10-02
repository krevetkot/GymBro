package ru.itmo.gymbro.profile.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import ru.itmo.gymbro.profile.model.UserProfile;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
class JpaUserProfileRepository implements UserProfileRepository {

    private final UserProfileDao dao;

    JpaUserProfileRepository(UserProfileDao dao) {
        this.dao = dao;
    }

    @Override
    public UserProfile save(UserProfile profile) {
        return dao.saveAndFlush(profile);
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
    public Optional<UserProfile> findByUserIdForUpdate(long userId) {
        return dao.findLockedByUserId(userId);
    }

    @Override
    public boolean existsByUserId(long userId) {
        return dao.existsByUserId(userId);
    }

    @Override
    public void deleteByUserId(long userId) {
        dao.findByUserId(userId).ifPresent(profile -> {
            dao.delete(profile);
            dao.flush();
        });
    }

    @Override
    public Slice<UserProfile> findFeed(long viewerProfileId, Collection<Long> excludedUserIds, Pageable pageable) {
        List<Long> ids = dao.findFeedProfileIds(
                viewerProfileId, excludedUserIds, pageable.getPageSize() + 1, pageable.getOffset());
        boolean hasNext = ids.size() > pageable.getPageSize();
        List<Long> pageIds = hasNext ? ids.subList(0, pageable.getPageSize()) : ids;
        Map<Long, UserProfile> byId = dao.findAllById(pageIds).stream()
                .collect(Collectors.toMap(UserProfile::getId, Function.identity()));
        return new SliceImpl<>(pageIds.stream().map(byId::get).toList(), pageable, hasNext);
    }
}
