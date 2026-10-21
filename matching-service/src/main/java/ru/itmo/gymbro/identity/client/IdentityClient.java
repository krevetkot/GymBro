package ru.itmo.gymbro.identity.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.itmo.gymbro.identity.api.UserSnapshot;

import java.util.Set;

@FeignClient(name = "identity-service")
public interface IdentityClient {

    @GetMapping("/internal/v1/users/{id}")
    UserSnapshot getSnapshot(@PathVariable("id") long id);

    @GetMapping("/internal/v1/users/banned-ids")
    Set<Long> bannedUserIds();
}
