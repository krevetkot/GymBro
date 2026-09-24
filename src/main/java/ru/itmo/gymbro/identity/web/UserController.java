package ru.itmo.gymbro.identity.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.gymbro.identity.api.UserUseCases;
import ru.itmo.gymbro.identity.dto.RegisterUserRequest;
import ru.itmo.gymbro.identity.dto.UpdateUserRequest;
import ru.itmo.gymbro.identity.dto.UserResponse;
import ru.itmo.gymbro.shared.web.PageResponses;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
class UserController {

    private final UserUseCases users;

    UserController(UserUseCases users) {
        this.users = users;
    }

    @PostMapping
    ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        UserResponse response = UserResponse.from(users.register(request.getEmail(), request.getPassword()));
        return ResponseEntity.created(URI.create("/api/v1/users/" + response.getId())).body(response);
    }

    @GetMapping("/{id}")
    UserResponse getById(@PathVariable @Positive long id) {
        return UserResponse.from(users.getById(id));
    }

    @GetMapping
    ResponseEntity<List<UserResponse>> getPage(Pageable pageable) {
        return PageResponses.withTotalCount(users.getPage(pageable).map(UserResponse::from));
    }

    @PatchMapping("/{id}")
    UserResponse update(@PathVariable @Positive long id, @Valid @RequestBody UpdateUserRequest request) {
        return UserResponse.from(users.update(id, request.getEmail(), request.getPassword()));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable @Positive long id) {
        users.delete(id);
        return ResponseEntity.noContent().build();
    }
}
