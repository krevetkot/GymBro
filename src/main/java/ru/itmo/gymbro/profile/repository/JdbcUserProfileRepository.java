package ru.itmo.gymbro.profile.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.itmo.gymbro.profile.model.UserProfile;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
class JdbcUserProfileRepository implements UserProfileRepository {

    private static final String FEED_PAGE = """
            SELECT p.id
            FROM user_profiles p
            WHERE p.user_id NOT IN (:excludedUserIds)
            ORDER BY
                (SELECT COUNT(*) FROM user_sports s
                 WHERE s.profile_id = p.id
                   AND s.sport_id IN (SELECT sport_id FROM user_sports WHERE profile_id = :viewerProfileId))
              + (SELECT COUNT(*) FROM user_gyms g
                 WHERE g.profile_id = p.id
                   AND g.gym_id IN (SELECT gym_id FROM user_gyms WHERE profile_id = :viewerProfileId)) DESC,
                p.id
            LIMIT :limit OFFSET :offset
            """;

    private final UserProfileDao dao;
    private final JdbcClient jdbc;

    JdbcUserProfileRepository(UserProfileDao dao, JdbcClient jdbc) {
        this.dao = dao;
        this.jdbc = jdbc;
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
    public Optional<UserProfile> findByUserIdForUpdate(long userId) {
        return dao.findLockedByUserId(userId);
    }

    @Override
    public boolean existsByUserId(long userId) {
        return dao.existsByUserId(userId);
    }

    @Override
    public void deleteByUserId(long userId) {
        dao.findByUserId(userId).ifPresent(dao::delete);
    }

    @Override
    public Slice<UserProfile> findFeed(long viewerProfileId, Collection<Long> excludedUserIds, Pageable pageable) {
        List<Long> ids = jdbc.sql(FEED_PAGE)
                .param("excludedUserIds", excludedUserIds)
                .param("viewerProfileId", viewerProfileId)
                .param("limit", pageable.getPageSize() + 1)
                .param("offset", pageable.getOffset())
                .query(Long.class)
                .list();
        boolean hasNext = ids.size() > pageable.getPageSize();
        List<Long> pageIds = hasNext ? ids.subList(0, pageable.getPageSize()) : ids;
        Map<Long, UserProfile> byId = dao.findAllById(pageIds).stream()
                .collect(Collectors.toMap(UserProfile::getId, Function.identity()));
        return new SliceImpl<>(pageIds.stream().map(byId::get).toList(), pageable, hasNext);
    }
}
