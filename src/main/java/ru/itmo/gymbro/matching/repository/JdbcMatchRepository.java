package ru.itmo.gymbro.matching.repository;

import org.springframework.stereotype.Repository;
import ru.itmo.gymbro.matching.model.Match;

import java.util.Optional;

@Repository
class JdbcMatchRepository implements MatchRepository {

    private final MatchDao dao;

    JdbcMatchRepository(MatchDao dao) {
        this.dao = dao;
    }

    @Override
    public Match save(Match match) {
        return dao.save(match);
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
}
