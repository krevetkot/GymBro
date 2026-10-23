package ru.itmo.gymbro.identity.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.itmo.gymbro.identity.api.UserDirectory;
import ru.itmo.gymbro.identity.api.UserSnapshot;
import ru.itmo.gymbro.shared.api.CurrentUserProvider;
import ru.itmo.gymbro.shared.api.ForbiddenException;
import ru.itmo.gymbro.shared.api.NotFoundException;
import ru.itmo.gymbro.shared.api.UnauthorizedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RemoteCurrentUserAccessTest {

    private final CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
    private final UserDirectory users = mock(UserDirectory.class);
    private final RemoteCurrentUserAccess access = new RemoteCurrentUserAccess(currentUser, users);

    @Test
    @DisplayName("Активный пользователь проходит проверку, но прав администратора не получает")
    void activeUserIsNotAdmin() {
        givenCurrentUser(new UserSnapshot(7, true, false));

        assertThat(access.requireActiveUser()).isEqualTo(7);
        assertThatThrownBy(access::requireAdmin)
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Операция доступна только администратору");
    }

    @Test
    @DisplayName("Активный администратор проходит обе проверки")
    void activeAdminPasses() {
        givenCurrentUser(new UserSnapshot(7, true, true));

        assertThat(access.requireActiveUser()).isEqualTo(7);
        assertThat(access.requireAdmin()).isEqualTo(7);
    }

    @Test
    @DisplayName("Заблокированному пользователю и администратору — 403")
    void bannedUserIsForbidden() {
        givenCurrentUser(new UserSnapshot(7, false, true));

        assertThatThrownBy(access::requireActiveUser).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(access::requireAdmin).isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Пользователь, которого identity не знает, не аутентифицирован")
    void unknownUserIsUnauthorized() {
        when(currentUser.currentUserId()).thenReturn(7L);
        when(users.getSnapshot(7)).thenThrow(new NotFoundException("Пользователь 7 не найден"));

        assertThatThrownBy(access::requireAdmin)
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Текущий пользователь не найден");
    }

    private void givenCurrentUser(UserSnapshot user) {
        when(currentUser.currentUserId()).thenReturn(user.id());
        when(users.getSnapshot(user.id())).thenReturn(user);
    }
}
