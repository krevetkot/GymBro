package ru.itmo.gymbro.identity.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.itmo.gymbro.identity.model.User;

import java.util.Set;

public interface UserUseCases {

    User register(String email, String rawPassword);

    User getById(long id);

    Page<User> getPage(Pageable pageable);

    User update(long id, String newEmail, String newRawPassword);

    void delete(long id);

    Set<Long> bannedUserIds();
}
