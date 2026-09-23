package ru.itmo.gymbro.shared.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import ru.itmo.gymbro.AbstractIntegrationTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PageableLimitTest extends AbstractIntegrationTest {

    @Autowired
    private PageableHandlerMethodArgumentResolver resolver;

    @Test
    @DisplayName("Без параметров отдаётся первая страница по 20 записей")
    void usesDefaultPageSize() throws Exception {
        Pageable pageable = resolve(Map.of());

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("Запрос больше 50 записей урезается до 50")
    void capsPageSizeAtFifty() throws Exception {
        Pageable pageable = resolve(Map.of("page", "2", "size", "1000"));

        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(50);
    }

    private Pageable resolve(Map<String, String> parameters) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        parameters.forEach(request::addParameter);
        MethodParameter parameter = new MethodParameter(
                PageableLimitTest.class.getDeclaredMethod("listing", Pageable.class), 0);
        return resolver.resolveArgument(parameter, null, new ServletWebRequest(request), null);
    }

    @SuppressWarnings("unused")
    private void listing(Pageable pageable) {
    }
}
