package ru.itmo.gymbro;

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

    public long trainer() {
        return add(true, Role.TRAINER);
    }

    public long admin() {
        return add(true, Role.ADMIN);
    }

    public long banned() {
        return add(false, Role.USER);
    }

    public void ban(long id) {
        UserSnapshot user = getSnapshot(id);
        users.put(id, new UserSnapshot(id, false, user.admin(), user.role()));
    }

    /** Меняет роль в identity. Новые токены получат её, уже выданные — сохраняют прежнюю. */
    public void changeRole(long id, Role role) {
        UserSnapshot user = getSnapshot(id);
        users.put(id, new UserSnapshot(id, user.active(), role == Role.ADMIN, role));
        TestTokens.rememberRole(id, role.name());
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
    public Set<Long> trainerIds(Set<Long> ids) {
        return ids.stream()
                .filter(id -> users.containsKey(id) && users.get(id).isTrainer())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<Long> bannedUserIds() {
        return users.values().stream()
                .filter(user -> !user.active())
                .map(UserSnapshot::id)
                .collect(Collectors.toUnmodifiableSet());
    }

    private long add(boolean active, Role role) {
        long id = ids.incrementAndGet();
        users.put(id, new UserSnapshot(id, active, role == Role.ADMIN, role));
        TestTokens.rememberRole(id, role.name());
        return id;
    }
}
