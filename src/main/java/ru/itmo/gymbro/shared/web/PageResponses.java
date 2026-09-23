package ru.itmo.gymbro.shared.web;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import java.util.List;

public final class PageResponses {

    public static final String TOTAL_COUNT_HEADER = "X-Total-Count";

    private PageResponses() {
    }

    public static <T> ResponseEntity<List<T>> withTotalCount(Page<T> page) {
        return ResponseEntity.ok()
                .header(TOTAL_COUNT_HEADER, String.valueOf(page.getTotalElements()))
                .body(page.getContent());
    }
}
