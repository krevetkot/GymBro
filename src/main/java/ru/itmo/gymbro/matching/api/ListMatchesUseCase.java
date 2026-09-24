package ru.itmo.gymbro.matching.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ListMatchesUseCase {

    Page<MyMatch> myMatches(Pageable pageable);
}
