package ru.itmo.gymbro.matching.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.itmo.gymbro.matching.model.Match;

import java.util.Optional;

@Repository
class JdbcMatchRepository implements MatchRepository {

    private final MatchDao dao;
    private final JdbcClient jdbc;

    JdbcMatchRepository(MatchDao dao, JdbcClient jdbc) {
        this.dao = dao;
        this.jdbc = jdbc;
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

    @Override
    public Page<Match> findInvolving(long userId, Pageable pageable) {
        return dao.findByUser1IdOrUser2Id(userId, userId, pageable);
    }

    @Override
    public void lockPairUntilCommit(long oneUserId, long anotherUserId) {
        String pairKey = "match:" + Math.min(oneUserId, anotherUserId) + ":" + Math.max(oneUserId, anotherUserId);
        jdbc.sql("SELECT pg_advisory_xact_lock(hashtextextended(:pairKey, 0))")
                .param("pairKey", pairKey)
                .query((row, number) -> pairKey)
                .single();
    }
}
