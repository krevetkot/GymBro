package ru.itmo.gymbro.matching.repository;

import org.springframework.data.repository.ListCrudRepository;
import ru.itmo.gymbro.matching.model.Like;

import java.util.Optional;

interface LikeDao extends ListCrudRepository<Like, Long> {

    Optional<Like> findByFromUserIdAndToUserId(long fromUserId, long toUserId);

    boolean existsByFromUserIdAndToUserId(long fromUserId, long toUserId);
}
