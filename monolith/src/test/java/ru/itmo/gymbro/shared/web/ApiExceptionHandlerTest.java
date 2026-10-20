package ru.itmo.gymbro.shared.web;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.gymbro.shared.api.ConflictException;
import ru.itmo.gymbro.shared.api.ForbiddenException;
import ru.itmo.gymbro.shared.api.NotFoundException;
import ru.itmo.gymbro.shared.api.UnauthorizedException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerTest {

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new FailingController())
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    @Test
    @DisplayName("Ненайденный ресурс превращается в 404 с текстом ошибки")
    void mapsNotFoundTo404() throws Exception {
        mvc.perform(get("/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Не найдено"))
                .andExpect(jsonPath("$.detail").value("Пользователь 42 не найден"));
    }

    @Test
    @DisplayName("Доменные исключения получают свои статусы")
    void mapsDomainExceptionsToStatuses() throws Exception {
        mvc.perform(get("/conflict")).andExpect(status().isConflict());
        mvc.perform(get("/forbidden")).andExpect(status().isForbidden());
        mvc.perform(get("/unauthorized")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Нарушенный инвариант сущности — 400, недопустимый переход состояния — 409")
    void mapsInvariantViolations() throws Exception {
        mvc.perform(get("/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Нельзя лайкнуть самого себя"));
        mvc.perform(get("/illegal-state"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Заявка уже рассмотрена"));
    }

    @Test
    @DisplayName("Невалидное тело запроса — 400 со списком полей")
    void listsInvalidBodyFields() throws Exception {
        mvc.perform(post("/bodies").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
    }

    @Test
    @DisplayName("Нечитаемый JSON — 400 с понятным сообщением")
    void rejectsMalformedJson() throws Exception {
        mvc.perform(post("/bodies").contentType(MediaType.APPLICATION_JSON).content("{name"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("JSON")));
    }

    @Test
    @DisplayName("Параметр пути неверного типа — 400 с именем параметра")
    void rejectsMistypedPathVariable() throws Exception {
        mvc.perform(get("/items/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("'id'")));
    }

    @Test
    @DisplayName("Невалидный параметр метода — 400 с именем параметра")
    void rejectsInvalidPathVariable() throws Exception {
        mvc.perform(get("/items/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("id"));
    }

    @Test
    @DisplayName("Нарушение Bean Validation при сохранении — 400 со списком полей")
    void mapsConstraintViolation() throws Exception {
        mvc.perform(get("/constraint-violation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    @DisplayName("Нарушение уникальности в базе — 409 без текста SQL")
    void hidesSqlOnDataIntegrityViolation() throws Exception {
        mvc.perform(get("/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", not(containsString("users_email_unique"))));
    }

    @Test
    @DisplayName("Непредвиденная ошибка — 500 без подробностей реализации")
    void hidesDetailsOfUnexpectedErrors() throws Exception {
        mvc.perform(get("/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail", not(containsString("NullPointerException"))));
    }

    @RestController
    static class FailingController {

        private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        @GetMapping("/not-found")
        void notFound() {
            throw new NotFoundException("Пользователь 42 не найден");
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new ConflictException("Email уже занят");
        }

        @GetMapping("/forbidden")
        void forbidden() {
            throw new ForbiddenException("Только администратор может одобрять заявки");
        }

        @GetMapping("/unauthorized")
        void unauthorized() {
            throw new UnauthorizedException("Не передан текущий пользователь");
        }

        @GetMapping("/illegal-argument")
        void illegalArgument() {
            throw new IllegalArgumentException("Нельзя лайкнуть самого себя");
        }

        @GetMapping("/illegal-state")
        void illegalState() {
            throw new IllegalStateException("Заявка уже рассмотрена");
        }

        @PostMapping("/bodies")
        void acceptBody(@Valid @RequestBody NamedBody body) {
        }

        @GetMapping("/items/{id}")
        void item(@PathVariable @Positive long id) {
        }

        @GetMapping("/constraint-violation")
        void constraintViolation() {
            throw new ConstraintViolationException(validator.validate(new NamedBody()));
        }

        @GetMapping("/duplicate")
        void duplicate() {
            throw new DuplicateKeyException("duplicate key value violates unique constraint \"users_email_unique\"");
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new NullPointerException("NullPointerException in internals");
        }
    }

    static class NamedBody {

        @NotBlank
        private String name;

        public String getName() {
            return name;
        }
    }
}
