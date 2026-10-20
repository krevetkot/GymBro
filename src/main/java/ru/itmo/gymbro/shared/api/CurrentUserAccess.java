package ru.itmo.gymbro.shared.api;

public interface CurrentUserAccess {

    public long requireActiveUser();

    public long requireAdmin();
}

