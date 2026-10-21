package ru.itmo.gymbro.identity.api;

import java.util.Set;

public interface UserDirectory {

    public UserSnapshot getSnapshot(long id);

    public Set<Long> bannedUserIds();
}
