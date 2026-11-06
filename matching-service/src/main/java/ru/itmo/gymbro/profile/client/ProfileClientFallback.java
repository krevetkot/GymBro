package ru.itmo.gymbro.profile.client;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.itmo.gymbro.shared.api.DependencyUnavailableException;

@Slf4j
@Component
class ProfileClientFallback implements FallbackFactory<ProfileClient> {

    @Override
    public ProfileClient create(Throwable cause) {
        return (viewerUserId, excludedUserIds, page, size) -> {
            if (cause instanceof FeignException.FeignClientException clientError) {
                throw clientError;
            }
            log.warn("Profile service unavailable, feed for user {} cannot be built: {}", viewerUserId, cause.toString());
            throw new DependencyUnavailableException(cause instanceof CallNotPermittedException
                    ? "Сервис анкет временно недоступен: после серии ошибок запросы к нему приостановлены"
                    : "Сервис анкет временно недоступен, лента не может быть показана", cause);
        };
    }
}
