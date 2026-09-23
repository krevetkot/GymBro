package ru.itmo.gymbro.identity.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.gymbro.identity.api.RegisterUserUseCase;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.UserRepository;
import ru.itmo.gymbro.shared.api.ConflictException;

@Service
class RegisterUserService implements RegisterUserUseCase {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    RegisterUserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User register(String email, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Пароль обязателен");
        }
        if (users.existsByEmail(email)) {
            throw new ConflictException("Пользователь с email " + email + " уже зарегистрирован");
        }
        return users.save(User.register(email, passwordEncoder.encode(rawPassword)));
    }
}
