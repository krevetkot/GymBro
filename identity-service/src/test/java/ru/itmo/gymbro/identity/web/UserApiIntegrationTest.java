package ru.itmo.gymbro.identity.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;

class UserApiIntegrationTest extends AbstractIntegrationTest {

    private static final String USERS = "/api/v1/users";

    @Autowired
    private UserRepository users;

    @Test
    @DisplayName("Регистрация возвращает 201, адрес пользователя и тело без пароля")
    void registersUser() {
        register("Anna@Mail.ru", "secret-password")
                .expectStatus().isCreated()
                .expectHeader().value("Location", matchesPattern("/api/v1/users/\\d+"))
                .expectBody()
                .jsonPath("$.id").isNumber()
                .jsonPath("$.email").isEqualTo("anna@mail.ru")
                .jsonPath("$.role").isEqualTo("USER")
                .jsonPath("$.status").isEqualTo("ACTIVE")
                .jsonPath("$.createdAt").isNotEmpty()
                .jsonPath("$.password").doesNotExist();

        User stored = users.findByEmail("anna@mail.ru").orElseThrow();
        assertThat(stored.getPassword()).isEqualTo("secret-password");
    }

    @Test
    @DisplayName("Зарегистрированный пользователь читается по адресу из Location")
    void readsRegisteredUser() {
        String location = registeredLocation("reader@mail.ru");

        client.get().uri(location).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.email").isEqualTo("reader@mail.ru")
                .jsonPath("$.password").doesNotExist();
    }

    @Test
    @DisplayName("Повторная регистрация на занятый email — 409 без учёта регистра")
    void rejectsDuplicateEmail() {
        registeredLocation("twin@mail.ru");

        register("TWIN@mail.ru", "another-password")
                .expectStatus().isEqualTo(409)
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.detail").isNotEmpty();
    }

    @Test
    @DisplayName("Невалидное тело регистрации — 400 со списком всех неверных полей")
    void rejectsInvalidRegistration() {
        register("not-an-email", "short")
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.errors[*].field").value(containsInAnyOrder("email", "password"));

        assertThat(users.existsByEmail("not-an-email")).isFalse();
    }

    @Test
    @DisplayName("Регистрация без обязательных полей — 400")
    void rejectsEmptyRegistration() {
        post("{}")
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.errors[*].field").value(containsInAnyOrder("email", "password"));
    }

    @Test
    @DisplayName("Нечитаемый JSON — 400")
    void rejectsMalformedJson() {
        post("{\"email\":").expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Несуществующий пользователь — 404")
    void returnsNotFoundForUnknownUser() {
        client.get().uri(USERS + "/" + Long.MAX_VALUE).exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.detail").isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "0", "-1"})
    @DisplayName("Идентификатор в пути не положительное число — 400")
    void rejectsInvalidIdentifier(String id) {
        client.get().uri(USERS + "/" + id).exchange().expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Список отдаётся страницами, общее количество — в X-Total-Count")
    void pagesUsersWithTotalCount() {
        long before = users.count();
        saveUsers("paged", 3);

        client.get().uri(USERS + "?page=0&size=2").exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Total-Count", String.valueOf(before + 3))
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].password").doesNotExist();
    }

    @Test
    @DisplayName("Больше 50 записей за запрос не отдаётся")
    void capsPageSizeAtFifty() {
        saveUsers("bulk", 51);

        client.get().uri(USERS + "?size=1000").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(50);
    }

    @Test
    @DisplayName("Страницы идут по возрастанию id и не пересекаются, сортировка из запроса игнорируется")
    void ordersPagesById() {
        saveUsers("ordered", 4);

        List<Integer> first = ids(USERS + "?page=0&size=2&sort=password,desc");
        List<Integer> second = ids(USERS + "?page=1&size=2&sort=unknown");

        assertThat(first).isSorted();
        assertThat(second).isSorted();
        assertThat(first.get(1)).isLessThan(second.get(0));
    }

    @Test
    @DisplayName("PATCH меняет только переданные поля")
    void updatesEmail() {
        String location = registeredLocation("old@mail.ru");

        patch(location, "{\"email\":\"New@Mail.ru\"}")
                .expectStatus().isOk()
                .expectBody().jsonPath("$.email").isEqualTo("new@mail.ru");

        assertThat(users.existsByEmail("old@mail.ru")).isFalse();
        assertThat(users.existsByEmail("new@mail.ru")).isTrue();
    }

    @Test
    @DisplayName("PATCH пароля сохраняет новый пароль")
    void updatesPassword() {
        String location = registeredLocation("secure@mail.ru");

        patch(location, "{\"password\":\"brand-new-password\"}")
                .expectStatus().isOk()
                .expectBody().jsonPath("$.email").isEqualTo("secure@mail.ru");

        assertThat(users.findByEmail("secure@mail.ru").orElseThrow().getPassword()).isEqualTo("brand-new-password");
    }

    @Test
    @DisplayName("PATCH на email другого пользователя — 409")
    void rejectsTakenEmailOnUpdate() {
        registeredLocation("taken@mail.ru");
        String location = registeredLocation("mover@mail.ru");

        patch(location, "{\"email\":\"taken@mail.ru\"}").expectStatus().isEqualTo(409);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"email\":\"broken\"}", "{\"password\":\"short\"}", "{\"password\":\"          \"}"})
    @DisplayName("PATCH с невалидными полями — 400")
    void rejectsInvalidUpdate(String body) {
        String location = registeredLocation("patched@mail.ru");

        patch(location, body)
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.errors").isArray();
    }

    @Test
    @DisplayName("PATCH несуществующего пользователя — 404")
    void rejectsUpdateOfUnknownUser() {
        patch(USERS + "/" + Long.MAX_VALUE, "{\"email\":\"ghost@mail.ru\"}").expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Удаление — 204, после него пользователь не найден, повторное удаление — 404")
    void deletesUser() {
        String location = registeredLocation("gone@mail.ru");

        client.delete().uri(location).exchange().expectStatus().isNoContent();
        client.get().uri(location).exchange().expectStatus().isNotFound();
        client.delete().uri(location).exchange().expectStatus().isNotFound();
    }

    private WebTestClient.ResponseSpec register(String email, String password) {
        return post("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password));
    }

    private WebTestClient.ResponseSpec post(String body) {
        return client.post().uri(USERS).contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange();
    }

    private WebTestClient.ResponseSpec patch(String location, String body) {
        return client.patch().uri(location).contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange();
    }

    private String registeredLocation(String email) {
        return register(email, "secret-password")
                .expectStatus().isCreated()
                .expectHeader().value("Location", startsWith(USERS))
                .returnResult(String.class).getResponseHeaders().getFirst("Location");
    }

    private void saveUsers(String prefix, int count) {
        for (int index = 0; index < count; index++) {
            users.save(User.register(prefix + index + "@mail.ru", "hash"));
        }
    }

    private List<Integer> ids(String uri) {
        String json = client.get().uri(uri).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
        return JsonPath.read(json, "$[*].id");
    }
}
