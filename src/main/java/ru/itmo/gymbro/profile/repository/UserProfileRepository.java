package ru.itmo.gymbro.profile.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.gymbro.profile.model.UserProfile;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    public Optional<UserProfile> findByUserId(long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT profile FROM UserProfile profile WHERE profile.userId = :userId")
    public Optional<UserProfile> findByUserIdForUpdate(long userId);

    public boolean existsByUserId(long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM UserProfile profile WHERE profile.userId = :userId")
    public void deleteByUserId(long userId);

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
    public List<Long> findFeedProfileIds(long viewerProfileId, Collection<Long> excludedUserIds,
                                         int limit, long offset);

    default Slice<UserProfile> findFeed(long viewerProfileId, Collection<Long> excludedUserIds,
                                        Pageable pageable) {
        List<Long> ids = findFeedProfileIds(
                viewerProfileId, excludedUserIds, pageable.getPageSize() + 1, pageable.getOffset());
        boolean hasNext = ids.size() > pageable.getPageSize();
        List<Long> pageIds = hasNext ? ids.subList(0, pageable.getPageSize()) : ids;
        Map<Long, UserProfile> byId = findAllById(pageIds).stream()
                .collect(Collectors.toMap(UserProfile::getId, Function.identity()));
        return new SliceImpl<>(pageIds.stream().map(byId::get).toList(), pageable, hasNext);
    }
}
