package ru.itmo.gymbro.profile.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.gymbro.profile.api.AddProfilePhotoUseCase;
import ru.itmo.gymbro.profile.api.RemoveProfilePhotoUseCase;
import ru.itmo.gymbro.profile.dto.ProfilePhotoWriteRequest;
import ru.itmo.gymbro.profile.dto.ProfileResponse;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/profiles/me/photos")
@Tag(name = "Анкеты")
@SecurityRequirement(name = "currentUser")
@ApiResponse(responseCode = "400", description = "Некорректный URL, позиция или повтор фотографии")
@ApiResponse(responseCode = "401", description = "Текущий пользователь не определён")
@ApiResponse(responseCode = "403", description = "Пользователь заблокирован")
@ApiResponse(responseCode = "404", description = "Анкета или фотография не найдена")
class ProfilePhotoController {

    private final AddProfilePhotoUseCase addPhoto;
    private final RemoveProfilePhotoUseCase removePhoto;

    ProfilePhotoController(AddProfilePhotoUseCase addPhoto, RemoveProfilePhotoUseCase removePhoto) {
        this.addPhoto = addPhoto;
        this.removePhoto = removePhoto;
    }

    @PostMapping
    @Operation(summary = "Добавить фотографию в свою анкету", description = "Принимает URL, а не файл. Максимум 10 фотографий. Возвращает обновлённую анкету; Location указывает на анкету.")
    @ApiResponse(responseCode = "201", description = "Фотография добавлена")
    @ApiResponse(responseCode = "409", description = "Достигнут лимит в 10 фотографий")
    ResponseEntity<ProfileResponse> add(@Valid @RequestBody ProfilePhotoWriteRequest request) {
        ProfileResponse response = ProfileResponse.from(addPhoto.addMyPhoto(request.getUrl()));
        return ResponseEntity.created(URI.create("/api/v1/profiles/" + response.getUserId())).body(response);
    }

    @DeleteMapping("/{position}")
    @Operation(summary = "Удалить фотографию по позиции", description = "Позиции начинаются с 0. После удаления позиции следующих фотографий сдвигаются.")
    @ApiResponse(responseCode = "204", description = "Фотография удалена")
    ResponseEntity<Void> remove(@PathVariable @PositiveOrZero int position) {
        removePhoto.removeMyPhoto(position);
        return ResponseEntity.noContent().build();
    }
}
