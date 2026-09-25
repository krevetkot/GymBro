package ru.itmo.gymbro.identity.api;

public interface CurrentUserAccess {

    public long requireActiveUser();

    public long requireAdmin();
}

