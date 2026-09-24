package ru.itmo.gymbro.matching.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
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
@Tag(name = "Поиск партнёров")
@SecurityRequirement(name = "currentUser")
@ApiResponse(responseCode = "401", description = "Текущий пользователь не определён")
@ApiResponse(responseCode = "403", description = "Пользователь заблокирован")
class MatchingController {

    private final MatchingUseCases matching;

    MatchingController(MatchingUseCases matching) {
        this.matching = matching;
    }

    @GetMapping("/feed")
    @Operation(summary = "Получить следующую страницу ленты",
            description = "Slice: items, page, size, hasNext; общего количества и X-Total-Count нет. "
                    + "page начинается с 0, size по умолчанию 20, максимум 50. "
                    + "Исключает себя, заблокированных и уже лайкнутых. Порядок: число общих видов спорта "
                    + "и залов по убыванию, затем ID анкеты; параметр sort не используется.")
    @ApiResponse(responseCode = "200", description = "Страница ленты без общего количества")
    @ApiResponse(responseCode = "400", description = "Некорректные параметры")
    @ApiResponse(responseCode = "404", description = "Сначала необходимо создать свою анкету")
    SliceResponse<FeedCardResponse> nextFeedCards(@ParameterObject Pageable pageable) {
        return SliceResponse.from(matching.nextFeedCards(pageable).map(FeedCardResponse::from));
    }

    @PostMapping("/likes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Поставить лайк пользователю",
            description = "Отправитель определяется по X-User-Id. В одной транзакции сохраняется лайк и, "
                    + "при встречном лайке, создаётся совпадение. "
                    + "Пара пользователей блокируется до завершения транзакции.")
    @ApiResponse(responseCode = "201", description = "Лайк создан; ответ указывает наличие совпадения")
    @ApiResponse(responseCode = "400", description = "Некорректный ID или лайк самому себе")
    @ApiResponse(responseCode = "404", description = "Получатель не найден или заблокирован")
    @ApiResponse(responseCode = "409", description = "Повторный лайк")
    LikeResponse like(@Valid @RequestBody LikeRequest request) {
        return matching.like(request.getToUserId());
    }

    @GetMapping("/matches")
    @Operation(summary = "Получить свои совпадения",
            description = "page начинается с 0, size по умолчанию 20, максимум 50. "
                    + "Возвращает только совпадения текущего пользователя; общее количество — в X-Total-Count.")
    @ApiResponse(responseCode = "200", description = "Страница совпадений",
            headers = @Header(name = "X-Total-Count", description = "Общее количество совпадений",
                    schema = @Schema(type = "integer", format = "int64")))
    @ApiResponse(responseCode = "400", description = "Некорректные параметры")
    ResponseEntity<List<MatchResponse>> myMatches(@ParameterObject Pageable pageable) {
        return PageResponses.withTotalCount(matching.myMatches(pageable));
    }
}
