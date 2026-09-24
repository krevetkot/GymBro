package ru.itmo.gymbro.profile.api;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import ru.itmo.gymbro.profile.model.UserProfile;

import java.util.Set;

public interface ProfileFeedQuery {

    Slice<UserProfile> findCandidates(long viewerUserId, Set<Long> excludedUserIds, Pageable pageable);
}
