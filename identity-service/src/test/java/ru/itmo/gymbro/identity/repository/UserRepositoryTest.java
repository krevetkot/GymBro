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

import java.util.List;

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
        assertThat(found.getPassword()).isEqualTo("hash");
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
    @DisplayName("Удаление мягкое: строка остаётся, но пользователь пропадает из поиска и из активных")
    void softDeletesUser() {
        User user = users.save(User.register("temp@mail.ru", "hash"));

        user.delete();
        users.save(user);

        assertThat(users.findById(user.getId())).isPresent();
        assertThat(users.findByIdAndDeletedFalse(user.getId())).isEmpty();
        assertThat(users.findAllByDeletedFalse(PageRequest.of(0, 50)).getContent()).doesNotContain(user);
        assertThat(users.findActiveIds(List.of(user.getId()))).isEmpty();
        assertThat(user.isActive()).isFalse();
    }

    @Test
    @DisplayName("Из переданных id активными считаются только существующие, не удалённые и не заблокированные")
    void findsActiveIdsAmongGiven() {
        long active = users.save(User.register("active@mail.ru", "hash")).getId();
        User bannedUser = User.register("banned@mail.ru", "hash");
        bannedUser.ban();
        long banned = users.save(bannedUser).getId();
        User deletedUser = User.register("deleted@mail.ru", "hash");
        deletedUser.delete();
        long deleted = users.save(deletedUser).getId();
        long otherActive = users.save(User.register("other@mail.ru", "hash")).getId();

        assertThat(users.findActiveIds(List.of(active, banned, deleted, Long.MAX_VALUE)))
                .containsExactly(active);
        assertThat(users.findActiveIds(List.of(banned, deleted))).isEmpty();
        assertThat(users.findActiveIds(List.of(active))).doesNotContain(otherActive);
    }

    @Test
    @DisplayName("Миграция создала тестового администратора с паролем admin12345")
    void seedsTestAdmin() {
        User admin = users.findByEmail("admin@gymbro.local").orElseThrow();

        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(admin.getPassword()).isEqualTo("admin12345");
    }
}
