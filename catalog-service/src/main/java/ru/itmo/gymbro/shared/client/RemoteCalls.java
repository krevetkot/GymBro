package ru.itmo.gymbro.shared.client;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import reactor.core.publisher.Mono;
import ru.itmo.gymbro.shared.api.DependencyUnavailableException;
import ru.itmo.gymbro.shared.api.ForbiddenException;
import ru.itmo.gymbro.shared.api.NotFoundException;
import ru.itmo.gymbro.shared.api.UnauthorizedException;

public final class RemoteCalls {

    private RemoteCalls() {
    }

    public static <T> Mono<T> call(String service, String notFound, Mono<T> request) {
        return request
                .switchIfEmpty(Mono.error(() -> new DependencyUnavailableException("Сервис " + service + " вернул пустой ответ")))
                .onErrorMap(failure -> !(failure instanceof DependencyUnavailableException),
                        failure -> translate(service, notFound, failure));
    }

    private static RuntimeException translate(String service, String notFound, Throwable failure) {
        if (failure instanceof NoFallbackAvailableException wrapped && wrapped.getCause() != null) {
            return translate(service, notFound, wrapped.getCause());
        }
        if (failure instanceof CallNotPermittedException) {
            return new DependencyUnavailableException("Сервис " + service
                    + " временно недоступен: после серии ошибок запросы к нему приостановлены", failure);
        }
        if (failure instanceof FeignException feign) {
            return switch (feign.status()) {
                case 400 -> new IllegalArgumentException("Некорректный запрос к сервису " + service);
                case 401 -> new UnauthorizedException("Сервис " + service + " требует аутентификацию");
                case 403 -> new ForbiddenException("Сервис " + service + " запретил операцию");
                case 404 -> new NotFoundException(notFound);
                default -> unavailable(service, failure);
            };
        }
        return unavailable(service, failure);
    }

    private static DependencyUnavailableException unavailable(String service, Throwable failure) {
        return new DependencyUnavailableException("Сервис " + service + " временно недоступен", failure);
    }
}
