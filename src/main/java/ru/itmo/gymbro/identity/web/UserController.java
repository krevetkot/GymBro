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
import ru.itmo.gymbro.identity.api.DeleteUserUseCase;
import ru.itmo.gymbro.identity.api.GetUserUseCase;
import ru.itmo.gymbro.identity.api.RegisterUserUseCase;
import ru.itmo.gymbro.identity.api.UpdateUserUseCase;
import ru.itmo.gymbro.identity.dto.RegisterUserRequest;
import ru.itmo.gymbro.identity.dto.UpdateUserRequest;
import ru.itmo.gymbro.identity.dto.UserResponse;
import ru.itmo.gymbro.shared.web.PageResponses;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
class UserController {

    private final RegisterUserUseCase registerUser;
    private final GetUserUseCase getUser;
    private final UpdateUserUseCase updateUser;
    private final DeleteUserUseCase deleteUser;

    UserController(RegisterUserUseCase registerUser, GetUserUseCase getUser,
                   UpdateUserUseCase updateUser, DeleteUserUseCase deleteUser) {
        this.registerUser = registerUser;
        this.getUser = getUser;
        this.updateUser = updateUser;
        this.deleteUser = deleteUser;
    }

    @PostMapping
    ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        UserResponse response = UserResponse.from(registerUser.register(request.getEmail(), request.getPassword()));
        return ResponseEntity.created(URI.create("/api/v1/users/" + response.getId())).body(response);
    }

    @GetMapping("/{id}")
    UserResponse getById(@PathVariable @Positive long id) {
        return UserResponse.from(getUser.getById(id));
    }

    @GetMapping
    ResponseEntity<List<UserResponse>> getPage(Pageable pageable) {
        return PageResponses.withTotalCount(getUser.getPage(pageable).map(UserResponse::from));
    }

    @PatchMapping("/{id}")
    UserResponse update(@PathVariable @Positive long id, @Valid @RequestBody UpdateUserRequest request) {
        return UserResponse.from(updateUser.update(id, request.getEmail(), request.getPassword()));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable @Positive long id) {
        deleteUser.delete(id);
        return ResponseEntity.noContent().build();
    }
}
