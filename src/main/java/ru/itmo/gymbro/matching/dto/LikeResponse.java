package ru.itmo.gymbro.matching.dto;

import ru.itmo.gymbro.matching.api.LikeOutcome;
import ru.itmo.gymbro.matching.model.Match;

import java.time.Instant;

public class LikeResponse {

    private final long toUserId;
    private final Instant likedAt;
    private final boolean matched;
    private final Long matchId;

    public LikeResponse(long toUserId, Instant likedAt, boolean matched, Long matchId) {
        this.toUserId = toUserId;
        this.likedAt = likedAt;
        this.matched = matched;
        this.matchId = matchId;
    }

    public static LikeResponse from(LikeOutcome outcome) {
        return new LikeResponse(
                outcome.getLike().getToUserId(),
                outcome.getLike().getCreatedAt(),
                outcome.getMatch().isPresent(),
                outcome.getMatch().map(Match::getId).orElse(null));
    }

    public long getToUserId() {
        return toUserId;
    }

    public Instant getLikedAt() {
        return likedAt;
    }

    public boolean isMatched() {
        return matched;
    }

    public Long getMatchId() {
        return matchId;
    }
}
