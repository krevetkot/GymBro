package ru.itmo.gymbro.shared.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntityValidationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository users;

    @Test
    @DisplayName("Пользователь с некорректным email не сохраняется")
    void rejectsMalformedEmail() {
        User user = User.register("not-an-email", "hash");

        assertThatThrownBy(() -> users.save(user))
                .isInstanceOfSatisfying(ConstraintViolationException.class,
                        exception -> assertThat(violatedPaths(exception)).containsExactly("email"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Long.class, "not-an-email")).isZero();
    }

    private static List<String> violatedPaths(ConstraintViolationException exception) {
        return exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .toList();
    }
}
