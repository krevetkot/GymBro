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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.gymbro.catalog.api.SportUseCases;
import ru.itmo.gymbro.catalog.dto.SportResponse;
import ru.itmo.gymbro.catalog.dto.SportWriteRequest;
import ru.itmo.gymbro.shared.web.PageResponses;

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
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать вид спорта")
    @ApiResponse(responseCode = "201", description = "Вид спорта создан")
    @ApiResponse(responseCode = "409", description = "Такой вид спорта уже существует")
    SportResponse create(@Valid @RequestBody SportWriteRequest request) {
        return SportResponse.from(sports.create(request.name()));
    }

    @GetMapping
    @Operation(summary = "Получить страницу видов спорта",
            description = "Нумерация страниц с нуля, размер по умолчанию 20, максимум 50. "
                    + "Общее число записей — в X-Total-Count. Поля сортировки: id, name.")
    @ApiResponse(responseCode = "200", description = "Страница видов спорта",
            headers = @Header(name = "X-Total-Count", description = "Общее количество записей",
                    schema = @Schema(type = "integer", format = "int64")))
    ResponseEntity<List<SportResponse>> getPage(
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return PageResponses.withTotalCount(sports.getPage(pageable).map(SportResponse::from));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить вид спорта")
    @ApiResponse(responseCode = "204", description = "Вид спорта удалён")
    @ApiResponse(responseCode = "404", description = "Вид спорта не найден")
    @ApiResponse(responseCode = "409", description = "Вид спорта указан в анкетах")
    ResponseEntity<Void> delete(@PathVariable @Positive long id) {
        sports.delete(id);
        return ResponseEntity.noContent().build();
    }
}
