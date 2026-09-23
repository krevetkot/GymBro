package ru.itmo.gymbro.catalog.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
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
import ru.itmo.gymbro.catalog.api.SportUseCases;
import ru.itmo.gymbro.catalog.dto.SportResponse;
import ru.itmo.gymbro.catalog.dto.SportWriteRequest;
import ru.itmo.gymbro.shared.web.PageResponses;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sports")
@Tag(name = "Виды спорта")
@SecurityRequirements
@ApiResponse(responseCode = "400", description = "Некорректные поля или параметры запроса")
class SportController {

    private final SportUseCases sports;

    SportController(SportUseCases sports) {
        this.sports = sports;
    }

    @PostMapping
    @Operation(summary = "Создать запись в каталоге")
    @ApiResponse(responseCode = "201", description = "Запись создана",
            headers = @Header(name = "Location", description = "Адрес созданной записи",
                    schema = @Schema(type = "string")))
    @ApiResponse(responseCode = "409", description = "Такая запись уже существует")
    ResponseEntity<SportResponse> create(@Valid @RequestBody SportWriteRequest request) {
        SportResponse response = SportResponse.from(sports.create(request.name()));
        return ResponseEntity.created(URI.create("/api/v1/sports/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить запись по идентификатору")
    @ApiResponse(responseCode = "200", description = "Запись найдена")
    @ApiResponse(responseCode = "404", description = "Запись не найдена")
    SportResponse getById(@PathVariable @Positive long id) {
        return SportResponse.from(sports.getById(id));
    }

    @GetMapping
    @Operation(summary = "Получить страницу каталога",
            description = "Нумерация страниц с нуля, размер по умолчанию 20, максимум 50. "
                    + "Общее число записей — в X-Total-Count. Поля сортировки: id, name.")
    @ApiResponse(responseCode = "200", description = "Страница каталога",
            headers = @Header(name = "X-Total-Count", description = "Общее количество записей",
                    schema = @Schema(type = "integer", format = "int64")))
    ResponseEntity<List<SportResponse>> getPage(
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return PageResponses.withTotalCount(sports.getPage(pageable).map(SportResponse::from));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить все поля записи")
    @ApiResponse(responseCode = "200", description = "Запись обновлена")
    @ApiResponse(responseCode = "404", description = "Запись не найдена")
    @ApiResponse(responseCode = "409", description = "Новые значения заняты другой записью")
    SportResponse update(@PathVariable @Positive long id, @Valid @RequestBody SportWriteRequest request) {
        return SportResponse.from(sports.update(id, request.name()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить запись")
    @ApiResponse(responseCode = "204", description = "Запись удалена")
    @ApiResponse(responseCode = "404", description = "Запись не найдена")
    @ApiResponse(responseCode = "409", description = "На запись ссылаются другие данные")
    ResponseEntity<Void> delete(@PathVariable @Positive long id) {
        sports.delete(id);
        return ResponseEntity.noContent().build();
    }
}
