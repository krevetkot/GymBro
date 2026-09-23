package ru.itmo.gymbro.identity.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.itmo.gymbro.identity.model.User;

public interface GetUserUseCase {

    User getById(long id);

    Page<User> getPage(Pageable pageable);
}
