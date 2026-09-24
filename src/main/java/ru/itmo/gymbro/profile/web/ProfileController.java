package ru.itmo.gymbro.profile.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.gymbro.profile.api.ProfileUseCases;
import ru.itmo.gymbro.profile.api.SavedProfile;
import ru.itmo.gymbro.profile.dto.ProfileGymsWriteRequest;
import ru.itmo.gymbro.profile.dto.ProfileResponse;
import ru.itmo.gymbro.profile.dto.ProfileSportItem;
import ru.itmo.gymbro.profile.dto.ProfileSportsWriteRequest;
import ru.itmo.gymbro.profile.dto.ProfileWriteRequest;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/profiles")
@Tag(name = "Анкеты", description = "Данные анкеты, выбранные виды спорта и залы")
@ApiResponse(responseCode = "400", description = "Некорректные поля запроса")
@ApiResponse(responseCode = "404", description = "Анкета или запись каталога не найдена")
class ProfileController {

    private final ProfileUseCases profiles;

    ProfileController(ProfileUseCases profiles) {
        this.profiles = profiles;
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Получить анкету по идентификатору пользователя")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "Анкета найдена")
    ProfileResponse getByUserId(@PathVariable @Positive long userId) {
        return ProfileResponse.from(profiles.getByUserId(userId));
    }

    @PutMapping("/me")
    @Operation(summary = "Создать или изменить свою анкету",
            description = "Текущий пользователь определяется по X-User-Id. "
                    + "При первом сохранении возвращается 201 и Location, при изменении — 200.")
    @SecurityRequirement(name = "currentUser")
    @ApiResponse(responseCode = "200", description = "Анкета обновлена")
    @ApiResponse(responseCode = "201", description = "Анкета создана; Location содержит её адрес")
    @ApiResponse(responseCode = "401", description = "Текущий пользователь не определён")
    @ApiResponse(responseCode = "403", description = "Пользователь заблокирован")
    @ApiResponse(responseCode = "409", description = "Конфликт параллельного создания анкеты")
    ResponseEntity<ProfileResponse> saveMine(@Valid @RequestBody ProfileWriteRequest request) {
        SavedProfile saved = profiles.saveMine(request.getName(), request.getBirthDate(), request.getAbout());
        ProfileResponse response = ProfileResponse.from(saved.getProfile());
        if (saved.isCreated()) {
            return ResponseEntity.created(URI.create("/api/v1/profiles/" + response.getUserId())).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/sports")
    @Operation(summary = "Заменить виды спорта в своей анкете",
            description = "Передаётся полный список с уровнем подготовки. "
                    + "Пустой список очищает выбор; повторяющиеся виды спорта запрещены.")
    @SecurityRequirement(name = "currentUser")
    @ApiResponse(responseCode = "200", description = "Анкета обновлена")
    @ApiResponse(responseCode = "401", description = "Текущий пользователь не определён")
    @ApiResponse(responseCode = "403", description = "Пользователь заблокирован")
    ProfileResponse replaceMySports(@Valid @RequestBody ProfileSportsWriteRequest request) {
        return ProfileResponse.from(profiles.replaceMySports(
                request.getSports().stream().map(ProfileSportItem::toUserSport).toList()));
    }

    @PutMapping("/me/gyms")
    @Operation(summary = "Заменить залы в своей анкете",
            description = "Передаётся полный список gymIds из каталога. Пустой список очищает выбор. "
                    + "Одобрение заявки само по себе этот список не меняет.")
    @SecurityRequirement(name = "currentUser")
    @ApiResponse(responseCode = "200", description = "Анкета обновлена")
    @ApiResponse(responseCode = "401", description = "Текущий пользователь не определён")
    @ApiResponse(responseCode = "403", description = "Пользователь заблокирован")
    ProfileResponse replaceMyGyms(@Valid @RequestBody ProfileGymsWriteRequest request) {
        return ProfileResponse.from(profiles.replaceMyGyms(request.getGymIds()));
    }
}
