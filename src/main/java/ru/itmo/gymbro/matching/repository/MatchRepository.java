package ru.itmo.gymbro.matching.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.itmo.gymbro.matching.model.Match;

import java.util.Optional;

public interface MatchRepository {

    public Match save(Match match);

    public Optional<Match> findById(long id);

    public Optional<Match> findByPair(long oneUserId, long anotherUserId);

    public Page<Match> findInvolving(long userId, Pageable pageable);

    public void lockPairUntilCommit(long oneUserId, long anotherUserId);
}
