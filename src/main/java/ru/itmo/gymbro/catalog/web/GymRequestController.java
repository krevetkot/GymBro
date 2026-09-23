package ru.itmo.gymbro.catalog.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.gymbro.catalog.api.GymRequestUseCases;
import ru.itmo.gymbro.catalog.dto.GymRequestResponse;
import ru.itmo.gymbro.catalog.dto.GymRequestSubmission;
import ru.itmo.gymbro.shared.web.PageResponses;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gym-requests")
@Tag(name = "Заявки на добавление залов")
@SecurityRequirement(name = "currentUser")
@ApiResponse(responseCode = "400", description = "Некорректные поля или параметры")
@ApiResponse(responseCode = "401", description = "Нет корректного X-User-Id или пользователь не существует")
@ApiResponse(responseCode = "403", description = "Пользователь заблокирован или недостаточно прав")
class GymRequestController {

    private final GymRequestUseCases requests;

    GymRequestController(GymRequestUseCases requests) {
        this.requests = requests;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Предложить новый зал",
            description = "Автор определяется по X-User-Id. Создаётся только заявка в статусе PENDING.")
    @ApiResponse(responseCode = "201", description = "Заявка создана")
    @ApiResponse(responseCode = "409", description = "Зал по этому адресу уже существует")
    GymRequestResponse submit(@Valid @RequestBody GymRequestSubmission request) {
        return GymRequestResponse.from(requests.submit(request.name(), request.city(), request.address()));
    }

    @GetMapping
    @Operation(summary = "Получить страницу заявок (только администратор)",
            description = "Страницы с нуля, по умолчанию 20 записей, максимум 50. "
                    + "По умолчанию новые заявки идут первыми. Поля сортировки: "
                    + "id, authorId, name, city, address, status, createdAt.")
    @ApiResponse(responseCode = "200", description = "Страница заявок",
            headers = @Header(name = "X-Total-Count", description = "Общее количество заявок",
                    schema = @Schema(type = "integer", format = "int64")))
    ResponseEntity<List<GymRequestResponse>> getPage(
            @ParameterObject @PageableDefault(size = 20, sort = {"createdAt", "id"},
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponses.withTotalCount(requests.getPage(pageable).map(GymRequestResponse::from));
    }
}

