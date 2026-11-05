package ru.itmo.gymbro.identity.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.itmo.gymbro.identity.api.UserDirectory;
import ru.itmo.gymbro.identity.api.UserSnapshot;

import java.util.Set;

import static ru.itmo.gymbro.shared.client.RemoteCalls.call;

@Component
@RequiredArgsConstructor
class RemoteUserDirectory implements UserDirectory {

    private static final String SERVICE = "пользователей";

    private final IdentityClient client;

    @Override
    public UserSnapshot getSnapshot(long id) {
        return call(SERVICE, "Пользователь " + id + " не найден", () -> client.getSnapshot(id));
    }

    @Override
    public Set<Long> bannedUserIds() {
        return call(SERVICE, "Список недоступных пользователей не найден", client::bannedUserIds);
    }

    @Override
    public Set<Long> trainerIds(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Set.of();
        }
        return call(SERVICE, "Список тренеров не найден", () -> client.trainerIds(ids));
    }
}
