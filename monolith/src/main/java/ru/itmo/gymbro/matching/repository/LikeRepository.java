package ru.itmo.gymbro.matching.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.itmo.gymbro.matching.model.Like;

import java.util.List;

public interface LikeRepository extends JpaRepository<Like, Long> {

    public boolean existsByFromUserIdAndToUserId(long fromUserId, long toUserId);

    default boolean existsBetween(long fromUserId, long toUserId) {
        return existsByFromUserIdAndToUserId(fromUserId, toUserId);
    }

    @Query(value = "SELECT to_user_id FROM likes WHERE from_user_id = :fromUserId", nativeQuery = true)
    public List<Long> findLikedUserIds(long fromUserId);
}
