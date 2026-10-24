package ru.itmo.gymbro.identity.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.itmo.gymbro.identity.model.Role;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.model.UserStatus;

import java.time.Instant;

@Getter
@RequiredArgsConstructor
public class UserResponse {

    private final long id;
    private final String email;
    private final Role role;
    private final UserStatus status;
    private final Instant createdAt;

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getStatus(), user.getCreatedAt());
    }
}
