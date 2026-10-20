package ru.itmo.gymbro.profile.web;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.gymbro.profile.api.ProfileSnapshot;
import ru.itmo.gymbro.profile.api.ProfileUseCases;
import ru.itmo.gymbro.shared.dto.SliceResponse;

import java.util.Set;

@Hidden
@RestController
@RequestMapping("/internal/v1/profiles")
class InternalProfileController {

    private final ProfileUseCases profiles;

    public InternalProfileController(ProfileUseCases profiles) {
        this.profiles = profiles;
    }

    @GetMapping("/feed")
    SliceResponse<ProfileSnapshot> findFeedCandidates(@RequestParam @Positive long viewerUserId,
                                                      @RequestParam(defaultValue = "") Set<Long> excludedUserIds,
                                                      Pageable pageable) {
        return SliceResponse.from(profiles.findFeedCandidates(viewerUserId, excludedUserIds,
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())));
    }
}
