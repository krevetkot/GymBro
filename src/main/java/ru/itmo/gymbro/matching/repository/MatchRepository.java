package ru.itmo.gymbro.matching.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.itmo.gymbro.matching.model.Match;

import java.util.Optional;

public interface MatchRepository {

    Match save(Match match);

    Optional<Match> findById(long id);

    Optional<Match> findByPair(long oneUserId, long anotherUserId);

    Page<Match> findInvolving(long userId, Pageable pageable);

    void lockPairUntilCommit(long oneUserId, long anotherUserId);
}
