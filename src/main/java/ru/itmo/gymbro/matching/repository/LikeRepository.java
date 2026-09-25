package ru.itmo.gymbro.matching.repository;

import ru.itmo.gymbro.matching.model.Like;

import java.util.List;

public interface LikeRepository {

    public Like save(Like like);

    public boolean existsBetween(long fromUserId, long toUserId);

    public List<Long> findLikedUserIds(long fromUserId);
}
