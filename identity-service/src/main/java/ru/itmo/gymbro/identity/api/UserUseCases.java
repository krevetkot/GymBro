package ru.itmo.gymbro.identity.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;
import ru.itmo.gymbro.identity.model.Role;
import ru.itmo.gymbro.identity.model.User;

public interface UserUseCases {

    /** Создаёт пользователя с ролью USER. */
    public default Mono<User> register(String email, String password) {
        return register(email, password, Role.USER);
    }

    /** Создаёт пользователя с ролью USER или ADMIN; {@code null} — USER. Только для ADMIN. */
    public Mono<User> register(String email, String password, Role role);

    public Mono<User> getById(long id);

    public Mono<Page<User>> getPage(Pageable pageable);

    public Mono<User> update(long id, String newEmail, String newPassword);

    public Mono<Void> delete(long id);

    /**
     * Назначает роль USER или ADMIN другому пользователю (так же снимается роль TRAINER).
     * Только для ADMIN; свою роль менять нельзя, TRAINER выдаётся только через сертификат.
     */
    public Mono<User> changeRole(long id, Role role);
}
