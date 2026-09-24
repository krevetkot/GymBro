package ru.itmo.gymbro.identity.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.gymbro.identity.api.ListBannedUsersUseCase;
import ru.itmo.gymbro.identity.repository.UserRepository;

import java.util.Set;

@Service
@Transactional(readOnly = true)
class ListBannedUsersService implements ListBannedUsersUseCase {

    private final UserRepository users;

    ListBannedUsersService(UserRepository users) {
        this.users = users;
    }

    @Override
    public Set<Long> bannedUserIds() {
        return Set.copyOf(users.findBannedIds());
    }
}
