package ru.itmo.gymbro.shared.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.catalog.model.Sport;
import ru.itmo.gymbro.catalog.repository.SportRepository;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.UserRepository;
import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.repository.UserProfileRepository;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntityValidationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository users;

    @Autowired
    private SportRepository sports;

    @Autowired
    private UserProfileRepository profiles;

    @Test
    @DisplayName("Пользователь с некорректным email не сохраняется")
    void rejectsMalformedEmail() {
        User user = User.register("not-an-email", "hash");

        assertThatThrownBy(() -> users.save(user))
                .isInstanceOfSatisfying(ConstraintViolationException.class,
                        exception -> assertThat(violatedPaths(exception)).containsExactly("email"));
        assertThat(users.existsByEmail("not-an-email")).isFalse();
    }

    @Test
    @DisplayName("Вид спорта с названием длиннее колонки не сохраняется")
    void rejectsTooLongSportName() {
        Sport sport = Sport.of("х".repeat(101));

        assertThatThrownBy(() -> sports.save(sport))
                .isInstanceOfSatisfying(ConstraintViolationException.class,
                        exception -> assertThat(violatedPaths(exception)).containsExactly("name"));
    }

    @Test
    @DisplayName("Анкета с именем длиннее колонки не сохраняется")
    void rejectsTooLongProfileName() {
        long userId = users.save(User.register("longname@mail.ru", "hash")).getId();
        UserProfile profile = UserProfile.create(userId, "я".repeat(101), LocalDate.of(2000, 1, 1), null);

        assertThatThrownBy(() -> profiles.save(profile))
                .isInstanceOfSatisfying(ConstraintViolationException.class,
                        exception -> assertThat(violatedPaths(exception)).containsExactly("name"));
        assertThat(profiles.existsByUserId(userId)).isFalse();
    }

    private static List<String> violatedPaths(ConstraintViolationException exception) {
        return exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .toList();
    }
}
