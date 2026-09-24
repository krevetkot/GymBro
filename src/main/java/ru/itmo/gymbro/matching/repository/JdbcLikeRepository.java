package ru.itmo.gymbro.matching.repository;

import org.springframework.stereotype.Repository;
import ru.itmo.gymbro.matching.model.Like;

import java.util.List;
import java.util.Optional;

@Repository
class JdbcLikeRepository implements LikeRepository {

    private final LikeDao dao;

    JdbcLikeRepository(LikeDao dao) {
        this.dao = dao;
    }

    @Override
    public Like save(Like like) {
        return dao.save(like);
    }

    @Override
    public Optional<Like> findBetween(long fromUserId, long toUserId) {
        return dao.findByFromUserIdAndToUserId(fromUserId, toUserId);
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
