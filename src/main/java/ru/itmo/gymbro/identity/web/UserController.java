package ru.itmo.gymbro.identity.web;

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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.gymbro.identity.api.UserUseCases;
import ru.itmo.gymbro.identity.dto.RegisterUserRequest;
import ru.itmo.gymbro.identity.dto.UpdateUserRequest;
import ru.itmo.gymbro.identity.dto.UserResponse;
import ru.itmo.gymbro.shared.web.PageResponses;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Пользователи", description = "Учётные записи. Ответы не содержат пароль.")
@SecurityRequirements
@ApiResponse(responseCode = "400", description = "Некорректные поля или параметры запроса")
class UserController {

    private final UserUseCases users;

    UserController(UserUseCases users) {
        this.users = users;
    }

    @PostMapping
    @Operation(summary = "Зарегистрировать пользователя",
            description = "Создаёт активного пользователя с ролью USER.")
    @ApiResponse(responseCode = "201", description = "Пользователь создан; Location содержит адрес записи")
    @ApiResponse(responseCode = "409", description = "Email уже занят")
    ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        UserResponse response = UserResponse.from(users.register(request.getEmail(), request.getPassword()));
        return ResponseEntity.created(URI.create("/api/v1/users/" + response.getId())).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить пользователя")
    @ApiResponse(responseCode = "200", description = "Пользователь найден")
    @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    UserResponse getById(@PathVariable @Positive long id) {
        return UserResponse.from(users.getById(id));
    }

    @GetMapping
    @Operation(summary = "Получить страницу пользователей",
            description = "page начинается с 0; size по умолчанию 20, максимум 50. Общее количество — в X-Total-Count.")
    @ApiResponse(responseCode = "200", description = "Страница пользователей",
            headers = @Header(name = "X-Total-Count", description = "Общее количество пользователей",
                    schema = @Schema(type = "integer", format = "int64")))
    ResponseEntity<List<UserResponse>> getPage(@ParameterObject Pageable pageable) {
        return PageResponses.withTotalCount(users.getPage(pageable).map(UserResponse::from));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Изменить email или пароль",
            description = "Поля, отсутствующие в запросе или равные null, сохраняют прежние значения. "
                    + "Проверка владельца записи пока не реализована.")
    @ApiResponse(responseCode = "200", description = "Пользователь обновлён")
    @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    @ApiResponse(responseCode = "409", description = "Email уже занят")
    UserResponse update(@PathVariable @Positive long id, @Valid @RequestBody UpdateUserRequest request) {
        return UserResponse.from(users.update(id, request.getEmail(), request.getPassword()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить пользователя",
            description = "Проверка владельца записи пока не реализована. "
                    + "Связанные данные удаляются согласно внешним ключам БД.")
    @ApiResponse(responseCode = "204", description = "Пользователь удалён")
    @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    @ApiResponse(responseCode = "409", description = "Удалению препятствуют связанные данные")
    ResponseEntity<Void> delete(@PathVariable @Positive long id) {
        users.delete(id);
        return ResponseEntity.noContent().build();
    }
}
