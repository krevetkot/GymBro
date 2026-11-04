package ru.itmo.gymbro.identity.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.identity.api.UserUseCases;
import ru.itmo.gymbro.identity.model.Role;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.model.UserStatus;
import ru.itmo.gymbro.identity.repository.UserRepository;
import ru.itmo.gymbro.shared.api.ConflictException;
import ru.itmo.gymbro.shared.api.NotFoundException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserUseCasesTest extends AbstractIntegrationTest {

    @Autowired
    private UserUseCases users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository repository;

    @Test
    @DisplayName("Регистрация создаёт активного пользователя с хэшем пароля")
    void registersUserWithPassword() {
        User user = asAdmin(users.register("New@Mail.ru", "secret-password")).block();

        User stored = asAdmin(users.getById(user.getId())).block();
        assertThat(stored.getEmail()).isEqualTo("new@mail.ru");
        assertThat(stored.getRole()).isEqualTo(Role.USER);
        assertThat(stored.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(stored.getPassword()).isNotEqualTo("secret-password");
        assertThat(passwordEncoder.matches("secret-password", stored.getPassword())).isTrue();
    }

    @Test
    @DisplayName("Повторная регистрация на тот же email без учёта регистра — конфликт")
    void rejectsDuplicateEmail() {
        asAdmin(users.register("twin@mail.ru", "secret-password")).block();

        StepVerifier.create(asAdmin(users.register("TWIN@mail.ru", "other-password")))
                .verifyError(ConflictException.class);
    }

    @Test
    @DisplayName("Регистрация без пароля отклоняется")
    void rejectsBlankPassword() {
        StepVerifier.create(asAdmin(users.register("nopass@mail.ru", " ")))
                .verifyError(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Несуществующий пользователь не найден")
    void failsOnUnknownUser() {
        StepVerifier.create(asAdmin(users.getById(Long.MAX_VALUE))).verifyError(NotFoundException.class);
    }

    @Test
    @DisplayName("Пользователи отдаются постранично")
    void pagesUsers() {
        for (int index = 0; index < 3; index++) {
            asAdmin(users.register("paged" + index + "@mail.ru", "secret-password")).block();
        }

        Page<User> page = asAdmin(users.getPage(PageRequest.of(0, 2))).block();

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Удалённого пользователя больше нет, повторное удаление — не найден")
    void deletesUser() {
        User user = asAdmin(users.register("gone@mail.ru", "secret-password")).block();

        StepVerifier.create(asAdmin(users.delete(user.getId()))).verifyComplete();

        StepVerifier.create(asAdmin(users.getById(user.getId()))).verifyError(NotFoundException.class);
        StepVerifier.create(asAdmin(users.delete(user.getId()))).verifyError(NotFoundException.class);
    }

    private <T> Mono<T> asAdmin(Mono<T> operation) {
        String id = repository.findByEmail("admin@gymbro.local").orElseThrow().getId().toString();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(id)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextImpl context = new SecurityContextImpl(new JwtAuthenticationToken(jwt));
        return operation.contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context)));
    }
}
