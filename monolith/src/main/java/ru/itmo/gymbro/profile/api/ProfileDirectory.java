package ru.itmo.gymbro.profile.api;

import java.util.Set;

/** Transport-ready read contract, independent of Spring Data and JPA. */
public interface ProfileDirectory {

    FeedCandidates findFeedCandidates(long viewerUserId, Set<Long> excludedUserIds, int page, int size);
}
