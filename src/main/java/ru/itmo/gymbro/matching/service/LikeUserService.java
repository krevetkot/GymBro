package ru.itmo.gymbro.matching.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.gymbro.identity.api.CurrentUserAccess;
import ru.itmo.gymbro.identity.api.GetUserUseCase;
import ru.itmo.gymbro.matching.api.LikeOutcome;
import ru.itmo.gymbro.matching.api.LikeUserUseCase;
import ru.itmo.gymbro.matching.model.Like;
import ru.itmo.gymbro.matching.model.Match;
import ru.itmo.gymbro.matching.repository.LikeRepository;
import ru.itmo.gymbro.matching.repository.MatchRepository;
import ru.itmo.gymbro.shared.api.ConflictException;
import ru.itmo.gymbro.shared.api.NotFoundException;

@Service
class LikeUserService implements LikeUserUseCase {

    private final CurrentUserAccess access;
    private final GetUserUseCase users;
    private final LikeRepository likes;
    private final MatchRepository matches;

    LikeUserService(CurrentUserAccess access, GetUserUseCase users,
                    LikeRepository likes, MatchRepository matches) {
        this.access = access;
        this.users = users;
        this.likes = likes;
        this.matches = matches;
    }

    @Override
    @Transactional
    public LikeOutcome like(long toUserId) {
        long fromUserId = access.requireActiveUser();
        Like like = Like.from(fromUserId, toUserId);
        if (!users.getById(toUserId).isActive()) {
            throw new NotFoundException("Пользователь " + toUserId + " недоступен");
        }

        matches.lockPairUntilCommit(fromUserId, toUserId);
        if (likes.existsBetween(fromUserId, toUserId)) {
            throw new ConflictException("Вы уже лайкнули пользователя " + toUserId);
        }
        Like saved = likes.save(like);
        if (!likes.existsBetween(toUserId, fromUserId)) {
            return LikeOutcome.oneSided(saved);
        }
        Match match = matches.findByPair(fromUserId, toUserId)
                .orElseGet(() -> matches.save(Match.between(fromUserId, toUserId)));
        return LikeOutcome.matched(saved, match);
    }
}
