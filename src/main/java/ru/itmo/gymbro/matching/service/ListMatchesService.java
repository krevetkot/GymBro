package ru.itmo.gymbro.matching.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.gymbro.identity.api.CurrentUserAccess;
import ru.itmo.gymbro.matching.api.ListMatchesUseCase;
import ru.itmo.gymbro.matching.api.MyMatch;
import ru.itmo.gymbro.matching.repository.MatchRepository;

@Service
@Transactional(readOnly = true)
class ListMatchesService implements ListMatchesUseCase {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final CurrentUserAccess access;
    private final MatchRepository matches;

    ListMatchesService(CurrentUserAccess access, MatchRepository matches) {
        this.access = access;
        this.matches = matches;
    }

    @Override
    public Page<MyMatch> myMatches(Pageable pageable) {
        long viewerId = access.requireActiveUser();
        return matches.findInvolving(viewerId,
                        PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), NEWEST_FIRST))
                .map(match -> MyMatch.of(match, viewerId));
    }
}
