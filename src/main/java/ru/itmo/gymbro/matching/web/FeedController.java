package ru.itmo.gymbro.matching.web;

import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.gymbro.matching.api.BrowseFeedUseCase;
import ru.itmo.gymbro.matching.dto.FeedCardResponse;
import ru.itmo.gymbro.shared.dto.SliceResponse;

@RestController
@RequestMapping("/api/v1/feed")
class FeedController {

    private final BrowseFeedUseCase browseFeed;

    FeedController(BrowseFeedUseCase browseFeed) {
        this.browseFeed = browseFeed;
    }

    @GetMapping
    SliceResponse<FeedCardResponse> nextCards(Pageable pageable) {
        return SliceResponse.from(browseFeed.nextCards(pageable).map(FeedCardResponse::from));
    }
}
