package ru.itmo.gymbro.matching.web;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.gymbro.matching.api.MatchingUseCases;
import ru.itmo.gymbro.matching.dto.FeedCardResponse;
import ru.itmo.gymbro.matching.dto.LikeRequest;
import ru.itmo.gymbro.matching.dto.LikeResponse;
import ru.itmo.gymbro.matching.dto.MatchResponse;
import ru.itmo.gymbro.shared.dto.SliceResponse;
import ru.itmo.gymbro.shared.web.PageResponses;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
class MatchingController {

    private final MatchingUseCases matching;

    MatchingController(MatchingUseCases matching) {
        this.matching = matching;
    }

    @GetMapping("/feed")
    SliceResponse<FeedCardResponse> nextFeedCards(Pageable pageable) {
        return SliceResponse.from(matching.nextFeedCards(pageable).map(FeedCardResponse::from));
    }

    @PostMapping("/likes")
    @ResponseStatus(HttpStatus.CREATED)
    LikeResponse like(@Valid @RequestBody LikeRequest request) {
        return matching.like(request.getToUserId());
    }

    @GetMapping("/matches")
    ResponseEntity<List<MatchResponse>> myMatches(Pageable pageable) {
        return PageResponses.withTotalCount(matching.myMatches(pageable));
    }
}
