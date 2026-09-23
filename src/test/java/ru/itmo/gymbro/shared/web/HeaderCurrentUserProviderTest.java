package ru.itmo.gymbro.shared.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ru.itmo.gymbro.shared.api.UnauthorizedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HeaderCurrentUserProviderTest {

    private final HeaderCurrentUserProvider provider = new HeaderCurrentUserProvider();

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Идентификатор берётся из заголовка X-User-Id")
    void readsUserIdFromHeader() {
        bindRequestWithHeader(" 42 ");

        assertThat(provider.currentUserId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("Без заголовка пользователь не аутентифицирован")
    void rejectsMissingHeader() {
        bindRequestWithHeader(null);

        assertThatThrownBy(provider::currentUserId)
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("X-User-Id");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "0", "-5", "1.5", "99999999999999999999"})
    @DisplayName("Заголовок не с положительным числом отклоняется")
    void rejectsMalformedHeader(String header) {
        bindRequestWithHeader(header);

        assertThatThrownBy(provider::currentUserId).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Вне HTTP-запроса текущего пользователя нет")
    void failsOutsideOfRequest() {
        assertThatThrownBy(provider::currentUserId).isInstanceOf(IllegalStateException.class);
    }

    private static void bindRequestWithHeader(String value) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (value != null) {
            request.addHeader("X-User-Id", value);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
