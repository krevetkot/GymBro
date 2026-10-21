package ru.itmo.gymbro;

import ru.itmo.gymbro.profile.api.FeedCandidates;
import ru.itmo.gymbro.profile.api.ProfileDirectory;
import ru.itmo.gymbro.profile.api.ProfileSnapshot;
import ru.itmo.gymbro.shared.api.NotFoundException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Анкеты для интеграционных тестов мэтчинга вместо profile-service.
 * Отдаёт кандидатов по возрастанию userId: порядок ленты по пересечению видов спорта и залов
 * — забота profile-service и проверяется там. Подменяет {@code RemoteProfileDirectory}.
 */
public class TestProfiles implements ProfileDirectory {

    private final Map<Long, ProfileSnapshot> profiles = new ConcurrentHashMap<>();

    public void add(long userId) {
        add(userId, List.of(), List.of());
    }

    public void add(long userId, List<Long> sportIds, List<Long> gymIds) {
        profiles.put(userId, new ProfileSnapshot(userId, "user" + userId, 25, "О себе", sportIds, gymIds));
    }

    public void remove(long userId) {
        profiles.remove(userId);
    }

    @Override
    public FeedCandidates findFeedCandidates(long viewerUserId, Set<Long> excludedUserIds, int page, int size) {
        if (!profiles.containsKey(viewerUserId)) {
            throw new NotFoundException("Сначала заполните анкету через PUT /api/v1/profiles/me");
        }
        List<ProfileSnapshot> candidates = profiles.values().stream()
                .filter(profile -> profile.userId() != viewerUserId)
                .filter(profile -> !excludedUserIds.contains(profile.userId()))
                .sorted((left, right) -> Long.compare(left.userId(), right.userId()))
                .toList();
        int from = Math.min(page * size, candidates.size());
        int to = Math.min(from + size, candidates.size());
        return new FeedCandidates(candidates.subList(from, to), page, size, to < candidates.size());
    }
}
