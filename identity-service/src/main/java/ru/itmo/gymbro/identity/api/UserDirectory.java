package ru.itmo.gymbro.identity.api;

import reactor.core.publisher.Mono;

import java.util.Set;

public interface UserDirectory {

    public Mono<UserSnapshot> getSnapshot(long id);

    public Mono<Set<Long>> bannedUserIds();
}
