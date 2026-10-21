package ru.itmo.gymbro.identity.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;
import ru.itmo.gymbro.identity.model.User;

public interface UserUseCases {

    public Mono<User> register(String email, String password);

    public Mono<User> getById(long id);

    public Mono<Page<User>> getPage(Pageable pageable);

    public Mono<User> update(long id, String newEmail, String newPassword);

    public Mono<Void> delete(long id);
}
