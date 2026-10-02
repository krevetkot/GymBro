package ru.itmo.gymbro.matching.repository;

import org.springframework.stereotype.Repository;
import ru.itmo.gymbro.matching.model.Like;

import java.util.List;

@Repository
class JpaLikeRepository implements LikeRepository {

    private final LikeDao dao;

    JpaLikeRepository(LikeDao dao) {
        this.dao = dao;
    }

    @Override
    public Like save(Like like) {
        return dao.saveAndFlush(like);
    }

    @Override
    public boolean existsBetween(long fromUserId, long toUserId) {
        return dao.existsByFromUserIdAndToUserId(fromUserId, toUserId);
    }

    @Override
    public List<Long> findLikedUserIds(long fromUserId) {
        return dao.findLikedUserIds(fromUserId);
    }
}
