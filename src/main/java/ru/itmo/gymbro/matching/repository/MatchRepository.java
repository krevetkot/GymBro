package ru.itmo.gymbro.matching.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.itmo.gymbro.matching.model.Match;

import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {

    public Optional<Match> findByUser1IdAndUser2Id(long user1Id, long user2Id);

    public Page<Match> findByUser1IdOrUser2Id(long user1Id, long user2Id, Pageable pageable);

    @Query(value = "SELECT COUNT(*) FROM (SELECT pg_advisory_xact_lock(hashtextextended(:pairKey, 0))) AS locked",
            nativeQuery = true)
    public long acquirePairLock(String pairKey);

    default Optional<Match> findByPair(long oneUserId, long anotherUserId) {
        return findByUser1IdAndUser2Id(
                Math.min(oneUserId, anotherUserId),
                Math.max(oneUserId, anotherUserId));
    }

    default Page<Match> findInvolving(long userId, Pageable pageable) {
        return findByUser1IdOrUser2Id(userId, userId, pageable);
    }

    default void lockPairUntilCommit(long oneUserId, long anotherUserId) {
        acquirePairLock("match:" + Math.min(oneUserId, anotherUserId)
                + ":" + Math.max(oneUserId, anotherUserId));
    }
}
