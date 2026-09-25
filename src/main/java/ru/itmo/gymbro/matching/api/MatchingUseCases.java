package ru.itmo.gymbro.matching.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import ru.itmo.gymbro.matching.dto.LikeResponse;
import ru.itmo.gymbro.matching.dto.MatchResponse;
import ru.itmo.gymbro.profile.model.UserProfile;

public interface MatchingUseCases {

    public Slice<UserProfile> nextFeedCards(Pageable pageable);

    public LikeResponse like(long toUserId);

    public Page<MatchResponse> myMatches(Pageable pageable);
}
