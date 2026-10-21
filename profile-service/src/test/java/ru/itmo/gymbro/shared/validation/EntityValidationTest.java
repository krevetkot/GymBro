package ru.itmo.gymbro.shared.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.repository.UserProfileRepository;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntityValidationTest extends AbstractIntegrationTest {

    @Autowired
    private UserProfileRepository profiles;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Анкета с именем длиннее колонки не сохраняется")
    void rejectsTooLongProfileName() {
        long userId = testUsers.active();
        UserProfile profile = UserProfile.create(userId, "я".repeat(101), LocalDate.of(2000, 1, 1), null);

        assertThatThrownBy(() -> profiles.save(profile))
                .isInstanceOfSatisfying(ConstraintViolationException.class,
                        exception -> assertThat(violatedPaths(exception)).containsExactly("name"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_profiles WHERE user_id = ?", Long.class, userId)).isZero();
    }

    private static List<String> violatedPaths(ConstraintViolationException exception) {
        return exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .toList();
    }
}
