package ru.itmo.gymbro.catalog.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.function.Function;

final class CatalogPagination {

    static Pageable validate(Pageable pageable, Set<String> allowedProperties) {
        if (pageable.isUnpaged() || pageable.getPageSize() > 50) {
            throw new IllegalArgumentException("Размер страницы должен быть от 1 до 50");
        }
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedProperties.contains(order.getProperty())) {
                throw new IllegalArgumentException("Неизвестное поле сортировки: " + order.getProperty());
            }
        }
        Sort sort = pageable.getSort();
        if (sort.getOrderFor("id") == null) {
            sort = sort.and(Sort.by("id"));
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    static <T> Mono<Page<T>> page(Pageable pageable, Set<String> allowedProperties,
                                  Function<Pageable, Flux<T>> content, Mono<Long> total) {
        return Mono.defer(() -> {
            Pageable checked = validate(pageable, allowedProperties);
            return Mono.zip(content.apply(checked).collectList(), total)
                    .map(result -> new PageImpl<>(result.getT1(), checked, result.getT2()));
        });
    }
}
