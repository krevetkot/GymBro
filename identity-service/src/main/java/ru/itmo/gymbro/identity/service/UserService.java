package ru.itmo.gymbro.identity.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.itmo.gymbro.identity.api.UserDirectory;
import ru.itmo.gymbro.identity.api.UserSnapshot;
import ru.itmo.gymbro.identity.api.UserUseCases;
import ru.itmo.gymbro.identity.model.Role;
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
@RequiredArgsConstructor
class UserService implements UserUseCases, UserDirectory, CurrentUserAccess {

    private final UserRepository users;
    private final CurrentUserProvider currentUser;
    private final BlockingJpa jpa;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<User> register(String email, String password, Role role) {
        return requireAdmin().then(Mono.fromRunnable(() -> requireAssignable(role))).then(jpa.write(() -> {
            if (users.existsByEmail(email)) {
                throw new ConflictException("Пользователь с email " + email + " уже зарегистрирован");
            }
            return users.save(User.register(email, encodePassword(password), role == null ? Role.USER : role));
        }));
    }

    @Override
    public Mono<User> getById(long id) {
        return requireOwnerOrAdmin(id).then(jpa.read(() -> findActive(id)));
    }

    @Override
    public Mono<Page<User>> getPage(Pageable pageable) {
        return requireAdmin().then(jpa.read(() -> users.findAllByDeletedFalse(
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("id")))));
    }

    @Override
    public Mono<User> update(long id, String newEmail, String newPassword) {
        return requireOwnerOrAdmin(id).then(jpa.write(() -> {
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
                user.changePassword(encodePassword(newPassword));
            }
            return users.save(user);
        }));
    }

    @Override
    public Mono<Void> delete(long id) {
        return requireOwnerOrAdmin(id).then(jpa.write(() -> {
            findActive(id).delete();
            return true;
        })).then();
    }

    @Override
    public Mono<User> changeRole(long id, Role role) {
        return requireAdmin().flatMap(adminId -> {
            if (adminId == id) {
                return Mono.error(new ConflictException("Нельзя менять собственную роль"));
            }
            requireAssignable(role);
            return jpa.write(() -> {
                User user = findActive(id);
                user.changeRole(role);
                return users.save(user);
            });
        });
    }

    @Override
    public Mono<UserSnapshot> getSnapshot(long id) {
        return jpa.read(() -> findActive(id))
                .map(user -> new UserSnapshot(user.getId(), user.isActive(), user.isAdmin(), user.getRole()));
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

    private Mono<Void> requireOwnerOrAdmin(long targetId) {
        return activeCurrentUser()
                .flatMap(actor -> actor.isAdmin() || actor.getId() == targetId
                        ? Mono.<Void>empty()
                        : Mono.error(new ForbiddenException("Доступна только своя запись или роль ADMIN")));
    }

    private Mono<User> activeCurrentUser() {
        return currentUser.currentUserId()
                .flatMap(id -> jpa.read(() -> users.findByIdAndDeletedFalse(id)
                        .orElseThrow(() -> new UnauthorizedException("Текущий пользователь не найден"))))
                .flatMap(user -> user.isActive()
                        ? Mono.just(user)
                        : Mono.error(new ForbiddenException("Пользователь заблокирован")));
    }

    /** Роль TRAINER выдаётся только одобрением сертификата, напрямую её назначить нельзя. */
    private static void requireAssignable(Role role) {
        if (role == Role.TRAINER) {
            throw new IllegalArgumentException("Роль TRAINER выдаётся только через одобренный сертификат");
        }
    }

    private User findActive(long id) {
        return users.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("Пользователь " + id + " не найден"));
    }

    private String encodePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Пароль обязателен");
        }
        return passwordEncoder.encode(password);
    }
}
