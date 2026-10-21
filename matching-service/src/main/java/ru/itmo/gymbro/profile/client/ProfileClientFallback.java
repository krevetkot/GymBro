package ru.itmo.gymbro.profile.client;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.itmo.gymbro.profile.api.FeedCandidates;

import java.util.List;

/**
 * Лента деградирует мягко: если сервис анкет упал, не ответил вовремя или Circuit Breaker
 * приостановил запросы к нему, пользователь получает пустую страницу вместо ошибки.
 * Ответы 4xx — не сбой сервиса (например, 404 «нет своей анкеты»), они пробрасываются как есть.
 */
@Slf4j
@Component
class ProfileClientFallback implements FallbackFactory<ProfileClient> {

    @Override
    public ProfileClient create(Throwable cause) {
        return (viewerUserId, excludedUserIds, page, size) -> {
            if (cause instanceof FeignException.FeignClientException clientError) {
                throw clientError;
            }
            log.warn("Сервис анкет недоступен, лента для пользователя {} отдана пустой: {}", viewerUserId, cause.toString());
            return new FeedCandidates(List.of(), page, size, false);
        };
    }
}
