package ru.itmo.gymbro.shared.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.shared.validation.ColumnLengthValidator.ExpectedColumn;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ColumnLengthValidatorTest extends AbstractIntegrationTest {

    @Autowired
    private ColumnLengthValidator validator;

    @Test
    @DisplayName("Hibernate берёт длину колонки из @Size сущности")
    void readsLengthFromSizeAnnotation() {
        assertThat(validator.expectedColumns())
                .anySatisfy(column -> {
                    assertThat(column.getTable()).isEqualTo("users");
                    assertThat(column.getName()).isEqualTo("email");
                    assertThat(column.getLength()).isEqualTo(255);
                });
    }

    @Test
    @DisplayName("Длины реальных сущностей совпадают со схемой")
    void acceptsActualSchema() throws Exception {
        assertThat(validator.mismatchesFor(validator.expectedColumns())).isEmpty();
    }

    @Test
    @DisplayName("@Size(max = 260) при varchar(255) в БД — ошибка")
    void rejectsLengthMismatch() throws Exception {
        List<String> mismatches = validator.mismatchesFor(List.of(new ExpectedColumn("users", "email", 260)));

        assertThat(mismatches).containsExactly("users.email: в сущности длина 260, в БД varchar(255)");
    }
}
