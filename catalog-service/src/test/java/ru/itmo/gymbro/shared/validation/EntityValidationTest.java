package ru.itmo.gymbro.shared.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.catalog.model.Sport;
import ru.itmo.gymbro.catalog.repository.SportRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EntityValidationTest extends AbstractIntegrationTest {

    @Autowired
    private SportRepository sports;

    @Test
    @DisplayName("Вид спорта с названием длиннее колонки не сохраняется")
    void rejectsTooLongSportName() {
        Sport sport = Sport.of("х".repeat(101));

        StepVerifier.create(sports.save(sport))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOfSatisfying(ConstraintViolationException.class,
                                exception -> assertThat(violatedPaths(exception)).containsExactly("name")))
                .verify();
        assertThat(sports.existsByName(sport.getName()).block()).isFalse();
    }

    private static List<String> violatedPaths(ConstraintViolationException exception) {
        return exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .toList();
    }
}
