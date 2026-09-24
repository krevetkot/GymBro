package ru.itmo.gymbro.catalog.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.gymbro.catalog.api.GymUseCases;
import ru.itmo.gymbro.catalog.dto.GymResponse;
import ru.itmo.gymbro.catalog.dto.GymWriteRequest;
import ru.itmo.gymbro.shared.web.PageResponses;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/gyms")
@Tag(name = "Залы")
@ApiResponse(responseCode = "400", description = "Некорректные поля или параметры запроса")
class GymController {

    private final GymUseCases gyms;

    GymController(GymUseCases gyms) {
        this.gyms = gyms;
    }

    @SecurityRequirements
    @PostMapping
    @Operation(summary = "Создать запись в каталоге")
    @ApiResponse(responseCode = "201", description = "Запись создана",
            headers = @Header(name = "Location", description = "Адрес созданной записи",
                    schema = @Schema(type = "string")))
    @ApiResponse(responseCode = "409", description = "Такая запись уже существует")
    ResponseEntity<GymResponse> create(@Valid @RequestBody GymWriteRequest request) {
        GymResponse response = GymResponse.from(gyms.create(request.name(), request.city(), request.address()));
        return ResponseEntity.created(URI.create("/api/v1/gyms/" + response.id())).body(response);
    }

    @SecurityRequirements
    @GetMapping("/{id}")
    @Operation(summary = "Получить запись по идентификатору")
    @ApiResponse(responseCode = "200", description = "Запись найдена")
    @ApiResponse(responseCode = "404", description = "Запись не найдена")
    GymResponse getById(@PathVariable @Positive long id) {
        return GymResponse.from(gyms.getById(id));
    }

    @SecurityRequirements
    @GetMapping
    @Operation(summary = "Получить страницу каталога",
            description = "Нумерация страниц с нуля, размер по умолчанию 20, максимум 50. "
                    + "Общее число записей — в X-Total-Count. Поля сортировки: id, name, city, address.")
    @ApiResponse(responseCode = "200", description = "Страница каталога",
            headers = @Header(name = "X-Total-Count", description = "Общее количество записей",
                    schema = @Schema(type = "integer", format = "int64")))
    ResponseEntity<List<GymResponse>> getPage(
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return PageResponses.withTotalCount(gyms.getPage(pageable).map(GymResponse::from));
    }

    @SecurityRequirement(name = "currentUser")
    @ApiResponse(responseCode = "401", description = "Текущий пользователь не определён")
    @ApiResponse(responseCode = "403", description = "Требуется активный администратор")
    @PutMapping("/{id}")
    @Operation(summary = "Обновить все поля записи")
    @ApiResponse(responseCode = "200", description = "Запись обновлена")
    @ApiResponse(responseCode = "404", description = "Запись не найдена")
    @ApiResponse(responseCode = "409", description = "Новые значения заняты другой записью")
    GymResponse update(@PathVariable @Positive long id, @Valid @RequestBody GymWriteRequest request) {
        return GymResponse.from(gyms.update(id, request.name(), request.city(), request.address()));
    }

    @SecurityRequirement(name = "currentUser")
    @ApiResponse(responseCode = "401", description = "Текущий пользователь не определён")
    @ApiResponse(responseCode = "403", description = "Требуется активный администратор")
    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить запись")
    @ApiResponse(responseCode = "204", description = "Запись удалена")
    @ApiResponse(responseCode = "404", description = "Запись не найдена")
    @ApiResponse(responseCode = "409", description = "На запись ссылаются другие данные")
    ResponseEntity<Void> delete(@PathVariable @Positive long id) {
        gyms.delete(id);
        return ResponseEntity.noContent().build();
    }
}
