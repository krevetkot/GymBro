package ru.itmo.gymbro.profile.web;

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
class ProfilePhotoController {

    private final AddProfilePhotoUseCase addPhoto;
    private final RemoveProfilePhotoUseCase removePhoto;

    ProfilePhotoController(AddProfilePhotoUseCase addPhoto, RemoveProfilePhotoUseCase removePhoto) {
        this.addPhoto = addPhoto;
        this.removePhoto = removePhoto;
    }

    @PostMapping
    ResponseEntity<ProfileResponse> add(@Valid @RequestBody ProfilePhotoWriteRequest request) {
        ProfileResponse response = ProfileResponse.from(addPhoto.addMyPhoto(request.getUrl()));
        return ResponseEntity.created(URI.create("/api/v1/profiles/" + response.getUserId())).body(response);
    }

    @DeleteMapping("/{position}")
    ResponseEntity<Void> remove(@PathVariable @PositiveOrZero int position) {
        removePhoto.removeMyPhoto(position);
        return ResponseEntity.noContent().build();
    }
}
