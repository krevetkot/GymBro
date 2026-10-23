package ru.itmo.gymbro.matching.dto;

import lombok.Getter;
import ru.itmo.gymbro.matching.model.Match;

import java.time.Instant;

@Getter
public class MatchResponse {

    private final long matchId;
    private final long partnerUserId;
    private final Instant matchedAt;

    private MatchResponse(long matchId, long partnerUserId, Instant matchedAt) {
        this.matchId = matchId;
        this.partnerUserId = partnerUserId;
        this.matchedAt = matchedAt;
    }

    public static MatchResponse of(Match match, long viewerUserId) {
        return new MatchResponse(match.getId(), match.partnerOf(viewerUserId), match.getCreatedAt());
    }
}
