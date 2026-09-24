package ru.itmo.gymbro.matching.api;

import ru.itmo.gymbro.matching.model.Like;
import ru.itmo.gymbro.matching.model.Match;

import java.util.Optional;

public class LikeOutcome {

    private final Like like;
    private final Match match;

    private LikeOutcome(Like like, Match match) {
        this.like = like;
        this.match = match;
    }

    public static LikeOutcome oneSided(Like like) {
        return new LikeOutcome(like, null);
    }

    public static LikeOutcome matched(Like like, Match match) {
        return new LikeOutcome(like, match);
    }

    public Like getLike() {
        return like;
    }

    public Optional<Match> getMatch() {
        return Optional.ofNullable(match);
    }
}
