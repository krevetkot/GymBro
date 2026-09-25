package ru.itmo.gymbro.identity.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.itmo.gymbro.identity.model.User;

import java.util.Set;

public interface UserUseCases {

    public User register(String email, String password);

    public User getById(long id);

    public Page<User> getPage(Pageable pageable);

    public User update(long id, String newEmail, String newPassword);

    public void delete(long id);

    public Set<Long> bannedUserIds();
}
