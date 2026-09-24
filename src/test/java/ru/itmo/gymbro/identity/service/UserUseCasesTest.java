package ru.itmo.gymbro.identity.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.identity.api.DeleteUserUseCase;
import ru.itmo.gymbro.identity.api.GetUserUseCase;
import ru.itmo.gymbro.identity.api.RegisterUserUseCase;
import ru.itmo.gymbro.identity.model.Role;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.model.UserStatus;
import ru.itmo.gymbro.shared.api.ConflictException;
import ru.itmo.gymbro.shared.api.NotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserUseCasesTest extends AbstractIntegrationTest {

    @Autowired
    private RegisterUserUseCase registerUser;

    @Autowired
    private GetUserUseCase getUser;

    @Autowired
    private DeleteUserUseCase deleteUser;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Регистрация создаёт активного пользователя с хэшем BCrypt вместо пароля")
    void registersUserWithHashedPassword() {
        User user = registerUser.register("New@Mail.ru", "secret-password");

        User stored = getUser.getById(user.getId());
        assertThat(stored.getEmail()).isEqualTo("new@mail.ru");
        assertThat(stored.getRole()).isEqualTo(Role.USER);
        assertThat(stored.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(stored.getPasswordHash()).isNotEqualTo("secret-password").startsWith("$2");
        assertThat(passwordEncoder.matches("secret-password", stored.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Повторная регистрация на тот же email без учёта регистра — конфликт")
    void rejectsDuplicateEmail() {
        registerUser.register("twin@mail.ru", "secret-password");

        assertThatThrownBy(() -> registerUser.register("TWIN@mail.ru", "other-password"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Регистрация без пароля отклоняется")
    void rejectsBlankPassword() {
        assertThatThrownBy(() -> registerUser.register("nopass@mail.ru", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Несуществующий пользователь не найден")
    void failsOnUnknownUser() {
        assertThatThrownBy(() -> getUser.getById(Long.MAX_VALUE))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Пользователи отдаются постранично")
    void pagesUsers() {
        for (int index = 0; index < 3; index++) {
            registerUser.register("paged" + index + "@mail.ru", "secret-password");
        }

        Page<User> page = getUser.getPage(PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Удалённого пользователя больше нет, повторное удаление — не найден")
    void deletesUser() {
        User user = registerUser.register("gone@mail.ru", "secret-password");

        deleteUser.delete(user.getId());

        assertThatThrownBy(() -> getUser.getById(user.getId())).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> deleteUser.delete(user.getId())).isInstanceOf(NotFoundException.class);
    }
}
