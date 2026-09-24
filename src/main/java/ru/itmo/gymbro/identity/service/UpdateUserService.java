package ru.itmo.gymbro.identity.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.gymbro.identity.api.UpdateUserUseCase;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.UserRepository;
import ru.itmo.gymbro.shared.api.ConflictException;
import ru.itmo.gymbro.shared.api.NotFoundException;

@Service
class UpdateUserService implements UpdateUserUseCase {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    UpdateUserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User update(long id, String newEmail, String newRawPassword) {
        User user = users.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь " + id + " не найден"));
        if (newEmail != null) {
            String currentEmail = user.getEmail();
            user.changeEmail(newEmail);
            if (!user.getEmail().equals(currentEmail) && users.existsByEmail(user.getEmail())) {
                throw new ConflictException("Пользователь с email " + user.getEmail() + " уже зарегистрирован");
            }
        }
        if (newRawPassword != null) {
            if (newRawPassword.isBlank()) {
                throw new IllegalArgumentException("Пароль обязателен");
            }
            user.changePassword(passwordEncoder.encode(newRawPassword));
        }
        return users.save(user);
    }
}
