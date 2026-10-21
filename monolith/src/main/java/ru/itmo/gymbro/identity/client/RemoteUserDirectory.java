package ru.itmo.gymbro.identity.client;

import feign.FeignException;
import org.springframework.stereotype.Component;
import ru.itmo.gymbro.identity.api.UserDirectory;
import ru.itmo.gymbro.identity.api.UserSnapshot;
import ru.itmo.gymbro.shared.api.DependencyUnavailableException;
import ru.itmo.gymbro.shared.api.ForbiddenException;
import ru.itmo.gymbro.shared.api.NotFoundException;
import ru.itmo.gymbro.shared.api.UnauthorizedException;

import java.util.Set;
import java.util.function.Supplier;

@Component
class RemoteUserDirectory implements UserDirectory {

    private final IdentityClient client;

    RemoteUserDirectory(IdentityClient client) {
        this.client = client;
    }

    @Override
    public UserSnapshot getSnapshot(long id) {
        return call(() -> client.getSnapshot(id));
    }

    @Override
    public Set<Long> bannedUserIds() {
        return call(client::bannedUserIds);
    }

    private <T> T call(Supplier<T> request) {
        try {
            T result = request.get();
            if (result == null) {
                throw new DependencyUnavailableException("Сервис пользователей вернул пустой ответ");
            }
            return result;
        } catch (FeignException exception) {
            throw switch (exception.status()) {
                case 400 -> new IllegalArgumentException("Некорректный запрос к сервису пользователей");
                case 401 -> new UnauthorizedException("Сервис пользователей требует аутентификацию");
                case 403 -> new ForbiddenException("Сервис пользователей запретил операцию");
                case 404 -> new NotFoundException("Пользователь не найден");
                default -> new DependencyUnavailableException("Сервис пользователей временно недоступен", exception);
            };
        }
    }
}
