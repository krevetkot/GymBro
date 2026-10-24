package ru.itmo.gymbro.identity.client;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactor.core.publisher.Mono;
import ru.itmo.gymbro.identity.api.UserSnapshot;

import java.util.Set;

@ReactiveFeignClient(name = "identity-service", url = "${gymbro.clients.identity-url:}")
public interface IdentityClient {

    @GetMapping("/internal/v1/users/{id}")
    Mono<UserSnapshot> getSnapshot(@PathVariable("id") long id);

    @GetMapping("/internal/v1/users/banned-ids")
    Mono<Set<Long>> bannedUserIds();
}
