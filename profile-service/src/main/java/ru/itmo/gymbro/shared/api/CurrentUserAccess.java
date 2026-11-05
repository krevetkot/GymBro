package ru.itmo.gymbro.shared.api;

public interface CurrentUserAccess {

    public long requireActiveUser();

    public long requireAdmin();

    /** Активный пользователь с ролью TRAINER, то есть с одобренным сертификатом. */
    public long requireTrainer();
}

