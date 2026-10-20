package ru.itmo.gymbro.identity.api;

import ru.itmo.gymbro.identity.model.User;

public record UserSnapshot(long id, boolean active, boolean admin) {

    public static UserSnapshot from(User user) {
        return new UserSnapshot(user.getId(), user.isActive(), user.isAdmin());
    }
}
