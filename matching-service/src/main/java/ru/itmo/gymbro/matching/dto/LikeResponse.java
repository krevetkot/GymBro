package ru.itmo.gymbro.matching.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.itmo.gymbro.matching.model.Like;
import ru.itmo.gymbro.matching.model.Match;

import java.time.Instant;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class LikeResponse {

    private final long toUserId;
    private final Instant likedAt;
    private final boolean matched;
    private final Long matchId;

    public static LikeResponse oneSided(Like like) {
        return new LikeResponse(like.getToUserId(), like.getCreatedAt(), false, null);
    }

    public static LikeResponse matched(Like like, Match match) {
        return new LikeResponse(like.getToUserId(), like.getCreatedAt(), true, match.getId());
    }
}
