package ru.itmo.gymbro.matching.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import ru.itmo.gymbro.matching.model.Match;

import java.util.Optional;

@Repository
class JpaMatchRepository implements MatchRepository {

    private final MatchDao dao;

    JpaMatchRepository(MatchDao dao) {
        this.dao = dao;
    }

    @Override
    public Match save(Match match) {
        return dao.saveAndFlush(match);
    }

    @Override
    public Optional<Match> findById(long id) {
        return dao.findById(id);
    }

    @Override
    public Optional<Match> findByPair(long oneUserId, long anotherUserId) {
        return dao.findByUser1IdAndUser2Id(
                Math.min(oneUserId, anotherUserId),
                Math.max(oneUserId, anotherUserId));
    }

    @Override
    public Page<Match> findInvolving(long userId, Pageable pageable) {
        return dao.findByUser1IdOrUser2Id(userId, userId, pageable);
    }

    @Override
    public void lockPairUntilCommit(long oneUserId, long anotherUserId) {
        dao.lockPairUntilCommit(
                "match:" + Math.min(oneUserId, anotherUserId) + ":" + Math.max(oneUserId, anotherUserId));
    }
}
