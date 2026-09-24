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
import ru.itmo.gymbro.profile.api.ChangeProfileGymsUseCase;
import ru.itmo.gymbro.profile.api.ChangeProfileSportsUseCase;
import ru.itmo.gymbro.profile.api.EditProfileUseCase;
import ru.itmo.gymbro.profile.api.GetProfileUseCase;
import ru.itmo.gymbro.profile.api.SavedProfile;
import ru.itmo.gymbro.profile.dto.ProfileGymsWriteRequest;
import ru.itmo.gymbro.profile.dto.ProfileResponse;
import ru.itmo.gymbro.profile.dto.ProfileSportRequest;
import ru.itmo.gymbro.profile.dto.ProfileSportsWriteRequest;
import ru.itmo.gymbro.profile.dto.ProfileWriteRequest;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/profiles")
class ProfileController {

    private final GetProfileUseCase getProfile;
    private final EditProfileUseCase editProfile;
    private final ChangeProfileSportsUseCase changeSports;
    private final ChangeProfileGymsUseCase changeGyms;

    ProfileController(GetProfileUseCase getProfile, EditProfileUseCase editProfile,
                      ChangeProfileSportsUseCase changeSports, ChangeProfileGymsUseCase changeGyms) {
        this.getProfile = getProfile;
        this.editProfile = editProfile;
        this.changeSports = changeSports;
        this.changeGyms = changeGyms;
    }

    @PutMapping("/me/sports")
    ProfileResponse replaceMySports(@Valid @RequestBody ProfileSportsWriteRequest request) {
        return ProfileResponse.from(changeSports.replaceMySports(
                request.getSports().stream().map(ProfileSportRequest::toUserSport).toList()));
    }

    @PutMapping("/me/gyms")
    ProfileResponse replaceMyGyms(@Valid @RequestBody ProfileGymsWriteRequest request) {
        return ProfileResponse.from(changeGyms.replaceMyGyms(request.getGymIds()));
    }

    @GetMapping("/{userId}")
    ProfileResponse getByUserId(@PathVariable @Positive long userId) {
        return ProfileResponse.from(getProfile.getByUserId(userId));
    }

    @PutMapping("/me")
    ResponseEntity<ProfileResponse> saveMine(@Valid @RequestBody ProfileWriteRequest request) {
        SavedProfile saved = editProfile.saveMine(request.getName(), request.getBirthDate(), request.getAbout());
        ProfileResponse response = ProfileResponse.from(saved.getProfile());
        if (saved.isCreated()) {
            return ResponseEntity.created(URI.create("/api/v1/profiles/" + response.getUserId())).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
