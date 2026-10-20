package ru.itmo.gymbro.identity.service;

import org.junit.jupiter.api.Test;
import ru.itmo.gymbro.identity.model.Role;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.model.UserStatus;
import ru.itmo.gymbro.identity.repository.UserRepository;
import ru.itmo.gymbro.shared.api.CurrentUserProvider;
import ru.itmo.gymbro.shared.api.ForbiddenException;
import ru.itmo.gymbro.shared.api.UnauthorizedException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
    private final UserRepository users = mock(UserRepository.class);
    private final UserService service = new UserService(users, currentUser);

    @Test
    void acceptsActiveUserButDoesNotGrantAdminRights() {
        givenUser(Role.USER, UserStatus.ACTIVE);
        assertThat(service.requireActiveUser()).isEqualTo(7L);
        assertThatThrownBy(service::requireAdmin).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void acceptsActiveAdmin() {
        givenUser(Role.ADMIN, UserStatus.ACTIVE);
        assertThat(service.requireAdmin()).isEqualTo(7L);
    }

    @Test
    void rejectsBannedAdminAsWellAsBannedUser() {
        givenUser(Role.ADMIN, UserStatus.BANNED);
        assertThatThrownBy(service::requireActiveUser).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(service::requireAdmin).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void rejectsUnknownCurrentUser() {
        when(currentUser.currentUserId()).thenReturn(7L);
        when(users.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(service::requireActiveUser).isInstanceOf(UnauthorizedException.class);
    }

    private void givenUser(Role role, UserStatus status) {
        when(currentUser.currentUserId()).thenReturn(7L);
        when(users.findById(7L)).thenReturn(Optional.of(
                new User(7L, "user@example.com", "hash", role, status, Instant.now())));
    }
}

