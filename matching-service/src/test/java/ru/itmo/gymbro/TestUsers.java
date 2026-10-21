package ru.itmo.gymbro;

import ru.itmo.gymbro.identity.api.UserDirectory;
import ru.itmo.gymbro.identity.api.UserSnapshot;
import ru.itmo.gymbro.shared.api.NotFoundException;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Пользователи для интеграционных тестов монолита вместо identity-service.
 * Подменяет {@code RemoteUserDirectory}: бизнес-тестам не нужен HTTP, а сам Feign-клиент
 * проверяется отдельно на WireMock.
 */
public class TestUsers implements UserDirectory {

    private final AtomicLong ids = new AtomicLong(1_000);
    private final Map<Long, UserSnapshot> users = new ConcurrentHashMap<>();

    public long active() {
        return add(true, false);
    }

    public long admin() {
        return add(true, true);
    }

    public long banned() {
        return add(false, false);
    }

    public void ban(long id) {
        UserSnapshot user = getSnapshot(id);
        users.put(id, new UserSnapshot(id, false, user.admin()));
    }

    public void remove(long id) {
        users.remove(id);
    }

    @Override
    public UserSnapshot getSnapshot(long id) {
        UserSnapshot user = users.get(id);
        if (user == null) {
            throw new NotFoundException("Пользователь не найден");
        }
        return user;
    }

    @Override
    public Set<Long> bannedUserIds() {
        return users.values().stream()
                .filter(user -> !user.active())
                .map(UserSnapshot::id)
                .collect(Collectors.toUnmodifiableSet());
    }

    private long add(boolean active, boolean admin) {
        long id = ids.incrementAndGet();
        users.put(id, new UserSnapshot(id, active, admin));
        return id;
    }
}
