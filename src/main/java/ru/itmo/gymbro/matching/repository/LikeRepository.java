package ru.itmo.gymbro.matching.repository;

import ru.itmo.gymbro.matching.model.Like;

import java.util.List;

public interface LikeRepository {

    Like save(Like like);

    boolean existsBetween(long fromUserId, long toUserId);

    List<Long> findLikedUserIds(long fromUserId);
}
