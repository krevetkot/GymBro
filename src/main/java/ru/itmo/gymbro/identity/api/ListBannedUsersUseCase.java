package ru.itmo.gymbro.identity.api;

import java.util.Set;

public interface ListBannedUsersUseCase {

    Set<Long> bannedUserIds();
}
