package ru.itmo.gymbro.matching.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.itmo.gymbro.matching.model.Like;

import java.util.List;

interface LikeDao extends JpaRepository<Like, Long> {

    public boolean existsByFromUserIdAndToUserId(long fromUserId, long toUserId);

    @Query(value = "SELECT to_user_id FROM likes WHERE from_user_id = :fromUserId", nativeQuery = true)
    public List<Long> findLikedUserIds(long fromUserId);
}
