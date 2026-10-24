package ru.itmo.gymbro.matching.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.itmo.gymbro.matching.model.Match;

import java.time.Instant;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MatchResponse {

    private final long matchId;
    private final long partnerUserId;
    private final Instant matchedAt;

    public static MatchResponse of(Match match, long viewerUserId) {
        return new MatchResponse(match.getId(), match.partnerOf(viewerUserId), match.getCreatedAt());
    }
}
