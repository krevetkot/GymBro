package ru.itmo.gymbro.profile.client;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.itmo.gymbro.profile.api.FeedCandidates;

import java.util.List;

@Slf4j
@Component
class ProfileClientFallback implements FallbackFactory<ProfileClient> {

    @Override
    public ProfileClient create(Throwable cause) {
        return (viewerUserId, excludedUserIds, page, size) -> {
            if (cause instanceof FeignException.FeignClientException clientError) {
                throw clientError;
            }
            log.warn("Profile service unavailable, returning empty feed for user {}: {}", viewerUserId, cause.toString());
            return new FeedCandidates(List.of(), page, size, false);
        };
    }
}
