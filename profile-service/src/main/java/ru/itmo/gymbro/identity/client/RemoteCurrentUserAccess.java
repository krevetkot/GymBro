package ru.itmo.gymbro.identity.client;

import org.springframework.stereotype.Component;
import ru.itmo.gymbro.identity.api.UserDirectory;
import ru.itmo.gymbro.identity.api.UserSnapshot;
import ru.itmo.gymbro.shared.api.CurrentUserAccess;
import ru.itmo.gymbro.shared.api.CurrentUserProvider;
import ru.itmo.gymbro.shared.api.ForbiddenException;
import ru.itmo.gymbro.shared.api.NotFoundException;
import ru.itmo.gymbro.shared.api.UnauthorizedException;

@Component
class RemoteCurrentUserAccess implements CurrentUserAccess {

    private final CurrentUserProvider currentUser;
    private final UserDirectory users;

    RemoteCurrentUserAccess(CurrentUserProvider currentUser, UserDirectory users) {
        this.currentUser = currentUser;
        this.users = users;
    }

    @Override
    public long requireActiveUser() {
        return activeCurrentUser().id();
    }

    @Override
    public long requireAdmin() {
        UserSnapshot user = activeCurrentUser();
        if (!user.admin()) {
            throw new ForbiddenException("Операция доступна только администратору");
        }
        return user.id();
    }

    private UserSnapshot activeCurrentUser() {
        long id = currentUser.currentUserId();
        UserSnapshot user;
        try {
            user = users.getSnapshot(id);
        } catch (NotFoundException exception) {
            throw new UnauthorizedException("Текущий пользователь не найден");
        }
        if (!user.active()) {
            throw new ForbiddenException("Пользователь заблокирован");
        }
        return user;
    }
}
