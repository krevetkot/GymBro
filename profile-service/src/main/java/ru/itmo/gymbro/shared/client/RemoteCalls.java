package ru.itmo.gymbro.shared.client;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import ru.itmo.gymbro.shared.api.DependencyUnavailableException;
import ru.itmo.gymbro.shared.api.ForbiddenException;
import ru.itmo.gymbro.shared.api.NotFoundException;
import ru.itmo.gymbro.shared.api.UnauthorizedException;

import java.util.function.Supplier;

public final class RemoteCalls {

    private RemoteCalls() {
    }

    public static <T> T call(String service, String notFound, Supplier<T> request) {
        T result;
        try {
            result = request.get();
        } catch (NoFallbackAvailableException exception) {
            throw translate(service, notFound, exception.getCause());
        } catch (FeignException exception) {
            throw translate(service, notFound, exception);
        }
        if (result == null) {
            throw new DependencyUnavailableException("Сервис " + service + " вернул пустой ответ");
        }
        return result;
    }

    private static RuntimeException translate(String service, String notFound, Throwable failure) {
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
