package ru.itmo.gymbro.identity.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.itmo.gymbro.identity.api.UserDirectory;
import ru.itmo.gymbro.identity.api.UserSnapshot;
import ru.itmo.gymbro.identity.api.UserUseCases;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.UserRepository;
import ru.itmo.gymbro.shared.api.ConflictException;
import ru.itmo.gymbro.shared.api.CurrentUserAccess;
import ru.itmo.gymbro.shared.api.CurrentUserProvider;
import ru.itmo.gymbro.shared.api.ForbiddenException;
import ru.itmo.gymbro.shared.api.NotFoundException;
import ru.itmo.gymbro.shared.api.UnauthorizedException;
import ru.itmo.gymbro.shared.persistence.BlockingJpa;

import java.util.Set;

@Service
class UserService implements UserUseCases, UserDirectory, CurrentUserAccess {

    private final UserRepository users;
    private final CurrentUserProvider currentUser;
    private final BlockingJpa jpa;

    public UserService(UserRepository users, CurrentUserProvider currentUser, BlockingJpa jpa) {
        this.users = users;
        this.currentUser = currentUser;
        this.jpa = jpa;
    }

    @Override
    public Mono<User> register(String email, String password) {
        return jpa.write(() -> {
            if (users.existsByEmail(email)) {
                throw new ConflictException("Пользователь с email " + email + " уже зарегистрирован");
            }
            return users.save(User.register(email, password));
        });
    }

    @Override
    public Mono<User> getById(long id) {
        return jpa.read(() -> findActive(id));
    }

    @Override
    public Mono<Page<User>> getPage(Pageable pageable) {
        return jpa.read(() -> users.findAllByDeletedFalse(
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("id"))));
    }

    @Override
    public Mono<User> update(long id, String newEmail, String newPassword) {
        return jpa.write(() -> {
            User user = findActive(id);
            if (newEmail != null) {
                users.findByEmail(newEmail)
                        .filter(other -> !other.getId().equals(user.getId()))
                        .ifPresent(other -> {
                            throw new ConflictException("Пользователь с email " + other.getEmail() + " уже зарегистрирован");
                        });
                user.changeEmail(newEmail);
            }
            if (newPassword != null) {
                user.changePassword(newPassword);
            }
            return users.save(user);
        });
    }

    @Override
    public Mono<Void> delete(long id) {
        return jpa.write(() -> {
            findActive(id).delete();
            return true;
        }).then();
    }

    @Override
    public Mono<UserSnapshot> getSnapshot(long id) {
        return getById(id).map(user -> new UserSnapshot(user.getId(), user.isActive(), user.isAdmin()));
    }

    @Override
    public Mono<Set<Long>> bannedUserIds() {
        return jpa.read(() -> Set.copyOf(users.findBannedIds()));
    }

    @Override
    public Mono<Long> requireActiveUser() {
        return activeCurrentUser().map(User::getId);
    }

    @Override
    public Mono<Long> requireAdmin() {
        return activeCurrentUser().flatMap(user -> user.isAdmin()
                ? Mono.just(user.getId())
                : Mono.error(new ForbiddenException("Операция доступна только администратору")));
    }

    private Mono<User> activeCurrentUser() {
        return currentUser.currentUserId()
                .flatMap(id -> jpa.read(() -> users.findByIdAndDeletedFalse(id)
                        .orElseThrow(() -> new UnauthorizedException("Текущий пользователь не найден"))))
                .flatMap(user -> user.isActive()
                        ? Mono.just(user)
                        : Mono.error(new ForbiddenException("Пользователь заблокирован")));
    }

    private User findActive(long id) {
        return users.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("Пользователь " + id + " не найден"));
    }
}
