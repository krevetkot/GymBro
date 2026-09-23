package ru.itmo.gymbro.identity.api;

import ru.itmo.gymbro.identity.model.User;

public interface RegisterUserUseCase {

    User register(String email, String rawPassword);
}
