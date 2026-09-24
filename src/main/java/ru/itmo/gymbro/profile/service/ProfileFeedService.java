package ru.itmo.gymbro.profile.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.gymbro.profile.api.ProfileFeedQuery;
import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.repository.UserProfileRepository;
import ru.itmo.gymbro.shared.api.NotFoundException;

import java.util.HashSet;
import java.util.Set;

@Service
@Transactional(readOnly = true)
class ProfileFeedService implements ProfileFeedQuery {

    private final UserProfileRepository profiles;

    ProfileFeedService(UserProfileRepository profiles) {
        this.profiles = profiles;
    }

    @Override
    public Slice<UserProfile> findCandidates(long viewerUserId, Set<Long> excludedUserIds, Pageable pageable) {
        UserProfile viewer = profiles.findByUserId(viewerUserId)
                .orElseThrow(() -> new NotFoundException("Сначала заполните анкету через PUT /api/v1/profiles/me"));
        Set<Long> excluded = new HashSet<>(excludedUserIds);
        excluded.add(viewerUserId);
        return profiles.findFeed(viewer.getId(), excluded, pageable);
    }
}
