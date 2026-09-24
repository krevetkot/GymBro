package ru.itmo.gymbro.matching.web;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.gymbro.matching.api.ListMatchesUseCase;
import ru.itmo.gymbro.matching.dto.MatchResponse;
import ru.itmo.gymbro.shared.web.PageResponses;

import java.util.List;

@RestController
@RequestMapping("/api/v1/matches")
class MatchController {

    private final ListMatchesUseCase listMatches;

    MatchController(ListMatchesUseCase listMatches) {
        this.listMatches = listMatches;
    }

    @GetMapping
    ResponseEntity<List<MatchResponse>> myMatches(Pageable pageable) {
        return PageResponses.withTotalCount(listMatches.myMatches(pageable).map(MatchResponse::from));
    }
}
