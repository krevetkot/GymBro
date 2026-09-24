package ru.itmo.gymbro.matching.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.ListCrudRepository;
import ru.itmo.gymbro.matching.model.Match;

import java.util.Optional;

interface MatchDao extends ListCrudRepository<Match, Long> {

    Optional<Match> findByUser1IdAndUser2Id(long user1Id, long user2Id);

    Page<Match> findByUser1IdOrUser2Id(long user1Id, long user2Id, Pageable pageable);
}
