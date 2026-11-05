package ru.itmo.gymbro.identity.api;

import java.util.Arrays;

/**
 * Снимок пользователя из identity-service. Поле {@code admin} ответа здесь не нужно:
 * права выводятся из роли.
 */
public record UserSnapshot(long id, boolean active, Role role) {

    public boolean hasAnyRole(Role... roles) {
        return Arrays.asList(roles).contains(role);
    }
}
