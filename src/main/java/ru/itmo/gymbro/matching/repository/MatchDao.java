package ru.itmo.gymbro.matching.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.itmo.gymbro.matching.model.Match;

import java.util.Optional;

interface MatchDao extends JpaRepository<Match, Long> {

    public Optional<Match> findByUser1IdAndUser2Id(long user1Id, long user2Id);

    public Page<Match> findByUser1IdOrUser2Id(long user1Id, long user2Id, Pageable pageable);

    @Query(value = "SELECT COUNT(*) FROM (SELECT pg_advisory_xact_lock(hashtextextended(:pairKey, 0))) AS locked",
            nativeQuery = true)
    public long lockPairUntilCommit(String pairKey);
}
