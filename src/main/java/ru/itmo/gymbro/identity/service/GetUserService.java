package ru.itmo.gymbro.identity.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.gymbro.identity.api.GetUserUseCase;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.UserRepository;
import ru.itmo.gymbro.shared.api.NotFoundException;

@Service
@Transactional(readOnly = true)
class GetUserService implements GetUserUseCase {

    private final UserRepository users;

    GetUserService(UserRepository users) {
        this.users = users;
    }

    @Override
    public User getById(long id) {
        return users.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь " + id + " не найден"));
    }

    @Override
    public Page<User> getPage(Pageable pageable) {
        return users.findAll(pageable);
    }
}
