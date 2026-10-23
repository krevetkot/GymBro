package ru.itmo.gymbro.profile.api;

import java.util.Set;

public interface ProfileDirectory {

    FeedCandidates findFeedCandidates(long viewerUserId, Set<Long> excludedUserIds, int page, int size);
}
