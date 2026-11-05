package ru.itmo.gymbro.identity.web;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.itmo.gymbro.identity.api.UserDirectory;
import ru.itmo.gymbro.identity.api.UserSnapshot;

import java.util.Set;

@Hidden
@RestController
@RequestMapping("/internal/v1/users")
@RequiredArgsConstructor
class InternalUserController {

    private final UserDirectory users;

    @GetMapping("/{id}")
    Mono<UserSnapshot> getSnapshot(@PathVariable @Positive long id) {
        return users.getSnapshot(id);
    }

    @GetMapping("/banned-ids")
    Mono<Set<Long>> bannedUserIds() {
        return users.bannedUserIds();
    }

    /** Для ленты: кто из кандидатов на странице — тренер. Не больше 50 идентификаторов, как и размер страницы. */
    @GetMapping("/trainer-ids")
    Mono<Set<Long>> trainerIds(@RequestParam(defaultValue = "") @Size(max = 50) Set<@Positive Long> ids) {
        return users.trainerIds(ids);
    }
}
