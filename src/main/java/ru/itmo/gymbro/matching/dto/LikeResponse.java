package ru.itmo.gymbro.matching.dto;

import ru.itmo.gymbro.matching.model.Like;
import ru.itmo.gymbro.matching.model.Match;

import java.time.Instant;

public class LikeResponse {

    private final long toUserId;
    private final Instant likedAt;
    private final boolean matched;
    private final Long matchId;

    private LikeResponse(long toUserId, Instant likedAt, boolean matched, Long matchId) {
        this.toUserId = toUserId;
        this.likedAt = likedAt;
        this.matched = matched;
        this.matchId = matchId;
    }

    public static LikeResponse oneSided(Like like) {
        return new LikeResponse(like.getToUserId(), like.getCreatedAt(), false, null);
    }

    public static LikeResponse matched(Like like, Match match) {
        return new LikeResponse(like.getToUserId(), like.getCreatedAt(), true, match.getId());
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
