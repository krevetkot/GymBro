package ru.itmo.gymbro.identity.service;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.itmo.gymbro.identity.model.Role;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.model.UserStatus;
import ru.itmo.gymbro.identity.repository.UserRepository;
import ru.itmo.gymbro.shared.api.CurrentUserProvider;
import ru.itmo.gymbro.shared.api.ForbiddenException;
import ru.itmo.gymbro.shared.api.UnauthorizedException;
import ru.itmo.gymbro.shared.persistence.BlockingJpa;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
    private final UserRepository users = mock(UserRepository.class);
    // менеджер транзакций-заглушка: TransactionTemplate просто выполняет работу
    private final UserService service = new UserService(users, currentUser,
            new BlockingJpa(mock(PlatformTransactionManager.class)));

    @Test
    void acceptsActiveUserButDoesNotGrantAdminRights() {
        givenUser(Role.USER, UserStatus.ACTIVE);
        StepVerifier.create(service.requireActiveUser()).expectNext(7L).verifyComplete();
        StepVerifier.create(service.requireAdmin()).verifyError(ForbiddenException.class);
    }

    @Test
    void acceptsActiveAdmin() {
        givenUser(Role.ADMIN, UserStatus.ACTIVE);
        StepVerifier.create(service.requireAdmin()).expectNext(7L).verifyComplete();
    }

    @Test
    void rejectsBannedAdminAsWellAsBannedUser() {
        givenUser(Role.ADMIN, UserStatus.BANNED);
        StepVerifier.create(service.requireActiveUser()).verifyError(ForbiddenException.class);
        StepVerifier.create(service.requireAdmin()).verifyError(ForbiddenException.class);
    }

    @Test
    void rejectsUnknownCurrentUser() {
        when(currentUser.currentUserId()).thenReturn(Mono.just(7L));
        when(users.findByIdAndDeletedFalse(7L)).thenReturn(Optional.empty());
        StepVerifier.create(service.requireActiveUser()).verifyError(UnauthorizedException.class);
    }

    @Test
    void doesNotQueryUsersWithoutCurrentUser() {
        when(currentUser.currentUserId()).thenReturn(Mono.error(new UnauthorizedException("Нет X-User-Id")));
        StepVerifier.create(service.requireAdmin()).verifyError(UnauthorizedException.class);
        verifyNoInteractions(users);
    }

    private void givenUser(Role role, UserStatus status) {
        when(currentUser.currentUserId()).thenReturn(Mono.just(7L));
        when(users.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(
                new User(7L, "user@example.com", "hash", role, status, Instant.now())));
    }
}

