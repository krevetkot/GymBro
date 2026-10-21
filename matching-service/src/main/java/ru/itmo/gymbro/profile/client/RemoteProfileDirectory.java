package ru.itmo.gymbro.profile.client;

import org.springframework.stereotype.Component;
import ru.itmo.gymbro.profile.api.FeedCandidates;
import ru.itmo.gymbro.profile.api.ProfileDirectory;

import java.util.Set;

import static ru.itmo.gymbro.shared.client.RemoteCalls.call;

@Component
class RemoteProfileDirectory implements ProfileDirectory {

    private static final String SERVICE = "анкет";

    private final ProfileClient client;

    RemoteProfileDirectory(ProfileClient client) {
        this.client = client;
    }

    @Override
    public FeedCandidates findFeedCandidates(long viewerUserId, Set<Long> excludedUserIds, int page, int size) {
        return call(SERVICE, "Сначала заполните анкету через PUT /api/v1/profiles/me",
                () -> client.findFeedCandidates(viewerUserId, excludedUserIds, page, size));
    }
}
