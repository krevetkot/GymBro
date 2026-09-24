package ru.itmo.gymbro.matching.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.gymbro.matching.api.BrowseFeedUseCase;
import ru.itmo.gymbro.matching.dto.FeedCardResponse;
import ru.itmo.gymbro.shared.dto.SliceResponse;

@RestController
@RequestMapping("/api/v1/feed")
@Tag(name = "Поиск партнёров")
@SecurityRequirement(name = "currentUser")
class FeedController {

    private final BrowseFeedUseCase browseFeed;

    FeedController(BrowseFeedUseCase browseFeed) {
        this.browseFeed = browseFeed;
    }

    @GetMapping
    @Operation(summary = "Получить следующую страницу ленты", description = "Slice: items, page, size, hasNext; общего количества и X-Total-Count нет. page начинается с 0, size по умолчанию 20, максимум 50. Исключает себя, заблокированных и уже лайкнутых. Порядок: число общих видов спорта и залов по убыванию, затем ID анкеты; параметр sort не используется.")
    @ApiResponse(responseCode = "200", description = "Страница ленты без общего количества")
    @ApiResponse(responseCode = "400", description = "Некорректные параметры")
    @ApiResponse(responseCode = "401", description = "Текущий пользователь не определён")
    @ApiResponse(responseCode = "403", description = "Пользователь заблокирован")
    @ApiResponse(responseCode = "404", description = "Сначала необходимо создать свою анкету")
    SliceResponse<FeedCardResponse> nextCards(@ParameterObject Pageable pageable) {
        return SliceResponse.from(browseFeed.nextCards(pageable).map(FeedCardResponse::from));
    }
}
