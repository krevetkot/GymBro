package ru.itmo.gymbro.identity.web;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.itmo.gymbro.identity.api.UserDirectory;
import ru.itmo.gymbro.identity.api.UserSnapshot;

import java.util.Set;

@Hidden
@RestController
@RequestMapping("/internal/v1/users")
class InternalUserController {

    private final UserDirectory users;

    public InternalUserController(UserDirectory users) {
        this.users = users;
    }

    @GetMapping("/{id}")
    Mono<UserSnapshot> getSnapshot(@PathVariable @Positive long id) {
        return users.getSnapshot(id);
    }

    @GetMapping("/banned-ids")
    Mono<Set<Long>> bannedUserIds() {
        return users.bannedUserIds();
    }
}
