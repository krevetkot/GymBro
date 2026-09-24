package ru.itmo.gymbro.matching.api;

import ru.itmo.gymbro.matching.model.Match;

import java.time.Instant;

public class MyMatch {

    private final long matchId;
    private final long partnerUserId;
    private final Instant matchedAt;

    public MyMatch(long matchId, long partnerUserId, Instant matchedAt) {
        this.matchId = matchId;
        this.partnerUserId = partnerUserId;
        this.matchedAt = matchedAt;
    }

    public static MyMatch of(Match match, long viewerUserId) {
        return new MyMatch(match.getId(), match.partnerOf(viewerUserId), match.getCreatedAt());
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
