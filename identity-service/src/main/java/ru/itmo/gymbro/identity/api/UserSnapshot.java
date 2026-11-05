package ru.itmo.gymbro.identity.api;

import ru.itmo.gymbro.identity.model.Role;

/**
 * Снимок пользователя для соседних сервисов. {@code admin} оставлен рядом с {@code role},
 * чтобы сервисы, которым нужна только проверка администратора, не зависели от списка ролей.
 */
public record UserSnapshot(long id, boolean active, boolean admin, Role role) {
}
