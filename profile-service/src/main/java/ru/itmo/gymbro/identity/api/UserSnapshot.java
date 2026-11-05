package ru.itmo.gymbro.identity.api;

/**
 * Снимок пользователя из identity-service. Роль TRAINER выдаётся только одобрением сертификата,
 * поэтому {@link #isTrainer()} и есть признак «сертификат подтверждён».
 */
public record UserSnapshot(long id, boolean active, boolean admin, Role role) {

    public boolean isTrainer() {
        return role == Role.TRAINER;
    }
}
