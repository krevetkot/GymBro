package ru.itmo.gymbro.identity.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.gymbro.identity.api.DeleteUserUseCase;
import ru.itmo.gymbro.identity.repository.UserRepository;
import ru.itmo.gymbro.shared.api.NotFoundException;

@Service
class DeleteUserService implements DeleteUserUseCase {

    private final UserRepository users;

    DeleteUserService(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional
    public void delete(long id) {
        if (!users.existsById(id)) {
            throw new NotFoundException("Пользователь " + id + " не найден");
        }
        users.deleteById(id);
    }
}
