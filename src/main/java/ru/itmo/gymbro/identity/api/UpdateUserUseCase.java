package ru.itmo.gymbro.identity.api;

import ru.itmo.gymbro.identity.model.User;

public interface UpdateUserUseCase {

    User update(long id, String newEmail, String newRawPassword);
}
