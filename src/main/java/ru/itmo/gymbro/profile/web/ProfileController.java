package ru.itmo.gymbro.profile.web;

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
class ProfileController {

    private final ProfileUseCases profiles;

    ProfileController(ProfileUseCases profiles) {
        this.profiles = profiles;
    }

    @GetMapping("/{userId}")
    ProfileResponse getByUserId(@PathVariable @Positive long userId) {
        return ProfileResponse.from(profiles.getByUserId(userId));
    }

    @PutMapping("/me")
    ResponseEntity<ProfileResponse> saveMine(@Valid @RequestBody ProfileWriteRequest request) {
        SavedProfile saved = profiles.saveMine(request.getName(), request.getBirthDate(), request.getAbout());
        ProfileResponse response = ProfileResponse.from(saved.getProfile());
        if (saved.isCreated()) {
            return ResponseEntity.created(URI.create("/api/v1/profiles/" + response.getUserId())).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/sports")
    ProfileResponse replaceMySports(@Valid @RequestBody ProfileSportsWriteRequest request) {
        return ProfileResponse.from(profiles.replaceMySports(
                request.getSports().stream().map(ProfileSportItem::toUserSport).toList()));
    }

    @PutMapping("/me/gyms")
    ProfileResponse replaceMyGyms(@Valid @RequestBody ProfileGymsWriteRequest request) {
        return ProfileResponse.from(profiles.replaceMyGyms(request.getGymIds()));
    }
}
