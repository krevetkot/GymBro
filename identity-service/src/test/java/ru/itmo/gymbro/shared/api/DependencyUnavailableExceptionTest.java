package ru.itmo.gymbro.shared.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyUnavailableExceptionTest {

    @Test
    @DisplayName("Исключение недоступного соседа хранит текст и исходную причину")
    void keepsMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("connection refused");

        assertThat(new DependencyUnavailableException("Сервис недоступен"))
                .hasMessage("Сервис недоступен")
                .hasNoCause();
        assertThat(new DependencyUnavailableException("Сервис недоступен", cause))
                .hasMessage("Сервис недоступен")
                .hasCause(cause);
    }
}
