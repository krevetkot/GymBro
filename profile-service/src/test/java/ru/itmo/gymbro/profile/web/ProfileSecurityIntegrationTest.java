package ru.itmo.gymbro.profile.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.TestTokens;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileSecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = securedMvc(context);
    }

    @Test
    @DisplayName("Без токена — 401 в формате ProblemDetail с WWW-Authenticate")
    void missingTokenIsProblemDetail() throws Exception {
        mvc.perform(request())
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Требуется аутентификация"));
    }

    @Test
    @DisplayName("Заголовок X-User-Id без токена больше не даёт доступа")
    void userIdHeaderIsIgnored() throws Exception {
        long user = testUsers.active();
        mvc.perform(request().header("X-User-Id", user)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"expired", "foreign-signature", "foreign-issuer", "garbage", "basic"})
    @DisplayName("Недействительный токен — 401")
    void rejectsInvalidTokens(String kind) throws Exception {
        long user = testUsers.active();
        String authorization = switch (kind) {
            case "expired" -> TestTokens.expired(user);
            case "foreign-signature" -> TestTokens.foreignSignature(user);
            case "foreign-issuer" -> TestTokens.foreignIssuer(user);
            case "garbage" -> "Bearer not-a-jwt";
            default -> "Basic dXNlcjpwYXNzd29yZA==";
        };
        mvc.perform(request().header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "0", "-1"})
    @DisplayName("Подписанный токен с некорректным субъектом — 401")
    void rejectsInvalidSubject(String subject) throws Exception {
        mvc.perform(request().header(HttpHeaders.AUTHORIZATION, TestTokens.bearer(subject)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Токен несуществующего пользователя — 401")
    void rejectsUnknownUser() throws Exception {
        mvc.perform(request().header(HttpHeaders.AUTHORIZATION, TestTokens.bearer(Long.MAX_VALUE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Чтение анкеты без токена — 401")
    void readingRequiresToken() throws Exception {
        mvc.perform(get("/api/v1/profiles/1")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Внутренние эндпоинты доступны без токена: запрос доходит до сервиса, у смотрящего нет анкеты — 404")
    void internalEndpointsAreOpen() throws Exception {
        mvc.perform(get("/internal/v1/profiles/feed").param("viewerUserId", "1"))
                .andExpect(status().isNotFound());
    }

    private static MockHttpServletRequestBuilder request() {
        return put("/api/v1/profiles/me").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ксения\",\"birthDate\":\"2003-05-17\",\"about\":null}");
    }
}
