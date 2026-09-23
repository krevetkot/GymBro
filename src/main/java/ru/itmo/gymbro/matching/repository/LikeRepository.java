package ru.itmo.gymbro.matching.repository;

import ru.itmo.gymbro.matching.model.Like;

import java.util.Optional;

public interface LikeRepository {

    Like save(Like like);

    Optional<Like> findBetween(long fromUserId, long toUserId);

    boolean existsBetween(long fromUserId, long toUserId);
}
