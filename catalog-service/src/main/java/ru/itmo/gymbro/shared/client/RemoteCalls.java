package ru.itmo.gymbro.shared.client;

import feign.FeignException;
import ru.itmo.gymbro.shared.api.DependencyUnavailableException;
import ru.itmo.gymbro.shared.api.ForbiddenException;
import ru.itmo.gymbro.shared.api.NotFoundException;
import ru.itmo.gymbro.shared.api.UnauthorizedException;

import java.util.function.Supplier;

/**
 * Вызов соседнего сервиса через Feign: ошибки клиента (4xx) превращаются в доменные исключения,
 * а 5xx, таймауты, обрывы и пустые ответы — в {@link DependencyUnavailableException} (503).
 */
public final class RemoteCalls {

    private RemoteCalls() {
    }

    /**
     * @param service  сервис в родительном падеже для текста ошибки: «пользователей», «каталога»
     * @param notFound текст ошибки, если сосед ответил 404
     */
    public static <T> T call(String service, String notFound, Supplier<T> request) {
        try {
            T result = request.get();
            if (result == null) {
                throw new DependencyUnavailableException("Сервис " + service + " вернул пустой ответ");
            }
            return result;
        } catch (FeignException exception) {
            throw switch (exception.status()) {
                case 400 -> new IllegalArgumentException("Некорректный запрос к сервису " + service);
                case 401 -> new UnauthorizedException("Сервис " + service + " требует аутентификацию");
                case 403 -> new ForbiddenException("Сервис " + service + " запретил операцию");
                case 404 -> new NotFoundException(notFound);
                default -> new DependencyUnavailableException("Сервис " + service + " временно недоступен", exception);
            };
        }
    }
}
