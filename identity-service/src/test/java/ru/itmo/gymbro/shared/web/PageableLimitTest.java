package ru.itmo.gymbro.shared.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.BindingContext;

import static org.assertj.core.api.Assertions.assertThat;

class PageableLimitTest {

    @Test
    @DisplayName("Без параметров отдаётся первая страница по 20 записей")
    void usesDefaultPageSize() throws Exception {
        Pageable pageable = resolve("/items");

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("Запрос больше 50 записей урезается до 50")
    void capsPageSizeAtFifty() throws Exception {
        Pageable pageable = resolve("/items?page=2&size=1000");

        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(50);
    }

    private Pageable resolve(String uri) throws Exception {
        MethodParameter parameter = new MethodParameter(
                PageableLimitTest.class.getDeclaredMethod("listing", Pageable.class), 0);
        return WebFluxConfiguration.pageableResolver().resolveArgumentValue(parameter, new BindingContext(),
                MockServerWebExchange.from(MockServerHttpRequest.get(uri)));
    }

    @SuppressWarnings("unused")
    private void listing(Pageable pageable) {
    }
}
