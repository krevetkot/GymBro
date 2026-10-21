package ru.itmo.gymbro.identity.client;

import org.springframework.stereotype.Component;
import ru.itmo.gymbro.identity.api.UserDirectory;
import ru.itmo.gymbro.identity.api.UserSnapshot;

import java.util.Set;

import static ru.itmo.gymbro.shared.client.RemoteCalls.call;

@Component
class RemoteUserDirectory implements UserDirectory {

    private static final String SERVICE = "пользователей";

    private final IdentityClient client;

    RemoteUserDirectory(IdentityClient client) {
        this.client = client;
    }

    @Override
    public UserSnapshot getSnapshot(long id) {
        return call(SERVICE, "Пользователь " + id + " не найден", () -> client.getSnapshot(id));
    }

    @Override
    public Set<Long> bannedUserIds() {
        return call(SERVICE, "Список недоступных пользователей не найден", client::bannedUserIds);
    }
}
