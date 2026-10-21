package ru.itmo.gymbro.identity.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.UserRepository;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

class InternalUserApiIntegrationTest extends AbstractIntegrationTest {

    private static final String INTERNAL_USERS = "/internal/v1/users";

    @Autowired
    private UserRepository users;

    private String marker;

    @BeforeEach
    void setUp() {
        marker = UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    @DisplayName("Снапшот пользователя содержит только id, активность и признак админа")
    void returnsSnapshot() {
        long id = users.save(User.register(marker + "@mail.ru", "hash")).getId();

        client.get().uri(INTERNAL_USERS + "/" + id).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(id)
                .jsonPath("$.active").isEqualTo(true)
                .jsonPath("$.admin").isEqualTo(false)
                .jsonPath("$.email").doesNotExist()
                .jsonPath("$.password").doesNotExist();
    }

    @Test
    @DisplayName("Снапшот несуществующего пользователя — 404")
    void missingUser() {
        client.get().uri(INTERNAL_USERS + "/" + Long.MAX_VALUE).exchange().expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Снапшот удалённого пользователя — 404, как будто его нет")
    void deletedUserIsMissing() {
        User user = users.save(User.register(marker + "-deleted@mail.ru", "hash"));
        user.delete();
        users.save(user);

        client.get().uri(INTERNAL_USERS + "/" + user.getId()).exchange().expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Список недоступных содержит забаненных и удалённых, но не активных")
    void listsBannedIds() {
        User bannedUser = User.register(marker + "-banned@mail.ru", "hash");
        bannedUser.ban();
        long banned = users.save(bannedUser).getId();
        User deletedUser = User.register(marker + "-gone@mail.ru", "hash");
        deletedUser.delete();
        long deleted = users.save(deletedUser).getId();
        long active = users.save(User.register(marker + "-active@mail.ru", "hash")).getId();

        client.get().uri(INTERNAL_USERS + "/banned-ids").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").value(hasItem((int) banned))
                .jsonPath("$").value(hasItem((int) deleted))
                .jsonPath("$").value(not(hasItem((int) active)));
    }
}
