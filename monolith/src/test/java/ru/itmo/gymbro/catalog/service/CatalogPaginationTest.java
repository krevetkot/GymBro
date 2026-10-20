package ru.itmo.gymbro.catalog.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogPaginationTest {

    private static final Set<String> FIELDS = Set.of("id", "name");

    @Test
    void addsIdToMakeOrderingDeterministic() {
        Pageable page = CatalogPagination.validate(PageRequest.of(2, 20, Sort.by("name")), FIELDS);
        assertThat(page.getPageNumber()).isEqualTo(2);
        assertThat(page.getSort()).containsExactly(Sort.Order.asc("name"), Sort.Order.asc("id"));
    }

    @Test
    void preservesExplicitIdDirection() {
        Pageable page = CatalogPagination.validate(PageRequest.of(0, 50, Sort.by(Sort.Order.desc("id"))), FIELDS);
        assertThat(page.getSort()).containsExactly(Sort.Order.desc("id"));
    }

    @Test
    void rejectsUnboundedRequestsAndUnknownSortFields() {
        assertThatThrownBy(() -> CatalogPagination.validate(Pageable.unpaged(), FIELDS))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CatalogPagination.validate(PageRequest.of(0, 51), FIELDS))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CatalogPagination.validate(PageRequest.of(0, 20, Sort.by("unknown")), FIELDS))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

