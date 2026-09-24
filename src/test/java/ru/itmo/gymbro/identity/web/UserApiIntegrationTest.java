package ru.itmo.gymbro.identity.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserApiIntegrationTest extends AbstractIntegrationTest {

    private static final String USERS = "/api/v1/users";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository users;

    private MockMvc mvc;

    @BeforeEach
    void setUpMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("Регистрация возвращает 201, адрес пользователя и тело без пароля")
    void registersUser() throws Exception {
        register("Anna@Mail.ru", "secret-password")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/v1/users/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("anna@mail.ru"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        User stored = users.findByEmail("anna@mail.ru").orElseThrow();
        assertThat(stored.getPasswordHash()).isNotEqualTo("secret-password").startsWith("$2");
    }

    @Test
    @DisplayName("Зарегистрированный пользователь читается по адресу из Location")
    void readsRegisteredUser() throws Exception {
        String location = registeredLocation("reader@mail.ru");

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("reader@mail.ru"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Повторная регистрация на занятый email — 409 без учёта регистра")
    void rejectsDuplicateEmail() throws Exception {
        registeredLocation("twin@mail.ru");

        register("TWIN@mail.ru", "another-password")
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    @DisplayName("Невалидное тело регистрации — 400 со списком всех неверных полей")
    void rejectsInvalidRegistration() throws Exception {
        register("not-an-email", "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("email", "password")));

        assertThat(users.existsByEmail("not-an-email")).isFalse();
    }

    @Test
    @DisplayName("Регистрация без обязательных полей — 400")
    void rejectsEmptyRegistration() throws Exception {
        mvc.perform(post(USERS).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("email", "password")));
    }

    @Test
    @DisplayName("Нечитаемый JSON — 400")
    void rejectsMalformedJson() throws Exception {
        mvc.perform(post(USERS).contentType(MediaType.APPLICATION_JSON).content("{\"email\":"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Несуществующий пользователь — 404")
    void returnsNotFoundForUnknownUser() throws Exception {
        mvc.perform(get(USERS + "/" + Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "0", "-1"})
    @DisplayName("Идентификатор в пути не положительное число — 400")
    void rejectsInvalidIdentifier(String id) throws Exception {
        mvc.perform(get(USERS + "/" + id)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Список отдаётся страницами, общее количество — в X-Total-Count")
    void pagesUsersWithTotalCount() throws Exception {
        long before = users.findAll(PageRequest.of(0, 1)).getTotalElements();
        saveUsers("paged", 3);

        mvc.perform(get(USERS).param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", String.valueOf(before + 3)))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Больше 50 записей за запрос не отдаётся")
    void capsPageSizeAtFifty() throws Exception {
        saveUsers("bulk", 51);

        mvc.perform(get(USERS).param("size", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(50)));
    }

    @Test
    @DisplayName("Страницы идут по возрастанию id и не пересекаются, сортировка из запроса игнорируется")
    void ordersPagesById() throws Exception {
        saveUsers("ordered", 4);

        List<Integer> first = ids(mvc.perform(get(USERS).param("page", "0").param("size", "2")
                .param("sort", "passwordHash,desc")));
        List<Integer> second = ids(mvc.perform(get(USERS).param("page", "1").param("size", "2")
                .param("sort", "unknown")));

        assertThat(first).isSorted();
        assertThat(second).isSorted();
        assertThat(first.get(1)).isLessThan(second.get(0));
    }

    @Test
    @DisplayName("PATCH меняет только переданные поля")
    void updatesEmail() throws Exception {
        String location = registeredLocation("old@mail.ru");

        mvc.perform(patch(location).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"New@Mail.ru\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@mail.ru"));

        assertThat(users.existsByEmail("old@mail.ru")).isFalse();
        assertThat(users.existsByEmail("new@mail.ru")).isTrue();
    }

    @Test
    @DisplayName("PATCH пароля сохраняет новый хэш")
    void updatesPassword() throws Exception {
        String location = registeredLocation("secure@mail.ru");
        String oldHash = users.findByEmail("secure@mail.ru").orElseThrow().getPasswordHash();

        mvc.perform(patch(location).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"brand-new-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("secure@mail.ru"));

        String newHash = users.findByEmail("secure@mail.ru").orElseThrow().getPasswordHash();
        assertThat(newHash).isNotEqualTo(oldHash).startsWith("$2");
    }

    @Test
    @DisplayName("PATCH на email другого пользователя — 409")
    void rejectsTakenEmailOnUpdate() throws Exception {
        registeredLocation("taken@mail.ru");
        String location = registeredLocation("mover@mail.ru");

        mvc.perform(patch(location).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"taken@mail.ru\"}"))
                .andExpect(status().isConflict());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"email\":\"broken\"}", "{\"password\":\"short\"}", "{\"password\":\"          \"}"})
    @DisplayName("PATCH с невалидными полями — 400")
    void rejectsInvalidUpdate(String body) throws Exception {
        String location = registeredLocation("patched@mail.ru");

        mvc.perform(patch(location).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("PATCH несуществующего пользователя — 404")
    void rejectsUpdateOfUnknownUser() throws Exception {
        mvc.perform(patch(USERS + "/" + Long.MAX_VALUE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ghost@mail.ru\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Удаление — 204, после него пользователь не найден, повторное удаление — 404")
    void deletesUser() throws Exception {
        String location = registeredLocation("gone@mail.ru");

        mvc.perform(delete(location)).andExpect(status().isNoContent());
        mvc.perform(get(location)).andExpect(status().isNotFound());
        mvc.perform(delete(location)).andExpect(status().isNotFound());
    }

    private ResultActions register(String email, String password) throws Exception {
        return mvc.perform(post(USERS).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
    }

    private String registeredLocation(String email) throws Exception {
        return register(email, "secret-password")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith(USERS)))
                .andReturn().getResponse().getHeader("Location");
    }

    private void saveUsers(String prefix, int count) {
        for (int index = 0; index < count; index++) {
            users.save(User.register(prefix + index + "@mail.ru", "hash"));
        }
    }

    private static List<Integer> ids(ResultActions result) throws Exception {
        String json = result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$[*].id");
    }
}
