package ru.itmo.gymbro.shared.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponsesTest {

    @Test
    @DisplayName("Страница отдаётся списком, а общее количество — в заголовке X-Total-Count")
    void putsTotalCountIntoHeader() {
        PageImpl<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 2), 57);

        ResponseEntity<List<String>> response = PageResponses.withTotalCount(page);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("X-Total-Count")).isEqualTo("57");
        assertThat(response.getBody()).containsExactly("a", "b");
    }

    @Test
    @DisplayName("Пустая страница отдаёт нулевое количество")
    void reportsZeroForEmptyPage() {
        ResponseEntity<List<String>> response = PageResponses.withTotalCount(Page.empty());

        assertThat(response.getHeaders().getFirst(PageResponses.TOTAL_COUNT_HEADER)).isEqualTo("0");
        assertThat(response.getBody()).isEmpty();
    }
}
