package ru.itmo.gymbro.identity.api;

public interface CurrentUserAccess {

    long requireActiveUser();

    long requireAdmin();
}

