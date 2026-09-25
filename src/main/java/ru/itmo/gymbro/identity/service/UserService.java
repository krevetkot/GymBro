package ru.itmo.gymbro.identity.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.gymbro.identity.api.CurrentUserAccess;
import ru.itmo.gymbro.identity.api.UserUseCases;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.UserRepository;
import ru.itmo.gymbro.shared.api.ConflictException;
import ru.itmo.gymbro.shared.api.CurrentUserProvider;
import ru.itmo.gymbro.shared.api.ForbiddenException;
import ru.itmo.gymbro.shared.api.NotFoundException;
import ru.itmo.gymbro.shared.api.UnauthorizedException;

import java.util.Set;

@Service
@Transactional(readOnly = true)
class UserService implements UserUseCases, CurrentUserAccess {

    private final UserRepository users;
    private final CurrentUserProvider currentUser;

    UserService(UserRepository users, CurrentUserProvider currentUser) {
        this.users = users;
        this.currentUser = currentUser;
    }

    @Override
    @Transactional
    public User register(String email, String password) {
        if (users.existsByEmail(email)) {
            throw new ConflictException("Пользователь с email " + email + " уже зарегистрирован");
        }
        return users.save(User.register(email, password));
    }

    @Override
    public User getById(long id) {
        return users.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь " + id + " не найден"));
    }

    @Override
    public Page<User> getPage(Pageable pageable) {
        return users.findAll(PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("id")));
    }

    @Override
    @Transactional
    public User update(long id, String newEmail, String newPassword) {
        User user = getById(id);
        if (newEmail != null) {
            String currentEmail = user.getEmail();
            user.changeEmail(newEmail);
            if (!user.getEmail().equals(currentEmail) && users.existsByEmail(user.getEmail())) {
                throw new ConflictException("Пользователь с email " + user.getEmail() + " уже зарегистрирован");
            }
        }
        if (newPassword != null) {
            user.changePassword(newPassword);
        }
        return users.save(user);
    }

    @Override
    @Transactional
    public void delete(long id) {
        if (!users.existsById(id)) {
            throw new NotFoundException("Пользователь " + id + " не найден");
        }
        users.deleteById(id);
    }

    @Override
    public Set<Long> bannedUserIds() {
        return Set.copyOf(users.findBannedIds());
    }

    @Override
    public long requireActiveUser() {
        return activeCurrentUser().getId();
    }

    @Override
    public long requireAdmin() {
        User user = activeCurrentUser();
        if (!user.isAdmin()) {
            throw new ForbiddenException("Операция доступна только администратору");
        }
        return user.getId();
    }

    private User activeCurrentUser() {
        long id = currentUser.currentUserId();
        User user = users.findById(id)
                .orElseThrow(() -> new UnauthorizedException("Текущий пользователь не найден"));
        if (!user.isActive()) {
            throw new ForbiddenException("Пользователь заблокирован");
        }
        return user;
    }
}
