package ru.itmo.gymbro.matching.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
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
@Tag(name = "Поиск партнёров")
@SecurityRequirement(name = "currentUser")
class MatchController {

    private final ListMatchesUseCase listMatches;

    MatchController(ListMatchesUseCase listMatches) {
        this.listMatches = listMatches;
    }

    @GetMapping
    @Operation(summary = "Получить свои совпадения", description = "page начинается с 0, size по умолчанию 20, максимум 50. Возвращает только совпадения текущего пользователя; общее количество — в X-Total-Count.")
    @ApiResponse(responseCode = "200", description = "Страница совпадений", headers = @io.swagger.v3.oas.annotations.headers.Header(name = "X-Total-Count", description = "Общее количество совпадений", schema = @io.swagger.v3.oas.annotations.media.Schema(type = "integer", format = "int64")))
    @ApiResponse(responseCode = "400", description = "Некорректные параметры")
    @ApiResponse(responseCode = "401", description = "Текущий пользователь не определён")
    @ApiResponse(responseCode = "403", description = "Пользователь заблокирован")
    ResponseEntity<List<MatchResponse>> myMatches(@ParameterObject Pageable pageable) {
        return PageResponses.withTotalCount(listMatches.myMatches(pageable).map(MatchResponse::from));
    }
}
