package ru.itmo.gymbro.identity.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.identity.model.Role;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.model.UserStatus;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository users;

    @Test
    @DisplayName("Пользователь сохраняется и читается обратно")
    void savesAndReadsUser() {
        User saved = users.save(User.register("Ksenia@Mail.RU", "hash"));

        assertThat(saved.getId()).isNotNull();

        User found = users.findById(saved.getId()).orElseThrow();
        assertThat(found.getEmail()).isEqualTo("ksenia@mail.ru");
        assertThat(found.getPasswordHash()).isEqualTo("hash");
        assertThat(found.getRole()).isEqualTo(Role.USER);
        assertThat(found.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Поиск по email не зависит от регистра")
    void findsByEmailIgnoringCase() {
        users.save(User.register("anna@mail.ru", "hash"));

        assertThat(users.findByEmail("ANNA@mail.ru")).isPresent();
        assertThat(users.existsByEmail("Anna@Mail.Ru")).isTrue();
        assertThat(users.existsByEmail("nobody@mail.ru")).isFalse();
    }

    @Test
    @DisplayName("Роль и статус переживают сохранение")
    void storesRoleAndStatus() {
        User user = users.save(User.register("trainer@mail.ru", "hash"));
        user.promoteToTrainer();
        user.ban();
        users.save(user);

        User reloaded = users.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getRole()).isEqualTo(Role.TRAINER);
        assertThat(reloaded.getStatus()).isEqualTo(UserStatus.BANNED);
        assertThat(reloaded.isActive()).isFalse();
    }

    @Test
    @DisplayName("Список пользователей отдаётся постранично")
    void listsUsersByPages() {
        for (int index = 0; index < 5; index++) {
            users.save(User.register("user" + index + "@mail.ru", "hash"));
        }

        Page<User> firstPage = users.findAll(PageRequest.of(0, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("Пользователь удаляется")
    void deletesUser() {
        User user = users.save(User.register("temp@mail.ru", "hash"));

        users.deleteById(user.getId());

        assertThat(users.existsById(user.getId())).isFalse();
    }
}
