package ru.itmo.gymbro.matching.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.gymbro.identity.api.UserDirectory;
import ru.itmo.gymbro.matching.api.MatchingUseCases;
import ru.itmo.gymbro.matching.dto.LikeResponse;
import ru.itmo.gymbro.matching.dto.MatchResponse;
import ru.itmo.gymbro.matching.model.Like;
import ru.itmo.gymbro.matching.model.Match;
import ru.itmo.gymbro.matching.repository.LikeRepository;
import ru.itmo.gymbro.matching.repository.MatchRepository;
import ru.itmo.gymbro.profile.api.ProfileSnapshot;
import ru.itmo.gymbro.profile.api.ProfileDirectory;
import ru.itmo.gymbro.profile.api.FeedCandidates;
import ru.itmo.gymbro.shared.api.ConflictException;
import ru.itmo.gymbro.shared.api.CurrentUserAccess;
import ru.itmo.gymbro.shared.api.NotFoundException;

import java.util.HashSet;
import java.util.Set;

@Service
@Transactional(readOnly = true)
class MatchingService implements MatchingUseCases {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final CurrentUserAccess access;
    private final UserDirectory users;
    private final ProfileDirectory profiles;
    private final LikeRepository likes;
    private final MatchRepository matches;

    public MatchingService(CurrentUserAccess access, UserDirectory users, ProfileDirectory profiles,
                           LikeRepository likes, MatchRepository matches) {
        this.access = access;
        this.users = users;
        this.profiles = profiles;
        this.likes = likes;
        this.matches = matches;
    }

    @Override
    public Slice<ProfileSnapshot> nextFeedCards(Pageable pageable) {
        long viewerId = access.requireActiveUser();
        Set<Long> excluded = new HashSet<>(likes.findLikedUserIds(viewerId));
        excluded.addAll(users.bannedUserIds());
        FeedCandidates candidates = profiles.findFeedCandidates(viewerId, excluded,
                pageable.getPageNumber(), pageable.getPageSize());
        return new SliceImpl<>(candidates.items(), PageRequest.of(candidates.page(), candidates.size()),
                candidates.hasNext());
    }

    @Override
    @Transactional
    public LikeResponse like(long toUserId) {
        long fromUserId = access.requireActiveUser();
        Like like = Like.from(fromUserId, toUserId);
        if (!users.getSnapshot(toUserId).active()) {
            throw new NotFoundException("Пользователь " + toUserId + " недоступен");
        }

        matches.lockPairUntilCommit(fromUserId, toUserId);
        if (likes.existsBetween(fromUserId, toUserId)) {
            throw new ConflictException("Вы уже лайкнули пользователя " + toUserId);
        }
        Like saved = likes.save(like);
        if (!likes.existsBetween(toUserId, fromUserId)) {
            return LikeResponse.oneSided(saved);
        }
        Match match = matches.findByPair(fromUserId, toUserId)
                .orElseGet(() -> matches.save(Match.between(fromUserId, toUserId)));
        return LikeResponse.matched(saved, match);
    }

    @Override
    public Page<MatchResponse> myMatches(Pageable pageable) {
        long viewerId = access.requireActiveUser();
        return matches.findInvolving(viewerId,
                        PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), NEWEST_FIRST))
                .map(match -> MatchResponse.of(match, viewerId));
    }
}
