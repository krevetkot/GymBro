package ru.itmo.gymbro.matching.dto;

import ru.itmo.gymbro.matching.api.MyMatch;

import java.time.Instant;

public class MatchResponse {

    private final long matchId;
    private final long partnerUserId;
    private final Instant matchedAt;

    public MatchResponse(long matchId, long partnerUserId, Instant matchedAt) {
        this.matchId = matchId;
        this.partnerUserId = partnerUserId;
        this.matchedAt = matchedAt;
    }

    public static MatchResponse from(MyMatch match) {
        return new MatchResponse(match.getMatchId(), match.getPartnerUserId(), match.getMatchedAt());
    }

    public long getMatchId() {
        return matchId;
    }

    public long getPartnerUserId() {
        return partnerUserId;
    }

    public Instant getMatchedAt() {
        return matchedAt;
    }
}
