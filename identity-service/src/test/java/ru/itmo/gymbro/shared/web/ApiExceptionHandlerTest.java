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
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.itmo.gymbro.shared.api.ConflictException;
import ru.itmo.gymbro.shared.api.DependencyUnavailableException;
import ru.itmo.gymbro.shared.api.ForbiddenException;
import ru.itmo.gymbro.shared.api.NotFoundException;
import ru.itmo.gymbro.shared.api.UnauthorizedException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

class ApiExceptionHandlerTest {

    private final WebTestClient client = WebTestClient.bindToController(new FailingController())
            .controllerAdvice(new ApiExceptionHandler())
            .build();

    @Test
    @DisplayName("Ненайденный ресурс превращается в 404 с текстом ошибки")
    void mapsNotFoundTo404() {
        client.get().uri("/not-found").exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.title").isEqualTo("Не найдено")
                .jsonPath("$.detail").isEqualTo("Пользователь 42 не найден");
    }

    @Test
    @DisplayName("Доменные исключения получают свои статусы, в том числе ошибки внутри Mono")
    void mapsDomainExceptionsToStatuses() {
        client.get().uri("/conflict").exchange().expectStatus().isEqualTo(409);
        client.get().uri("/forbidden").exchange().expectStatus().isForbidden();
        client.get().uri("/unauthorized").exchange().expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Недоступный соседний сервис — 503 с понятным текстом")
    void mapsUnavailableDependencyTo503() {
        client.get().uri("/dependency-unavailable").exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Сервис недоступен")
                .jsonPath("$.detail").isEqualTo("Сервис пользователей временно недоступен");
    }

    @Test
    @DisplayName("Нарушенный инвариант сущности — 400, недопустимый переход состояния — 409")
    void mapsInvariantViolations() {
        client.get().uri("/illegal-argument").exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.detail").isEqualTo("Нельзя лайкнуть самого себя");
        client.get().uri("/illegal-state").exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.detail").isEqualTo("Заявка уже рассмотрена");
    }

    @Test
    @DisplayName("Невалидное тело запроса — 400 со списком полей")
    void listsInvalidBodyFields() {
        client.post().uri("/bodies").contentType(MediaType.APPLICATION_JSON).bodyValue("{\"name\": \" \"}").exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors[0].field").isEqualTo("name")
                .jsonPath("$.errors[0].message").isNotEmpty();
    }

    @Test
    @DisplayName("Нечитаемый JSON — 400 с понятным сообщением")
    void rejectsMalformedJson() {
        client.post().uri("/bodies").contentType(MediaType.APPLICATION_JSON).bodyValue("{name").exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.detail").value(containsString("JSON"));
    }

    @Test
    @DisplayName("Параметр пути неверного типа — 400 с именем параметра")
    void rejectsMistypedPathVariable() {
        client.get().uri("/items/abc").exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.detail").value(containsString("'id'"));
    }

    @Test
    @DisplayName("Невалидный параметр метода — 400 с именем параметра")
    void rejectsInvalidPathVariable() {
        client.get().uri("/items/-1").exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.errors[0].field").isEqualTo("id");
    }

    @Test
    @DisplayName("Нарушение Bean Validation при сохранении — 400 со списком полей")
    void mapsConstraintViolation() {
        client.get().uri("/constraint-violation").exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.errors[0].field").isEqualTo("name");
    }

    @Test
    @DisplayName("Нарушение уникальности в базе — 409 без текста SQL")
    void hidesSqlOnDataIntegrityViolation() {
        client.get().uri("/duplicate").exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.detail").value(not(containsString("users_email_unique")));
    }

    @Test
    @DisplayName("Непредвиденная ошибка — 500 без подробностей реализации")
    void hidesDetailsOfUnexpectedErrors() {
        client.get().uri("/unexpected").exchange()
                .expectStatus().is5xxServerError()
                .expectBody().jsonPath("$.detail").value(not(containsString("NullPointerException")));
    }

    @RestController
    static class FailingController {

        private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        @GetMapping("/not-found")
        Mono<Void> notFound() {
            return Mono.error(new NotFoundException("Пользователь 42 не найден"));
        }

        @GetMapping("/conflict")
        Mono<Void> conflict() {
            return Mono.error(new ConflictException("Email уже занят"));
        }

        @GetMapping("/forbidden")
        void forbidden() {
            throw new ForbiddenException("Только администратор может одобрять заявки");
        }

        @GetMapping("/unauthorized")
        Mono<Void> unauthorized() {
            return Mono.error(new UnauthorizedException("Не передан текущий пользователь"));
        }

        @GetMapping("/dependency-unavailable")
        Mono<Void> dependencyUnavailable() {
            return Mono.error(new DependencyUnavailableException("Сервис пользователей временно недоступен"));
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
        Mono<Void> acceptBody(@Valid @RequestBody NamedBody body) {
            return Mono.empty();
        }

        @GetMapping("/items/{id}")
        Mono<Void> item(@PathVariable @Positive long id) {
            return Mono.empty();
        }

        @GetMapping("/constraint-violation")
        Mono<Void> constraintViolation() {
            return Mono.error(new ConstraintViolationException(validator.validate(new NamedBody())));
        }

        @GetMapping("/duplicate")
        Mono<Void> duplicate() {
            return Mono.error(new DuplicateKeyException(
                    "duplicate key value violates unique constraint \"users_email_unique\""));
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

        public void setName(String name) {
            this.name = name;
        }
    }
}
