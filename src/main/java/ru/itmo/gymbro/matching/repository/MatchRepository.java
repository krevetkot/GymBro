package ru.itmo.gymbro.matching.repository;

import ru.itmo.gymbro.matching.model.Match;

import java.util.Optional;

public interface MatchRepository {

    Match save(Match match);

    Optional<Match> findById(long id);

    Optional<Match> findByPair(long oneUserId, long anotherUserId);

    void lockPairUntilCommit(long oneUserId, long anotherUserId);
}
