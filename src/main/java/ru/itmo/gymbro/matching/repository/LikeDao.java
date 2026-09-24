package ru.itmo.gymbro.matching.repository;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import ru.itmo.gymbro.matching.model.Like;

import java.util.List;

interface LikeDao extends ListCrudRepository<Like, Long> {

    boolean existsByFromUserIdAndToUserId(long fromUserId, long toUserId);

    @Query("SELECT to_user_id FROM likes WHERE from_user_id = :fromUserId")
    List<Long> findLikedUserIds(long fromUserId);
}
