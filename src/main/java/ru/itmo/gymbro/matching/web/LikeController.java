package ru.itmo.gymbro.matching.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.gymbro.matching.api.LikeUserUseCase;
import ru.itmo.gymbro.matching.dto.LikeRequest;
import ru.itmo.gymbro.matching.dto.LikeResponse;

@RestController
@RequestMapping("/api/v1/likes")
@Tag(name = "Поиск партнёров")
@SecurityRequirement(name = "currentUser")
class LikeController {

    private final LikeUserUseCase likeUser;

    LikeController(LikeUserUseCase likeUser) {
        this.likeUser = likeUser;
    }

    @PostMapping
    @Operation(summary = "Поставить лайк пользователю", description = "Отправитель определяется по X-User-Id. В одной транзакции сохраняется лайк и, при встречном лайке, создаётся совпадение. Пара пользователей блокируется до завершения транзакции.")
    @ApiResponse(responseCode = "201", description = "Лайк создан; ответ указывает наличие совпадения")
    @ApiResponse(responseCode = "400", description = "Некорректный ID или лайк самому себе")
    @ApiResponse(responseCode = "401", description = "Текущий пользователь не определён")
    @ApiResponse(responseCode = "403", description = "Отправитель заблокирован")
    @ApiResponse(responseCode = "404", description = "Получатель не найден или заблокирован")
    @ApiResponse(responseCode = "409", description = "Повторный лайк")
    ResponseEntity<LikeResponse> like(@Valid @RequestBody LikeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(LikeResponse.from(likeUser.like(request.getToUserId())));
    }
}
