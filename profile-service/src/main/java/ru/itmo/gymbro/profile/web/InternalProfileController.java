package ru.itmo.gymbro.profile.web;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.gymbro.profile.api.FeedCandidates;
import ru.itmo.gymbro.profile.api.ProfileDirectory;

import java.util.Set;

@Hidden
@RestController
@RequestMapping("/internal/v1/profiles")
class InternalProfileController {

    private final ProfileDirectory profiles;

    public InternalProfileController(ProfileDirectory profiles) {
        this.profiles = profiles;
    }

    @GetMapping("/feed")
    FeedCandidates findFeedCandidates(@RequestParam @Positive long viewerUserId,
                                                      @RequestParam(defaultValue = "") Set<Long> excludedUserIds,
                                                      Pageable pageable) {
        return profiles.findFeedCandidates(viewerUserId, excludedUserIds,
                pageable.getPageNumber(), pageable.getPageSize());
    }
}
