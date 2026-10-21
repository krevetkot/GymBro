package ru.itmo.gymbro.identity.client;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
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
    public Mono<Long> requireActiveUser() {
        return activeCurrentUser().map(UserSnapshot::id);
    }

    @Override
    public Mono<Long> requireAdmin() {
        return activeCurrentUser().flatMap(user -> user.admin()
                ? Mono.just(user.id())
                : Mono.error(new ForbiddenException("Операция доступна только администратору")));
    }

    private Mono<UserSnapshot> activeCurrentUser() {
        return currentUser.currentUserId()
                // Feign блокирующий: вызов identity уходит на пул для блокирующих задач,
                // чтобы не занимать потоки event loop
                .flatMap(id -> Mono.fromCallable(() -> users.getSnapshot(id))
                        .subscribeOn(Schedulers.boundedElastic())
                        .onErrorMap(NotFoundException.class,
                                missing -> new UnauthorizedException("Текущий пользователь не найден")))
                .flatMap(user -> user.active()
                        ? Mono.just(user)
                        : Mono.error(new ForbiddenException("Пользователь заблокирован")));
    }
}
