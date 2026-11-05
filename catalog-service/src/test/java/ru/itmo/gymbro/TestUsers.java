package ru.itmo.gymbro;

import reactor.core.publisher.Mono;
import ru.itmo.gymbro.identity.api.Role;
import ru.itmo.gymbro.identity.api.UserDirectory;
import ru.itmo.gymbro.identity.api.UserSnapshot;
import ru.itmo.gymbro.shared.api.NotFoundException;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class TestUsers implements UserDirectory {

    private final AtomicLong ids = new AtomicLong(1_000);
    private final Map<Long, UserSnapshot> users = new ConcurrentHashMap<>();

    public long active() {
        return add(true, Role.USER);
    }

    public long admin() {
        return add(true, Role.ADMIN);
    }

    public long banned() {
        return add(false, Role.USER);
    }

    public void ban(long id) {
        UserSnapshot user = snapshot(id);
        users.put(id, new UserSnapshot(id, false, user.role()));
    }

    /** Меняет роль в identity. Новые токены получат её, уже выданные — сохраняют прежнюю. */
    public void changeRole(long id, Role role) {
        UserSnapshot user = snapshot(id);
        users.put(id, new UserSnapshot(id, user.active(), role));
        TestTokens.rememberRole(id, role.name());
    }

    public void remove(long id) {
        users.remove(id);
    }

    @Override
    public Mono<UserSnapshot> getSnapshot(long id) {
        return Mono.fromCallable(() -> snapshot(id));
    }

    private UserSnapshot snapshot(long id) {
        UserSnapshot user = users.get(id);
        if (user == null) {
            throw new NotFoundException("Пользователь не найден");
        }
        return user;
    }

    @Override
    public Mono<Set<Long>> bannedUserIds() {
        return Mono.just(users.values().stream()
                .filter(user -> !user.active())
                .map(UserSnapshot::id)
                .collect(Collectors.toUnmodifiableSet()));
    }

    private long add(boolean active, Role role) {
        long id = ids.incrementAndGet();
        users.put(id, new UserSnapshot(id, active, role));
        TestTokens.rememberRole(id, role.name());
        return id;
    }
}
