package ru.itmo.gymbro.matching.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.gymbro.identity.api.CurrentUserAccess;
import ru.itmo.gymbro.identity.api.ListBannedUsersUseCase;
import ru.itmo.gymbro.matching.api.BrowseFeedUseCase;
import ru.itmo.gymbro.matching.repository.LikeRepository;
import ru.itmo.gymbro.profile.api.ProfileFeedQuery;
import ru.itmo.gymbro.profile.model.UserProfile;

import java.util.HashSet;
import java.util.Set;

@Service
@Transactional(readOnly = true)
class BrowseFeedService implements BrowseFeedUseCase {

    private final CurrentUserAccess access;
    private final LikeRepository likes;
    private final ListBannedUsersUseCase bannedUsers;
    private final ProfileFeedQuery profileFeed;

    BrowseFeedService(CurrentUserAccess access, LikeRepository likes,
                      ListBannedUsersUseCase bannedUsers, ProfileFeedQuery profileFeed) {
        this.access = access;
        this.likes = likes;
        this.bannedUsers = bannedUsers;
        this.profileFeed = profileFeed;
    }

    @Override
    public Slice<UserProfile> nextCards(Pageable pageable) {
        long viewerId = access.requireActiveUser();
        Set<Long> excluded = new HashSet<>(likes.findLikedUserIds(viewerId));
        excluded.addAll(bannedUsers.bannedUserIds());
        return profileFeed.findCandidates(viewerId, excluded,
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));
    }
}
