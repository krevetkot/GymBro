package ru.itmo.gymbro.catalog.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.gymbro.catalog.api.ReviewGymRequestUseCase;
import ru.itmo.gymbro.catalog.dto.GymRequestResponse;

@RestController
@RequestMapping("/api/v1/gym-requests")
@Tag(name = "Заявки на добавление залов")
@SecurityRequirement(name = "currentUser")
@ApiResponse(responseCode = "400", description = "Некорректный идентификатор")
@ApiResponse(responseCode = "401", description = "Текущий пользователь не определён")
@ApiResponse(responseCode = "403", description = "Требуется активный администратор")
@ApiResponse(responseCode = "404", description = "Заявка не найдена")
@ApiResponse(responseCode = "409", description = "Заявка уже рассмотрена или операция конфликтует с данными")
class GymRequestReviewController {

    private final ReviewGymRequestUseCase review;

    GymRequestReviewController(ReviewGymRequestUseCase review) {
        this.review = review;
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Одобрить заявку",
            description = "В одной транзакции создаёт зал в общем каталоге и одобряет заявку. "
                    + "Профиль автора не изменяется. При существующем зале возвращает 409.")
    @ApiResponse(responseCode = "200", description = "Заявка одобрена, зал создан в каталоге")
    GymRequestResponse approve(@PathVariable @Positive long id) {
        return GymRequestResponse.from(review.approve(id));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Отклонить заявку",
            description = "Меняет статус PENDING на REJECTED и сохраняет администратора. Зал не создаётся.")
    @ApiResponse(responseCode = "200", description = "Заявка отклонена")
    GymRequestResponse reject(@PathVariable @Positive long id) {
        return GymRequestResponse.from(review.reject(id));
    }
}
