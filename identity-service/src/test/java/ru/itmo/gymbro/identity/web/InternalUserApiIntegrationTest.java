package ru.itmo.gymbro.identity.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.UserRepository;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalUserApiIntegrationTest extends AbstractIntegrationTest {

    private static final String INTERNAL_USERS = "/internal/v1/users";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository users;

    private MockMvc mvc;
    private String marker;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        marker = UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    @DisplayName("Снапшот пользователя содержит только id, активность и признак админа")
    void returnsSnapshot() throws Exception {
        long id = users.save(User.register(marker + "@mail.ru", "hash")).getId();

        mvc.perform(get(INTERNAL_USERS + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.admin").value(false))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("Снапшот несуществующего пользователя — 404")
    void missingUser() throws Exception {
        mvc.perform(get(INTERNAL_USERS + "/" + Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Снапшот удалённого пользователя — 404, как будто его нет")
    void deletedUserIsMissing() throws Exception {
        User user = users.save(User.register(marker + "-deleted@mail.ru", "hash"));
        user.delete();
        users.save(user);

        mvc.perform(get(INTERNAL_USERS + "/" + user.getId())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Список недоступных содержит забаненных и удалённых, но не активных")
    void listsBannedIds() throws Exception {
        User bannedUser = User.register(marker + "-banned@mail.ru", "hash");
        bannedUser.ban();
        long banned = users.save(bannedUser).getId();
        User deletedUser = User.register(marker + "-gone@mail.ru", "hash");
        deletedUser.delete();
        long deleted = users.save(deletedUser).getId();
        long active = users.save(User.register(marker + "-active@mail.ru", "hash")).getId();

        mvc.perform(get(INTERNAL_USERS + "/banned-ids"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem((int) banned)))
                .andExpect(jsonPath("$", hasItem((int) deleted)))
                .andExpect(jsonPath("$", not(hasItem((int) active))));
    }
}
