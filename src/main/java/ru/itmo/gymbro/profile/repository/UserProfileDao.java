package ru.itmo.gymbro.profile.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import ru.itmo.gymbro.profile.model.UserProfile;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

interface UserProfileDao extends JpaRepository<UserProfile, Long> {

    public Optional<UserProfile> findByUserId(long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public Optional<UserProfile> findLockedByUserId(long userId);

    public boolean existsByUserId(long userId);

    @Query(value = """
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
            """, nativeQuery = true)
    public List<Long> findFeedProfileIds(long viewerProfileId, Collection<Long> excludedUserIds, int limit, long offset);
}
