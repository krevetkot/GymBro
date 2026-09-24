package ru.itmo.gymbro.identity.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.gymbro.identity.api.CurrentUserAccess;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.UserRepository;
import ru.itmo.gymbro.shared.api.CurrentUserProvider;
import ru.itmo.gymbro.shared.api.ForbiddenException;
import ru.itmo.gymbro.shared.api.UnauthorizedException;

@Service
@Transactional(readOnly = true)
class CurrentUserAccessService implements CurrentUserAccess {

    private final CurrentUserProvider currentUser;
    private final UserRepository users;

    CurrentUserAccessService(CurrentUserProvider currentUser, UserRepository users) {
        this.currentUser = currentUser;
        this.users = users;
    }

    @Override
    public long requireActiveUser() {
        return activeUser().getId();
    }

    @Override
    public long requireAdmin() {
        User user = activeUser();
        if (!user.isAdmin()) {
            throw new ForbiddenException("Операция доступна только администратору");
        }
        return user.getId();
    }

    private User activeUser() {
        long id = currentUser.currentUserId();
        User user = users.findById(id)
                .orElseThrow(() -> new UnauthorizedException("Текущий пользователь не найден"));
        if (!user.isActive()) {
            throw new ForbiddenException("Пользователь заблокирован");
        }
        return user;
    }
}

